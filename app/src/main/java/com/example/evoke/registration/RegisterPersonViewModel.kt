package com.example.evoke.registration

import android.app.Application
import android.graphics.Rect
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.evoke.R
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.FaceEmbedding
import com.example.evoke.database.Person
import com.example.evoke.database.repository.PersonRepository
import com.example.evoke.database.repository.RecognitionRepository
import com.example.evoke.location.LocationHelper
import com.example.evoke.mobilefacenet.FaceConfig
import com.example.evoke.mobilefacenet.FaceRecognitionResult
import com.example.evoke.mobilefacenet.FaceTemplateStore
import com.example.evoke.storage.AudioStorage
import com.example.evoke.storage.PersonImageStorage
import com.example.evoke.voice.EmbeddingMath
import com.example.evoke.voice.VoiceSessionViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RegisterUiState(
    /** Shown in the Location field (place name or coordinates). */
    val locationText: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLocating: Boolean = false,
    val isSaving: Boolean = false,
    /** One-off message for a Toast; cleared with [RegisterPersonViewModel.messageShown]. */
    @StringRes val messageRes: Int? = null,
    val messageArg: String? = null
)

/**
 * Registers the visitor from the Not-recognised screen as a new person.
 *
 * One PersonRepository.createPerson transaction writes:
 * - persons (name, relation, profile image, voiceEnabled)
 * - face_embeddings (the MobileFaceNet embedding of the captured face)
 * - voice_embeddings (the 1–3 voice samples from the recorder)
 * - recognition_events + person_history (when a location was taken)
 * - conversations + pending_conversations (when a conversation was recorded;
 *   it is then transcribed and summarised from the queue)
 *
 * The database requires a location OR a recorded conversation for the first
 * recognition, so Save is refused when neither is there.
 *
 * Afterwards the face is also added to FaceTemplateStore under the new
 * personId, so the next photo of this person is recognised as them.
 */
class RegisterPersonViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext
    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val audioStorage by lazy { AudioStorage(appContext) }
    private val imageStorage by lazy { PersonImageStorage(appContext) }
    private val personRepository by lazy {
        PersonRepository(
            db = db,
            recognitionRepository = RecognitionRepository(db, audioStorage),
            audioStorage = audioStorage,
            imageStorage = imageStorage
        )
    }

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    /** New face check / leaving the screen: start a fresh form. */
    fun reset() {
        if (!_state.value.isSaving) _state.value = RegisterUiState()
    }

    fun messageShown() {
        _state.update { it.copy(messageRes = null, messageArg = null) }
    }

    // ── location ──────────────────────────────────────────────────────────────

    /** Takes the current location. The caller has already been granted location permission. */
    fun fetchCurrentLocation() {
        if (_state.value.isLocating) return
        if (!LocationHelper.isLocationEnabled(appContext)) {
            _state.update { it.copy(messageRes = R.string.addloc_turn_on_location) }
            return
        }
        _state.update { it.copy(isLocating = true, messageRes = R.string.addloc_finding) }
        viewModelScope.launch {
            val location = LocationHelper.getCurrentLocation(appContext)
            if (location == null) {
                _state.update { it.copy(isLocating = false, messageRes = R.string.addloc_location_not_found) }
                return@launch
            }
            val name = LocationHelper.placeName(appContext, location.latitude, location.longitude)
            _state.update {
                it.copy(
                    isLocating = false,
                    locationText = name,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    messageRes = null
                )
            }
        }
    }

    // ── save ──────────────────────────────────────────────────────────────────

    fun register(
        name: String,
        relation: String,
        face: FaceRecognitionResult?,
        faceStore: FaceTemplateStore,
        voiceSession: VoiceSessionViewModel,
        onSuccess: (personId: Long) -> Unit
    ) {
        val current = _state.value
        if (current.isSaving) return

        // ── checks before anything is written ─────────────────────────────────
        if (name.isBlank() || relation.isBlank()) {
            _state.update { it.copy(messageRes = R.string.register_name_relation_required) }
            return
        }
        if (face == null) {
            _state.update { it.copy(messageRes = R.string.register_no_face) }
            return
        }
        val hasLocation = current.latitude != null && current.longitude != null
        val hasConversation = voiceSession.pendingConversationAudioPath != null
        if (!hasLocation && !hasConversation) {
            _state.update { it.copy(messageRes = R.string.register_need_location_or_conversation) }
            return
        }

        _state.update { it.copy(isSaving = true, messageRes = null) }

        viewModelScope.launch {
            // From here createPerson owns the conversation audio (deletes it on failure).
            val (voiceprints, audioPath) = voiceSession.consumePendingPerson()
            val voiceEmbeddings = voiceprints
                .sortedByDescending { it.quality }
                .take(VoiceSessionViewModel.SAMPLES_PER_SESSION)
                .map { it.toVoiceEmbedding() }

            try {
                val personId = personRepository.createPerson(
                    person = Person(
                        name = name.trim(),
                        relation = relation.trim(),
                        profileImagePath = "",
                        voiceEnabled = voiceEmbeddings.isNotEmpty()   // repository sets it too
                    ),
                    firstFaceEmbedding = FaceEmbedding(
                        personId = 0,                                 // repository sets the real id
                        embedding = EmbeddingMath.toBytes(face.embedding),
                        type = "${FaceConfig.MODEL_ID}/${FaceConfig.PIPELINE_VERSION}",
                        quality = 1f                                  // passed the RECOGNIZE quality check
                    ),
                    dateTime = System.currentTimeMillis(),
                    latitude = current.latitude,
                    longitude = current.longitude,
                    audioPath = audioPath,
                    voiceEmbedding = voiceEmbeddings.firstOrNull(),
                    additionalVoiceEmbeddings = voiceEmbeddings.drop(1),
                    profileImageProvider = face.profilePhoto?.let { photo ->
                        { id: Long ->
                            // The profile photo is already framed around the face: keep it whole.
                            imageStorage.saveProfileImage(
                                personId = id,
                                bitmap = photo,
                                faceBox = Rect(0, 0, photo.width, photo.height),
                                marginFraction = 0f
                            )
                        }
                    }
                )

                // Face gallery used for recognition, keyed by the Room personId.
                face.alignedFace?.let { aligned ->
                    withContext(Dispatchers.IO) {
                        faceStore.addFace(personId.toString(), face.embedding, aligned)
                    }
                }

                _state.value = RegisterUiState()
                onSuccess(personId)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Registering the person failed", e)
                // The voice samples survive; the conversation audio was deleted by
                // createPerson (it owns the file), so it has to be recorded again.
                voiceSession.restorePendingVoiceprints(voiceprints)
                _state.update {
                    it.copy(
                        isSaving = false,
                        messageRes = R.string.register_failed,
                        messageArg = e.message ?: e.javaClass.simpleName
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "RegisterPerson"
    }
}
