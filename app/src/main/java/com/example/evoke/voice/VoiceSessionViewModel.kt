package com.example.evoke.voice

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.evoke.R
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.repository.RecognitionRepository
import com.example.evoke.storage.AudioStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** What the recorder screen is recording. */
enum class RecorderMode {
    /** Profile / "Let us know you": the patient's voice samples (script shown, 3 samples). */
    PATIENT_SAMPLE,

    /** Not-recognised screen: the new person's voice samples (script shown, 3 samples). */
    PARTNER_SAMPLE,

    /** Recognised / Not-recognised screen: patient + visitor conversation (no script, max 30 min). */
    CONVERSATION
}

enum class RecorderPhase { READY, RECORDING, STOPPED, PROCESSING, DONE }

/** A message for the recorder screen, resolved with stringResource in the UI. */
data class RecorderMessage(
    @StringRes val res: Int,
    val args: List<Any> = emptyList(),
    val isError: Boolean = false
)

data class RecorderUiState(
    val mode: RecorderMode = RecorderMode.PATIENT_SAMPLE,
    val phase: RecorderPhase = RecorderPhase.READY,
    val message: RecorderMessage? = null,
    /** DONE without success, but recording again can fix it (e.g. too little speech). */
    val canRetry: Boolean = false,
    /** Voice samples recorded in this session (sample modes). */
    val samplesRecorded: Int = 0,
    /** DONE after a good sample and fewer than [VoiceSessionViewModel.SAMPLES_PER_SESSION] recorded. */
    val canRecordAnother: Boolean = false,
    /** A visitor sample was refused as "sounds like the patient"; it can still be kept. */
    val canKeepAnyway: Boolean = false
) {
    /** Number of the sample being recorded now (1..3). */
    val currentSample: Int
        get() = (samplesRecorded + 1).coerceAtMost(VoiceSessionViewModel.SAMPLES_PER_SESSION)
}

/**
 * Drives the recorder screen for all three uses and runs the voice pipeline.
 *
 * Scoped to the Activity (created in AppNavigation, like FaceRecognitionViewModel),
 * so the "Save recording?" / "Don't save recording?" popups, the recorder screen
 * and the Not-recognised screen all share the same data.
 *
 * Where results go:
 * - PATIENT_SAMPLE → user_voice_embeddings (VoiceEnrollmentManager.enrollPatient;
 *   the repository keeps the best 3)
 * - PARTNER_SAMPLE → for a saved person ([personId] set): voice_embeddings.
 *   For the new person on the Not-recognised screen: up to 3 voiceprints are kept
 *   in [pendingPartnerVoiceprints] and saved by RegisterPersonViewModel
 *   (PersonRepository.createPerson) when the person is registered.
 * - CONVERSATION   → saved person: recognition + pending_conversations
 *   (ConversationPipeline.saveConversationForPerson). New person: filtered with
 *   [pendingPartnerVoiceprints]; the audio waits in [pendingConversationAudioPath]
 *   and is queued by createPerson(audioPath = ...) at registration.
 */
class VoiceSessionViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        /** Voice samples asked for per speaker (the database keeps the best 3). */
        const val SAMPLES_PER_SESSION = 3
        private const val TAG = "VoiceSession"
    }

    private val appContext = app.applicationContext
    private val db by lazy { AppDatabase.getDatabase(appContext) }
    private val enrollment by lazy { VoiceEnrollmentManager(appContext, db) }
    private val pipeline by lazy { ConversationPipeline(appContext, db) }
    private val audioStorage by lazy { AudioStorage(appContext) }

    val recorder = VoiceRecorder(appContext)

    private val _state = MutableStateFlow(RecorderUiState())
    val state: StateFlow<RecorderUiState> = _state.asStateFlow()

    /** Saved person (Room personId) the sample / conversation belongs to; null = new person. */
    private var personId: Long? = null

    private var recordJob: Job? = null
    private var processJob: Job? = null
    private var rawFile: File? = null
    private var liveFilter: LiveConversationFilter? = null

    /** Visitor voiceprint refused as "sounds like the patient", kept for "Keep this sample". */
    private var conflictPrint: VoiceprintResult.Ready? = null

    // ── new person, not saved yet (Not-recognised screen) ─────────────────────

    private val _pendingPartnerVoiceprints = mutableListOf<VoiceprintResult.Ready>()

    /** The new person's voiceprints (max 3), waiting for registration. */
    val pendingPartnerVoiceprints: List<VoiceprintResult.Ready>
        get() = _pendingPartnerVoiceprints.toList()

    /** Filtered conversation (AudioStorage path) waiting for registration. */
    var pendingConversationAudioPath: String? = null
        private set

    /** For the Not-recognised screen: voice samples recorded / conversation recorded. */
    data class PendingPersonInfo(val voiceSamples: Int = 0, val hasConversation: Boolean = false)

    private val _pendingInfo = MutableStateFlow(PendingPersonInfo())
    val pendingInfo: StateFlow<PendingPersonInfo> = _pendingInfo.asStateFlow()

    private fun publishPending() {
        _pendingInfo.value = PendingPersonInfo(
            voiceSamples = _pendingPartnerVoiceprints.size,
            hasConversation = pendingConversationAudioPath != null
        )
    }

    // ── screen actions ────────────────────────────────────────────────────────

    /** Call before navigating to the recorder screen. */
    fun prepare(mode: RecorderMode, personId: Long? = null) {
        // A previous recording still being processed must not overwrite this new session.
        processJob?.cancel()
        processJob = null
        discardRecording()
        this.personId = personId

        var alreadyRecorded = 0
        if (mode == RecorderMode.PARTNER_SAMPLE && personId == null) {
            if (_pendingPartnerVoiceprints.size >= SAMPLES_PER_SESSION) {
                // A full set exists: this is a new set for the person (re-record).
                // The old conversation was filtered with the old voice, so drop it too.
                clearPendingPerson()
            }
            alreadyRecorded = _pendingPartnerVoiceprints.size
        }
        _state.value = RecorderUiState(mode = mode, samplesRecorded = alreadyRecorded)
    }

    /** Starts recording. The caller has already been granted RECORD_AUDIO. */
    fun startRecording() {
        val current = _state.value
        if (current.phase != RecorderPhase.READY) return

        conflictPrint = null
        _state.update {
            it.copy(phase = RecorderPhase.RECORDING, message = null, canRetry = false,
                canRecordAnother = false, canKeepAnyway = false)
        }

        recordJob = viewModelScope.launch {
            try {
                // A conversation can only be filtered when both voices are known:
                // check that before recording, not after 30 minutes.
                if (current.mode == RecorderMode.CONVERSATION) {
                    val problem = conversationPrecheck()
                    if (problem != null) {
                        finish(problem)
                        return@launch
                    }
                    liveFilter = createLiveFilter()
                }

                val prefix = if (current.mode == RecorderMode.CONVERSATION) "conversation" else "voice_sample"
                val file = VoiceRecorder.newTempFile(appContext, prefix)
                val maxMs = if (current.mode == RecorderMode.CONVERSATION)
                    VoiceConfig.MAX_CONVERSATION_RECORDING_MS   // 30 minutes
                else
                    VoiceConfig.MAX_ENROLL_RECORDING_MS         // 1 minute

                val live = liveFilter
                recorder.record(file, maxMs, onAudio = live?.let { it::accept })
                rawFile = file
                _state.update { it.copy(phase = RecorderPhase.STOPPED) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Recording failed", e)
                liveFilter?.cancel()
                liveFilter = null
                finish(RecorderMessage(R.string.voice_recording_failed, listOf(e.message ?: ""), isError = true), canRetry = true)
            }
        }
    }

    /** "Done" while recording: finishes the recording (then STOPPED). */
    fun stopRecording() {
        if (_state.value.phase == RecorderPhase.RECORDING) recorder.stop()
    }

    /** "Save": runs enrollment / the conversation pipeline on the recording. */
    fun saveRecording() {
        val phase = _state.value.phase
        if (phase != RecorderPhase.RECORDING && phase != RecorderPhase.STOPPED) return
        recorder.stop()

        processJob = viewModelScope.launch {
            recordJob?.join()                       // wait until the WAV file is closed
            if (_state.value.phase != RecorderPhase.STOPPED) return@launch
            val file = rawFile ?: return@launch
            rawFile = null

            _state.update { it.copy(phase = RecorderPhase.PROCESSING) }
            try {
                when (_state.value.mode) {
                    RecorderMode.PATIENT_SAMPLE -> processPatientSample(file)
                    RecorderMode.PARTNER_SAMPLE -> processPartnerSample(file)
                    RecorderMode.CONVERSATION -> processConversation(file)
                }
            } catch (e: CancellationException) {
                file.delete()
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Processing failed", e)
                file.delete()
                finish(RecorderMessage(R.string.voice_failed, listOf(e.message ?: ""), isError = true), canRetry = true)
            }
        }
    }

    /** "Try again" after a fixable problem: same sample number, back to READY. */
    fun retry() {
        conflictPrint = null
        discardRecording()
        _state.update { it.copy(phase = RecorderPhase.READY, message = null, canRetry = false, canRecordAnother = false) }
    }

    /** "Record sample n" after a good sample: next sample, back to READY. */
    fun nextSample() = retry()

    /**
     * "Keep this sample": the caregiver confirms the refused visitor sample is a
     * different person from the patient, so its voiceprint is kept after all.
     */
    fun keepSampleAnyway() {
        val print = conflictPrint ?: return
        if (_state.value.phase != RecorderPhase.DONE) return
        conflictPrint = null

        processJob = viewModelScope.launch {
            _state.update {
                it.copy(phase = RecorderPhase.PROCESSING, message = null, canRetry = false, canKeepAnyway = false)
            }
            val savedPersonId = personId
            if (savedPersonId != null) {
                finishEnrollment(enrollment.savePartnerVoiceprint(savedPersonId, print), isPatient = false)
            } else if (_pendingPartnerVoiceprints.size < SAMPLES_PER_SESSION) {
                _pendingPartnerVoiceprints += print
                publishPending()
                sampleAccepted(
                    RecorderMessage(R.string.voice_partner_sample_collected, listOf(_pendingPartnerVoiceprints.size))
                )
            } else {
                sampleAccepted(RecorderMessage(R.string.voice_all_samples_done))
            }
        }
    }

    /** "Don't save" / leaving the screen: drops the current recording. */
    fun discardRecording() {
        recorder.stop()
        recordJob?.cancel()
        recordJob = null
        conflictPrint = null
        liveFilter?.cancel()
        liveFilter = null
        rawFile?.delete()
        rawFile = null
        if (_state.value.phase != RecorderPhase.PROCESSING) {
            _state.update {
                it.copy(phase = RecorderPhase.READY, message = null, canRetry = false,
                    canRecordAnother = false, canKeepAnyway = false)
            }
        }
    }

    /** Forget the unsaved person (new face check, or leaving the Not-recognised screen). */
    fun clearPendingPerson() {
        _pendingPartnerVoiceprints.clear()
        pendingConversationAudioPath?.let { audioStorage.deleteAudio(it) }
        pendingConversationAudioPath = null
        publishPending()
    }

    /**
     * Hands the new person's voiceprints and conversation to the registration
     * and forgets them WITHOUT deleting the audio: PersonRepository.createPerson
     * owns the audio file from now on (it deletes it if registration fails).
     */
    fun consumePendingPerson(): Pair<List<VoiceprintResult.Ready>, String?> {
        val prints = _pendingPartnerVoiceprints.toList()
        val audioPath = pendingConversationAudioPath
        _pendingPartnerVoiceprints.clear()
        pendingConversationAudioPath = null
        publishPending()
        return prints to audioPath
    }

    /** Registration failed: give the voiceprints back so they don't need recording again. */
    fun restorePendingVoiceprints(prints: List<VoiceprintResult.Ready>) {
        if (_pendingPartnerVoiceprints.isEmpty()) {
            _pendingPartnerVoiceprints += prints.take(SAMPLES_PER_SESSION)
        }
        publishPending()
    }

    override fun onCleared() {
        discardRecording()
        clearPendingPerson()
        super.onCleared()
    }

    // ── processing ────────────────────────────────────────────────────────────

    private suspend fun processPatientSample(file: File) {
        val result = try {
            enrollment.enrollPatient(file)
        } finally {
            file.delete()
        }
        finishEnrollment(result, isPatient = true)
    }

    private suspend fun processPartnerSample(file: File) {
        val savedPersonId = personId
        try {
            if (savedPersonId != null) {
                finishEnrollment(enrollment.enrollPartner(savedPersonId, file), isPatient = false)
                return
            }

            when (val print = enrollment.createPartnerVoiceprint(file)) {
                is VoiceprintResult.Rejected -> finishEnrollment(print.reason, isPatient = false)
                is VoiceprintResult.Ready -> {
                    // The new sample must sound like this person's earlier samples.
                    val earlier = _pendingPartnerVoiceprints.map { it.embedding }
                    val similarity = earlier.takeIf { it.isNotEmpty() }
                        ?.let { EmbeddingMath.bestScore(print.embedding, it) }
                    if (similarity != null && similarity < VoiceConfig.ENROLL_CONSISTENCY_THRESHOLD) {
                        finishEnrollment(EnrollmentResult.DoesNotMatchExisting(similarity), isPatient = false)
                        return
                    }
                    _pendingPartnerVoiceprints += print
                    publishPending()
                    sampleAccepted(
                        RecorderMessage(R.string.voice_partner_sample_collected, listOf(_pendingPartnerVoiceprints.size))
                    )
                }
            }
        } finally {
            file.delete()
        }
    }

    private suspend fun processConversation(file: File) {
        val live = liveFilter
        liveFilter = null
        val savedPersonId = personId

        if (savedPersonId != null) {
            val message = when (val r = pipeline.saveConversationForPerson(
                personId = savedPersonId,
                dateTime = System.currentTimeMillis(),
                latitude = null,
                longitude = null,
                rawWav = file,
                liveFilter = live
            )) {
                is ConversationSaveResult.Saved -> RecorderMessage(R.string.conversation_saved)
                is ConversationSaveResult.NothingKept ->
                    RecorderMessage(R.string.conversation_nothing_kept, isError = true)
                ConversationSaveResult.QueueFull ->
                    RecorderMessage(R.string.conversation_queue_full, isError = true)
                ConversationSaveResult.NoPatientVoice ->
                    RecorderMessage(R.string.conversation_no_patient_voice, isError = true)
                ConversationSaveResult.NoPartnerVoice ->
                    RecorderMessage(R.string.conversation_no_partner_voice, isError = true)
                is ConversationSaveResult.Failed ->
                    RecorderMessage(R.string.voice_failed, listOf(r.message), isError = true)
            }
            finish(message, canRetry = message.res == R.string.conversation_nothing_kept)
            return
        }

        val partner = _pendingPartnerVoiceprints.map { it.embedding }
        if (partner.isEmpty()) {
            live?.cancel()
            file.delete()
            finish(RecorderMessage(R.string.conversation_no_partner_voice, isError = true))
            return
        }

        when (val r = pipeline.filter(file, partner, live)) {
            is ConversationResult.Filtered -> {
                pendingConversationAudioPath?.let { audioStorage.deleteAudio(it) }
                pendingConversationAudioPath = r.audioPath
                publishPending()
                finish(RecorderMessage(R.string.conversation_filtered_pending))
            }
            is ConversationResult.NothingKept ->
                finish(RecorderMessage(R.string.conversation_nothing_kept, isError = true), canRetry = true)
            ConversationResult.NoPatientVoice ->
                finish(RecorderMessage(R.string.conversation_no_patient_voice, isError = true))
            ConversationResult.NoPartnerVoice ->
                finish(RecorderMessage(R.string.conversation_no_partner_voice, isError = true))
            is ConversationResult.Failed ->
                finish(RecorderMessage(R.string.voice_failed, listOf(r.message), isError = true), canRetry = true)
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Null when a conversation can be recorded, otherwise the reason it can't. */
    private suspend fun conversationPrecheck(): RecorderMessage? {
        if (VoiceEnrollmentManager.loadPatientEmbeddings(db).isEmpty()) {
            return RecorderMessage(R.string.conversation_no_patient_voice, isError = true)
        }
        val savedPersonId = personId
        val partnerKnown = if (savedPersonId != null) {
            VoiceEnrollmentManager.loadPartnerEmbeddings(db, savedPersonId).isNotEmpty()
        } else {
            _pendingPartnerVoiceprints.isNotEmpty()
        }
        if (!partnerKnown) {
            return RecorderMessage(R.string.conversation_no_partner_voice, isError = true)
        }
        // The queue holds at most 5 conversations; a new person's conversation
        // is queued at registration, so it must fit too.
        if (db.pendingConversationDao().getPendingCount() >=
            RecognitionRepository.MAX_PENDING_CONVERSATIONS
        ) {
            return RecorderMessage(R.string.conversation_queue_full, isError = true)
        }
        return null
    }

    private suspend fun createLiveFilter(): LiveConversationFilter? {
        val savedPersonId = personId
        return if (savedPersonId != null) {
            pipeline.createLiveFilter(savedPersonId)
        } else {
            _pendingPartnerVoiceprints.takeIf { it.isNotEmpty() }
                ?.let { prints -> pipeline.createLiveFilter(prints.map { it.embedding }) }
        }
    }

    private fun finishEnrollment(result: EnrollmentResult, isPatient: Boolean) {
        when (result) {
            is EnrollmentResult.Saved -> sampleAccepted(
                RecorderMessage(
                    if (isPatient) R.string.voice_patient_sample_saved else R.string.voice_partner_sample_saved,
                    listOf(_state.value.samplesRecorded + 1)
                )
            )
            is EnrollmentResult.TooLittleSpeech -> finish(
                RecorderMessage(R.string.voice_too_little_speech, listOf(result.requiredSeconds.toInt()), isError = true),
                canRetry = true
            )
            is EnrollmentResult.DoesNotMatchExisting -> finish(
                RecorderMessage(R.string.voice_does_not_match, isError = true),
                canRetry = true
            )
            is EnrollmentResult.SoundsLikePatient -> {
                conflictPrint = result.voiceprint
                finish(
                    RecorderMessage(R.string.voice_sounds_like_patient, isError = true),
                    canRetry = true,
                    canKeepAnyway = result.voiceprint != null
                )
            }
            is EnrollmentResult.Failed -> finish(
                RecorderMessage(R.string.voice_failed, listOf(result.message), isError = true),
                canRetry = true
            )
        }
    }

    /** A good sample: count it; offer the next one until 3 are recorded. */
    private fun sampleAccepted(message: RecorderMessage) {
        _state.update {
            val recorded = (it.samplesRecorded + 1).coerceAtMost(SAMPLES_PER_SESSION)
            it.copy(
                phase = RecorderPhase.DONE,
                message = if (recorded >= SAMPLES_PER_SESSION)
                    RecorderMessage(R.string.voice_all_samples_done)
                else
                    message,
                canRetry = false,
                samplesRecorded = recorded,
                canRecordAnother = recorded < SAMPLES_PER_SESSION,
                canKeepAnyway = false
            )
        }
    }

    private fun finish(message: RecorderMessage, canRetry: Boolean = false, canKeepAnyway: Boolean = false) {
        _state.update {
            it.copy(
                phase = RecorderPhase.DONE,
                message = message,
                canRetry = canRetry && message.isError,
                canRecordAnother = false,
                canKeepAnyway = canKeepAnyway
            )
        }
    }
}
