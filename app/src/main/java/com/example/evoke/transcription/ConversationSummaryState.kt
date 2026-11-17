package com.example.evoke.transcription

import android.content.Context
import android.util.Log
import com.example.evoke.storage.AudioStorage
import com.example.evoke.voice.FilteredSegment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [CONVERSATION SUMMARY STATE]
 * Filtered conversation audio -> Groq transcription (English) -> Groq summary,
 * kept ONLY in variables for now (no database yet).
 *
 *   ConversationPipeline.filter()  saves the filtered audio (AudioStorage, "audio/<uuid>.wav")
 *     -> [startFromFilteredAudio]  reads that file into the variable filteredAudio
 *     -> GroqClient.transcribeToEnglish  -> [englishTranscript]
 *     -> GroqClient.summarize            -> [englishSummary]
 *
 * AppNavigation.kt reads [englishSummary] into its variable `englishSummary`.
 * TODO (database step): save the transcript/summary with
 *      RecognitionRepository.saveTranscription / completeConversation instead.
 */
object ConversationSummaryState {

    private const val TAG = "ConversationSummary"

    enum class Status { IDLE, TRANSCRIBING, SUMMARISING, DONE, FAILED }

    private val _status = MutableStateFlow(Status.IDLE)
    val status: StateFlow<Status> = _status.asStateFlow()

    /** English transcript of the last filtered conversation. */
    private val _englishTranscript = MutableStateFlow<String?>(null)
    val englishTranscript: StateFlow<String?> = _englishTranscript.asStateFlow()

    /** English summary of the last filtered conversation (the final result). */
    private val _englishSummary = MutableStateFlow<String?>(null)
    val englishSummary: StateFlow<String?> = _englishSummary.asStateFlow()

    /** Error message when the last run failed (e.g. no internet, wrong API key). */
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Runs independently of any screen, so leaving a screen does not stop it.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    /**
     * [START TRANSCRIPTION + SUMMARY]
     * Called by ConversationPipeline right after the filtered audio is saved.
     * The file is read into memory at once (the database queue may delete it
     * later), then Groq runs in the background.
     */
    fun startFromFilteredAudio(context: Context, audioPath: String, segments: List<FilteredSegment>) {
        // [FILTERED AUDIO VARIABLE] the filtered conversation (patient + partner only)
        val filteredAudio: ByteArray = try {
            AudioStorage(context.applicationContext).loadAudio(audioPath)?.readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "Could not read filtered audio $audioPath", e)
            null
        } ?: run {
            _status.value = Status.FAILED
            _error.value = "Filtered audio not found"
            return
        }
        startFromAudioBytes(filteredAudio, segments)
    }

    /**
     * Same as [startFromFilteredAudio], with the WAV (16 kHz, mono, 16-bit) already in memory.
     * Also used by the temporary debug test (debug/GroqDebugTest.kt).
     */
    fun startFromAudioBytes(filteredAudio: ByteArray, segments: List<FilteredSegment> = emptyList()) {
        job?.cancel()
        _englishTranscript.value = null
        _englishSummary.value = null
        _error.value = null
        _status.value = Status.TRANSCRIBING

        job = scope.launch {
            try {
                // [TRANSCRIPTION] filtered audio -> English transcript
                val start = System.currentTimeMillis()
                val transcript = GroqClient.transcribeToEnglish(
                    filteredAudio = filteredAudio,
                    segments = segments,
                    onProgress = { Log.i(TAG, it) }
                )
                Log.i(TAG, "Transcription completed in ${(System.currentTimeMillis() - start) / 1000.0} s")
                if (transcript.english.isBlank()) {
                    _status.value = Status.FAILED
                    _error.value = "No words were recognised"
                    return@launch
                }
                _englishTranscript.value = transcript.english
                Log.i(TAG, "English transcript: ${transcript.english}")

                // [SUMMARY] English transcript -> English summary
                _status.value = Status.SUMMARISING
                val summaryStart = System.currentTimeMillis()
                val summary = GroqClient.summarize(transcript.english)
                Log.i(TAG, "Summary completed in ${(System.currentTimeMillis() - summaryStart) / 1000.0} s")
                Log.i(TAG, "English summary: $summary")
                Log.i(
                    TAG, "Summary length: ${GroqConfig.wordCount(summary)} words " +
                        "(limit ${GroqConfig.summaryWordLimit(transcript.english)}, transcript ${GroqConfig.wordCount(transcript.english)} words)"
                )

                _englishSummary.value = summary
                _status.value = Status.DONE
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Transcription/summary failed", e)
                _error.value = when (e) {
                    is GroqException -> "Groq error: ${e.message}"
                    else -> "Groq not reachable: ${e.message}"
                }
                _status.value = Status.FAILED
            }
        }
    }

    /** Clears the last result (e.g. after it has been saved or shown). */
    fun clear() {
        job?.cancel()
        _englishTranscript.value = null
        _englishSummary.value = null
        _error.value = null
        _status.value = Status.IDLE
    }
}
