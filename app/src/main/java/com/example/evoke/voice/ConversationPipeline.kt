package com.example.evoke.voice

import android.content.Context
import android.util.Log
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.repository.RecognitionRepository
import com.example.evoke.storage.AudioStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/** Result of filtering one recorded conversation. */
sealed class ConversationResult {
    /**
     * Patient + partner speech kept and saved by [AudioStorage] as
     * [audioPath] (relative, e.g. "audio/<uuid>.wav": 16 kHz mono 16-bit WAV).
     *
     * The caller now owns that file and must hand [audioPath] to
     * RecognitionRepository.createRecognition(...) or
     * PersonRepository.createPerson(...), which queue it in
     * pending_conversations (WAITING_TRANSCRIPTION) and take over deleting it.
     * If it is not handed over, delete it with AudioStorage.deleteAudio(audioPath).
     *
     * [filter] describes who spoke when; filter.filteredFile is a temporary
     * copy that has already been deleted.
     */
    data class Filtered(val audioPath: String, val filter: FilterResult) : ConversationResult()

    /** Nothing matched the patient or the partner; no audio was saved. */
    data class NothingKept(val filter: FilterResult) : ConversationResult()

    object NoPatientVoice : ConversationResult()
    object NoPartnerVoice : ConversationResult()

    data class Failed(val message: String) : ConversationResult()
}

/** Result of [ConversationPipeline.saveConversationForPerson]. */
sealed class ConversationSaveResult {
    /** Recognition saved; the filtered audio is queued for transcription. */
    data class Saved(val recognitionId: Long, val filter: FilterResult) : ConversationSaveResult()

    /** Nothing matched the patient or the partner; nothing was saved. */
    data class NothingKept(val filter: FilterResult) : ConversationSaveResult()

    /** Already MAX_PENDING_CONVERSATIONS (5) waiting; the recording was not filtered or saved. */
    object QueueFull : ConversationSaveResult()

    object NoPatientVoice : ConversationSaveResult()

    /** The person has no voice sample (voiceEnabled is false), so a conversation can't be filtered. */
    object NoPartnerVoice : ConversationSaveResult()

    data class Failed(val message: String) : ConversationSaveResult()
}

/**
 * Recorded Conversation → Silero VAD → segments → CAM++
 * → compare with patient + partner → KEEP / REMOVE
 * → Filtered Conversation (16 kHz WAV) → AudioStorage
 * → pending_conversations (WAITING_TRANSCRIPTION) via the repositories.
 *
 * Transcription and summarising happen later, online, by walking the
 * pending queue (RecognitionRepository.getOldestPending → saveTranscription
 * → completeConversation). They are not part of this class.
 *
 * Typical use for a person who is already saved (Recognised screen):
 *   val live = pipeline.createLiveFilter(personId)          // null = no voiceprints yet
 *   recorder.record(rawWav, VoiceConfig.MAX_CONVERSATION_RECORDING_MS, onAudio = live?.let { it::accept })
 *   pipeline.saveConversationForPerson(personId, System.currentTimeMillis(), lat, lng, rawWav, live)
 *   // if the recording is thrown away instead: live?.cancel()
 *
 * For a person who is NOT saved yet (Not-recognised screen):
 *   val print = enrollment.createPartnerVoiceprint(sampleWav) as VoiceprintResult.Ready
 *   val live  = pipeline.createLiveFilter(listOf(print.embedding))
 *   ...record...
 *   val r = pipeline.filter(rawWav, listOf(print.embedding), live)
 *   if (r is ConversationResult.Filtered) personRepository.createPerson(
 *       ..., audioPath = r.audioPath, voiceEmbedding = print.toVoiceEmbedding())
 */
