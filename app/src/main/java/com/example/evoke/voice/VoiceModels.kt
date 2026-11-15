package com.example.evoke.voice

import android.content.Context
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractor
import com.k2fsa.sherpa.onnx.SpeakerEmbeddingExtractorConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig

/**
 * Loads the two ONNX models through sherpa-onnx.
 *
 * - The speaker extractor (CAM++, see VoiceConfig.SPEAKER_MODEL_ASSET) is the
 *   heavy one and stateless → one shared instance.
 * - Silero VAD keeps internal state per audio stream → a fresh instance per job
 *   (cheap: the model is ~2 MB).
 */
object VoiceModels {

    @Volatile
    private var extractor: SpeakerEmbeddingExtractor? = null

    /** sherpa-onnx's extractor is not documented as thread-safe; guard calls with this. */
    internal val extractorLock = Any()

    /** Returns the names of model files missing from assets (empty = all good). */
    fun missingModels(context: Context): List<String> {
        val present = context.assets.list("")?.toSet() ?: emptySet()
        return listOf(VoiceConfig.VAD_MODEL_ASSET, VoiceConfig.SPEAKER_MODEL_ASSET)
            .filterNot { it in present }
    }

    fun requireModels(context: Context) {
        val missing = missingModels(context)
        check(missing.isEmpty()) {
            "Missing model file(s) in app/src/main/assets: ${missing.joinToString()}"
        }
    }

    fun speakerExtractor(context: Context): SpeakerEmbeddingExtractor {
        extractor?.let { return it }
        return synchronized(this) {
            extractor ?: run {
                requireModels(context)
                SpeakerEmbeddingExtractor(
                    assetManager = context.applicationContext.assets,
                    config = SpeakerEmbeddingExtractorConfig(
                        model = VoiceConfig.SPEAKER_MODEL_ASSET,
                        numThreads = VoiceConfig.speakerThreads(),
                        debug = false,
                        provider = "cpu",
                    )
                ).also { extractor = it }
            }
        }
    }

    /** Creates a new Silero VAD. Call release() on it when done. */
    fun createVad(context: Context): Vad {
        requireModels(context)
        return Vad(
            assetManager = context.applicationContext.assets,
            config = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = VoiceConfig.VAD_MODEL_ASSET,
                    threshold = VoiceConfig.VAD_THRESHOLD,
                    minSilenceDuration = VoiceConfig.VAD_MIN_SILENCE_SEC,
                    minSpeechDuration = VoiceConfig.VAD_MIN_SPEECH_SEC,
                    windowSize = VoiceConfig.VAD_WINDOW_SIZE,
                    maxSpeechDuration = VoiceConfig.VAD_MAX_SPEECH_SEC,
                ),
                sampleRate = VoiceConfig.SAMPLE_RATE,
                numThreads = 1,
                provider = "cpu",
                debug = false,
            )
        )
    }

    /** Frees the extractor (e.g. from Application.onTrimMemory). */
    fun release() {
        synchronized(this) {
            synchronized(extractorLock) {
                extractor?.release()
                extractor = null
            }
        }
    }
}
