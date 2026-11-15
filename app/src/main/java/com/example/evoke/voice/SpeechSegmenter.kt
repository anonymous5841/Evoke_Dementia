package com.example.evoke.voice

import android.content.Context
import com.k2fsa.sherpa.onnx.Vad
import java.io.File

/** A stretch of speech, in samples, inside the original audio. */
data class SpeechRegion(val startSample: Long, val endSample: Long) {
    val lengthSamples: Long get() = endSample - startSample
    val durationSec: Float get() = lengthSamples.toFloat() / VoiceConfig.SAMPLE_RATE
}

/** Step "Silero VAD → Speech Segments". */
class SpeechSegmenter(private val context: Context) {

    /** Finds speech in a WAV file, streaming it (works for long recordings). */
    fun detect(wavFile: File): List<SpeechRegion> {
        val vad = VoiceModels.createVad(context)
        val regions = mutableListOf<SpeechRegion>()
        try {
            WavReader(wavFile).use { reader ->
                require(reader.sampleRate == VoiceConfig.SAMPLE_RATE) {
                    "Audio must be ${VoiceConfig.SAMPLE_RATE} Hz (got ${reader.sampleRate})"
                }
                reader.forEachChunk(VoiceConfig.VAD_WINDOW_SIZE) { chunk ->
                    vad.acceptWaveform(chunk)
                    drain(vad, regions)
                }
            }
            vad.flush()
            drain(vad, regions)
        } finally {
            vad.release()
        }
        return regions.sortedBy { it.startSample }
    }

    /** Finds speech in samples already in memory (short voice samples). */
    fun detect(samples: FloatArray): List<SpeechRegion> {
        val vad = VoiceModels.createVad(context)
        val regions = mutableListOf<SpeechRegion>()
        try {
            val window = VoiceConfig.VAD_WINDOW_SIZE
            var pos = 0
            while (pos < samples.size) {
                val end = minOf(pos + window, samples.size)
                val chunk = samples.copyOfRange(pos, end)
                vad.acceptWaveform(if (chunk.size == window) chunk else chunk.copyOf(window))
                drain(vad, regions)
                pos += window
            }
            vad.flush()
            drain(vad, regions)
        } finally {
            vad.release()
        }
        return regions.sortedBy { it.startSample }
    }

    private fun drain(vad: Vad, out: MutableList<SpeechRegion>) {
        while (!vad.empty()) {
            val segment = vad.front()
            val start = segment.start.toLong()
            out += SpeechRegion(start, start + segment.samples.size)
            vad.pop()
        }
    }
}
