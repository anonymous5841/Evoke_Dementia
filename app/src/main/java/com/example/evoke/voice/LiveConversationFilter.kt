package com.example.evoke.voice

import android.content.Context
import com.k2fsa.sherpa.onnx.Vad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Filters a conversation WHILE it is being recorded.
 *
 * Batch filtering ([ConversationFilter]) only starts after Stop, so a
 * 30-minute conversation is analysed from scratch at the end. Here the
 * recorder hands every audio block to [accept] as it is captured; a
 * background worker runs Silero VAD on it and, as soon as a speech region
 * ends, cuts it into windows, embeds them and labels them — the same
 * [RegionClassifier] the batch filter uses, so the result is identical.
 *
 * After Stop, [finish] only has to flush the last region and write the
 * filtered WAV, which takes seconds instead of minutes.
 *
 * Usage (see ConversationPipeline):
 *   val live = LiveConversationFilter(context, patientPrints, partnerPrints).also { it.start() }
 *   recorder.record(file, maxMs, onAudio = live::accept)
 *   val result = live.finish(file, filteredFile)   // or live.cancel() if the recording is discarded
 *
 * Note: if the phone is slower than real time the queued audio grows in
 * memory (about 3.8 MB per minute of backlog); it is still processed, just
 * later. With CAM++ this is far faster than real time on current phones.
 */
class LiveConversationFilter(
    context: Context,
    private val patientEmbeddings: List<FloatArray>,
    private val partnerEmbeddings: List<FloatArray>,
    private val threshold: Float = VoiceConfig.SPEAKER_MATCH_THRESHOLD
) {
    private val appContext = context.applicationContext

    /** Audio blocks from the recorder, in order (floats in -1..1). */
    private val audio = Channel<FloatArray>(Channel.UNLIMITED)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var worker: Deferred<List<ClassifiedPiece>>? = null

    /** How much of the recording has been through VAD so far (for progress UI). */
    @Volatile
    var analysedMs: Long = 0L
        private set

    init {
        require(patientEmbeddings.isNotEmpty()) { "No patient voice sample enrolled" }
        require(partnerEmbeddings.isNotEmpty()) { "No partner voice sample enrolled" }
    }

    /** Starts the background worker. Call once, before the recording starts. */
    fun start() {
        check(worker == null) { "Already started" }
        worker = scope.async {
            try {
                analyse()
            } catch (e: Throwable) {
                // Stop queuing audio nobody will read; finish() then rethrows
                // and ConversationPipeline falls back to batch filtering.
                audio.cancel()
                throw e
            }
        }
    }

    /**
     * Called by [VoiceRecorder] for every block it writes to the WAV file.
     * Copies the samples, so the recorder may reuse [pcm]. Never blocks.
     */
    fun accept(pcm: ShortArray, count: Int) {
        audio.trySend(FloatArray(count) { pcm[it] / 32768f })
    }

    /**
     * Call after the recording has finished and [rawWav] is closed.
     * Waits for the remaining audio to be analysed, then writes [outputWav].
     */
    suspend fun finish(rawWav: File, outputWav: File): FilterResult {
        val running = checkNotNull(worker) { "start() was not called" }
        audio.close()                                   // no more audio: the worker drains and stops
        return try {
            val pieces = running.await()
            withContext(Dispatchers.IO) { FilterAssembler.assemble(rawWav, pieces, outputWav) }
        } finally {
            scope.cancel()
        }
    }

    /** Stops the worker and drops everything (recording cancelled or discarded). */
    fun cancel() {
        audio.close()
        scope.cancel()
    }

    // ── worker ─────────────────────────────────────────────────────────────────

    private suspend fun analyse(): List<ClassifiedPiece> {
        val classifier = RegionClassifier(
            SpeakerEmbedder(appContext), patientEmbeddings, partnerEmbeddings, threshold
        )
        val pieces = mutableListOf<ClassifiedPiece>()
        val window = VoiceConfig.VAD_WINDOW_SIZE
        val pending = FloatArray(window)
        var filled = 0
        var fedSamples = 0L

        val vad = VoiceModels.createVad(appContext)
        try {
            // Feed the VAD in exact 512-sample steps, like SpeechSegmenter does for a file.
            for (block in audio) {
                var i = 0
                while (i < block.size) {
                    val n = minOf(window - filled, block.size - i)
                    System.arraycopy(block, i, pending, filled, n)
                    filled += n
                    i += n
                    if (filled == window) {
                        vad.acceptWaveform(pending.copyOf())
                        filled = 0
                        fedSamples += window
                        analysedMs = fedSamples * 1000 / VoiceConfig.SAMPLE_RATE
                        drain(vad, classifier, pieces)
                    }
                }
            }

            // Recording finished: last partial step, zero-padded (same as WavReader.forEachChunk).
            if (filled > 0) {
                vad.acceptWaveform(pending.copyOf(filled).copyOf(window))
                drain(vad, classifier, pieces)
            }
            vad.flush()
            drain(vad, classifier, pieces)
        } finally {
            vad.release()
        }
        return pieces
    }

    /** Classifies every speech region the VAD has finished. */
    private suspend fun drain(vad: Vad, classifier: RegionClassifier, out: MutableList<ClassifiedPiece>) {
        while (!vad.empty()) {
            coroutineContext.ensureActive()             // stop promptly after cancel()
            val segment = vad.front()
            val start = segment.start.toLong()
            out += classifier.classify(start, start + segment.samples.size, segment.samples)
            vad.pop()
        }
    }
}