class ConversationPipeline(
    context: Context,
    private val db: AppDatabase
) {
    private val appContext = context.applicationContext
    private val audioStorage = AudioStorage(appContext)
    private val recognitionRepository = RecognitionRepository(db, audioStorage)

    // ── live filtering (while recording) ─────────────────────────────────────

    /**
     * A started [LiveConversationFilter] for a saved person, or null if the
     * patient or that person has no voiceprint yet (then filtering simply
     * runs on the whole file after Stop). Make a new one for every recording
     * attempt and cancel() it if the attempt is thrown away.
     */
    suspend fun createLiveFilter(personId: Long): LiveConversationFilter? =
        createLiveFilter(VoiceEnrollmentManager.loadPartnerEmbeddings(db, personId))

    /** Same, with the partner's voiceprints given directly (person not saved yet). */
    suspend fun createLiveFilter(partnerEmbeddings: List<FloatArray>): LiveConversationFilter? {
        val patient = VoiceEnrollmentManager.loadPatientEmbeddings(db)
        if (patient.isEmpty() || partnerEmbeddings.isEmpty()) return null
        return LiveConversationFilter(appContext, patient, partnerEmbeddings).also { it.start() }
    }

    // ── filtering ────────────────────────────────────────────────────────────

    /** [filter] for a saved person: loads their voiceprints from voice_embeddings. */
    suspend fun filterForPerson(
        personId: Long,
        rawWav: File,
        liveFilter: LiveConversationFilter? = null,
        deleteRawAfter: Boolean = true
    ): ConversationResult = filter(
        rawWav = rawWav,
        partnerEmbeddings = VoiceEnrollmentManager.loadPartnerEmbeddings(db, personId),
        liveFilter = liveFilter,
        deleteRawAfter = deleteRawAfter
    )

    /**
     * Filters [rawWav] (recorded with [VoiceRecorder]) and saves the kept
     * speech in [AudioStorage]. Nothing is written to the database here.
     *
     * @param partnerEmbeddings the conversation partner's voiceprints
     * @param liveFilter        the [LiveConversationFilter] that was fed while
     *                          [rawWav] was recorded: then only the last speech
     *                          region is left to analyse. Null = filter the whole
     *                          file now. If the live filter failed, the whole
     *                          file is filtered now as a fallback.
     * @param deleteRawAfter    delete the unfiltered recording (recommended: it may
     *                          contain other people's voices)
     */
    suspend fun filter(
        rawWav: File,
        partnerEmbeddings: List<FloatArray>,
        liveFilter: LiveConversationFilter? = null,
        deleteRawAfter: Boolean = true
    ): ConversationResult = withContext(Dispatchers.Default) {

        val filteredFile = File(appContext.cacheDir, "filtered_${System.currentTimeMillis()}.wav")
        try {
            // 1. Voiceprints
            val patient = VoiceEnrollmentManager.loadPatientEmbeddings(db)
            if (patient.isEmpty()) return@withContext ConversationResult.NoPatientVoice
            if (partnerEmbeddings.isEmpty()) return@withContext ConversationResult.NoPartnerVoice

            // 2. VAD → CAM++ → keep patient + partner, remove unknown
            //    (mostly done already during the recording when liveFilter is given)
            val filter = liveFilter
                ?.let { finishLive(it, rawWav, filteredFile) }
                ?: ConversationFilter(appContext).filter(
                    rawWav = rawWav,
                    patientEmbeddings = patient,
                    partnerEmbeddings = partnerEmbeddings,
                    outputWav = filteredFile
                )

            if (!filter.hasKeptSpeech) {
                return@withContext ConversationResult.NothingKept(filter)
            }

            // 3. Filtered conversation → permanent app storage (files/audio/<uuid>.wav)
            val audioPath = withContext(Dispatchers.IO) {
                audioStorage.saveFilteredAudio(filteredFile)
            }
            ConversationResult.Filtered(audioPath, filter)

        } catch (e: CancellationException) {
            throw e                       // screen closed → just stop
        } catch (e: Exception) {
            Log.e(TAG, "Conversation filtering failed", e)
            ConversationResult.Failed(e.message ?: e.javaClass.simpleName)
        } finally {
            liveFilter?.cancel()
            if (deleteRawAfter) rawWav.delete()
            filteredFile.delete()         // the kept copy lives in AudioStorage
        }
    }

    // ── filtering + saving for a saved person ────────────────────────────────

    /**
     * Filters the conversation and saves a recognition for [personId] with
     * the filtered audio queued in pending_conversations
     * (RecognitionRepository.createRecognition). [latitude]/[longitude] are
     * stored in person_history when both are given.
     *
     * Checked BEFORE filtering: the patient's and the person's voiceprints and
     * the pending queue (max 5). In those cases nothing is filtered or saved
     * and [rawWav] is deleted (if [deleteRawAfter]); the caller may still save
     * a location-only recognition with RecognitionRepository.createRecognition.
     */
    suspend fun saveConversationForPerson(
        personId: Long,
        dateTime: Long,
        latitude: Double?,
        longitude: Double?,
        rawWav: File,
        liveFilter: LiveConversationFilter? = null,
        deleteRawAfter: Boolean = true
    ): ConversationSaveResult {
        val early: ConversationSaveResult? = try {
            when {
                VoiceEnrollmentManager.loadPatientEmbeddings(db).isEmpty() ->
                    ConversationSaveResult.NoPatientVoice
                VoiceEnrollmentManager.loadPartnerEmbeddings(db, personId).isEmpty() ->
                    ConversationSaveResult.NoPartnerVoice
                recognitionRepository.isPendingQueueFull() ->
                    ConversationSaveResult.QueueFull
                else -> null
            }
        } catch (e: CancellationException) {
            liveFilter?.cancel()
            if (deleteRawAfter) rawWav.delete()
            throw e
        } catch (e: Exception) {
            ConversationSaveResult.Failed(e.message ?: e.javaClass.simpleName)
        }
        if (early != null) {
            liveFilter?.cancel()
            if (deleteRawAfter) rawWav.delete()
            return early
        }

        return when (val result = filterForPerson(personId, rawWav, liveFilter, deleteRawAfter)) {
            is ConversationResult.Filtered -> try {
                // The repository now owns the audio file: it deletes it if
                // saving fails or the recording can't be queued.
                val recognitionId = recognitionRepository.createRecognition(
                    personId = personId,
                    dateTime = dateTime,
                    latitude = latitude,
                    longitude = longitude,
                    audioPath = result.audioPath
                )
                ConversationSaveResult.Saved(recognitionId, result.filter)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Saving the conversation failed", e)
                ConversationSaveResult.Failed(e.message ?: e.javaClass.simpleName)
            }

            is ConversationResult.NothingKept -> ConversationSaveResult.NothingKept(result.filter)
            ConversationResult.NoPatientVoice -> ConversationSaveResult.NoPatientVoice
            ConversationResult.NoPartnerVoice -> ConversationSaveResult.NoPartnerVoice
            is ConversationResult.Failed -> ConversationSaveResult.Failed(result.message)
        }
    }

    /**
     * Finishes the live filter. Returns null if it failed, so the caller
     * filters the whole recording instead (slower, same result).
     */
    private suspend fun finishLive(
        live: LiveConversationFilter,
        rawWav: File,
        filteredFile: File
    ): FilterResult? = try {
        live.finish(rawWav, filteredFile)
    } catch (e: CancellationException) {
        coroutineContext.ensureActive()   // this call was cancelled → stop
        Log.w(TAG, "Live filter was cancelled; filtering the whole recording instead")
        filteredFile.delete()
        null
    } catch (e: Exception) {
        Log.w(TAG, "Live filter failed; filtering the whole recording instead", e)
        filteredFile.delete()
        null
    }

    private companion object {
        const val TAG = "ConversationPipeline"
    }
}
