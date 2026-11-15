package com.example.evoke.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.sqrt

/**
 * Records 16 kHz, 16-bit mono audio straight into a WAV file.
 *
 * Usage:
 *   val recorder = VoiceRecorder(context)
 *   scope.launch { val result = recorder.record(file, maxMs) }  // suspends while recording
 *   recorder.stop()                                             // finishes recording
 *
 * Cancelling the coroutine also stops and releases the microphone.
 */
class VoiceRecorder(private val context: Context) {

    data class Result(val file: File, val durationMs: Long)

    private val _level = MutableStateFlow(0f)          // 0..1, for a level meter
    val level: StateFlow<Float> = _level.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    @Volatile
    private var stopRequested = false

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /** Asks the current recording to finish. record() then returns normally. */
    fun stop() {
        stopRequested = true
    }

    /**
     * @param onAudio optional: receives every block of samples right after it
     *                is written to [outputFile] (e.g. LiveConversationFilter::accept,
     *                to filter while recording). Called on the recording thread;
     *                it must return quickly and must copy the samples it keeps,
     *                because the buffer is reused for the next block.
     */
    @SuppressLint("MissingPermission") // checked by hasPermission() below
    suspend fun record(
        outputFile: File,
        maxDurationMs: Long,
        onAudio: ((ShortArray, Int) -> Unit)? = null
    ): Result = withContext(Dispatchers.IO) {
        if (!hasPermission()) throw SecurityException("RECORD_AUDIO permission not granted")

        val sampleRate = VoiceConfig.SAMPLE_RATE
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) throw IOException("16 kHz mono recording is not supported on this device")

        val readSize = sampleRate / 10                     // 100 ms per read
        val recorder = AudioRecord(
            // VOICE_RECOGNITION: no automatic gain / effects → cleaner voice for the models
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, readSize * 2 * 4)
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IOException("Could not open the microphone")
        }

        outputFile.parentFile?.mkdirs()
        val maxSamples = maxDurationMs * sampleRate / 1000
        var totalSamples = 0L
        stopRequested = false
        _elapsedMs.value = 0L
        _isRecording.value = true

        try {
            WavWriter(outputFile, sampleRate).use { writer ->
                val buffer = ShortArray(readSize)
                recorder.startRecording()
                while (isActive && !stopRequested && totalSamples < maxSamples) {
                    val n = recorder.read(buffer, 0, buffer.size)
                    if (n < 0) throw IOException("Microphone read error: $n")
                    if (n == 0) continue
                    writer.writePcm16(buffer, n)
                    onAudio?.invoke(buffer, n)
                    totalSamples += n
                    _level.value = rmsLevel(buffer, n)
                    _elapsedMs.value = totalSamples * 1000 / sampleRate
                }
            }
        } catch (e: Exception) {
            outputFile.delete()
            throw e
        } finally {
            try {
                recorder.stop()
            } catch (e: IllegalStateException) {
                // Recording never started (e.g. startRecording failed) — nothing to stop.
            }
            recorder.release()
            _isRecording.value = false
            _level.value = 0f
        }

        Result(outputFile, totalSamples * 1000 / sampleRate)
    }

    companion object {
        /** A new temporary WAV file in the cache folder for a recording. */
        fun newTempFile(context: Context, prefix: String): File =
            File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.wav")
    }

    private fun rmsLevel(buffer: ShortArray, n: Int): Float {
        var sum = 0.0
        for (i in 0 until n) {
            val v = buffer[i] / 32768.0
            sum += v * v
        }
        // Scale so normal speech fills most of the meter.
        return (sqrt(sum / n) * 4).toFloat().coerceIn(0f, 1f)
    }
}
