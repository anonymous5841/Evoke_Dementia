package com.example.evoke.voice

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/** Step "speaker model (CAM++) → speaker embedding". */
class SpeakerEmbedder(context: Context) {

    private val appContext = context.applicationContext

    init {
        // Load the model now (off the UI thread, as before), so the first
        // embed() call does not pay for it.
        VoiceModels.speakerExtractor(appContext)
    }

    /** Embedding size of the loaded model. */
    val dimension: Int get() = VoiceModels.speakerExtractor(appContext).dim()

    /**
     * Returns an L2-normalised embedding, or null if the audio is too short
     * for the model to produce one.
     *
     * The model is fetched on every call instead of being kept: a live
     * filter can run for 30 minutes, and VoiceModels.release() (e.g. called
     * from Application.onTrimMemory) may free the model meanwhile; it is then simply
     * loaded again. Locks are taken in the same order as release()
     * (VoiceModels, then extractorLock), so the two cannot deadlock.
     */
    fun embed(samples: FloatArray): FloatArray? {
        if (samples.isEmpty()) return null
        synchronized(VoiceModels) {
            synchronized(VoiceModels.extractorLock) {
                val extractor = VoiceModels.speakerExtractor(appContext)
                val stream = extractor.createStream()
                try {
                    stream.acceptWaveform(samples, VoiceConfig.SAMPLE_RATE)
                    stream.inputFinished()
                    if (!extractor.isReady(stream)) return null
                    return EmbeddingMath.l2Normalize(extractor.compute(stream))
                } finally {
                    stream.release()
                }
            }
        }
    }
}

/** Embedding helpers: (de)serialisation for Room and cosine similarity. */
object EmbeddingMath {

    /** FloatArray → ByteArray (little-endian float32) for the `embedding` BLOB column. */
    fun toBytes(embedding: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(embedding.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        embedding.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    fun fromBytes(bytes: ByteArray): FloatArray {
        require(bytes.size % 4 == 0) { "Invalid embedding blob size ${bytes.size}" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return FloatArray(bytes.size / 4) { buffer.get(it) }
    }

    fun l2Normalize(v: FloatArray): FloatArray {
        var sum = 0.0
        v.forEach { sum += it * it }
        val norm = sqrt(sum).toFloat()
        return if (norm < 1e-10f) v.copyOf() else FloatArray(v.size) { v[it] / norm }
    }

    fun cosine(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embedding sizes differ: ${a.size} vs ${b.size}" }
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in a.indices) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = sqrt(na) * sqrt(nb)
        return if (denom < 1e-10) 0f else (dot / denom).toFloat()
    }

    /** Best (highest) similarity of [embedding] against a speaker's stored samples. */
    fun bestScore(embedding: FloatArray, references: List<FloatArray>): Float =
        references.maxOfOrNull { cosine(embedding, it) } ?: -1f
}
