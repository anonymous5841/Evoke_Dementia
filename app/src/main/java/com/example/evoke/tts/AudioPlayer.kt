package com.example.evoke.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.tanh
import android.media.PlaybackParams

class AudioPlayer {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    private data class CachedAudio(
        val text: String,
        val language: TtsLanguage,
        val audio: TtsAudio,
        val pcm: ByteArray
    )

    @Volatile
    private var requestedPlaybackSpeed = 1f

    fun setPlaybackSpeed(speed: Float) {
        require(speed in 0.5f..2f) {
            "Playback speed must be between 0.5x and 2x."
        }

        requestedPlaybackSpeed = speed

        synchronized(playbackLock) {
            val track = currentTrack ?: return

            try {
                track.playbackParams =
                    PlaybackParams().setSpeed(speed)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Could not change playback speed", e)
            } catch (e: IllegalStateException) {
                Log.e(TAG, "AudioTrack is not ready for speed change", e)
            }
        }
    }

    @Volatile
    private var currentTrack: AudioTrack? = null

    @Volatile
    private var pauseRequested = false

    @Volatile
    private var speechActive = false

    private val playbackLock = Any()

    // Retain only the latest generated audio in memory.
    @Volatile
    private var cachedAudio: CachedAudio? = null

    @Volatile
    var onPlaybackCompleted: (() -> Unit)? = null

    suspend fun speak(
        ttsApi: TtsApi,
        text: String,
        language: TtsLanguage
    ) {
        // If speech is already being generated or played,
        // this Play request resumes that operation.
        val shouldStart = synchronized(playbackLock) {
            if (speechActive) {
                pauseRequested = false

                val track = currentTrack
                if (track?.playState == AudioTrack.PLAYSTATE_PAUSED) {
                    track.play()
                }

                false
            } else {
                speechActive = true
                pauseRequested = false
                true
            }
        }

        if (!shouldStart) return

        try {
            var cached = cachedAudio

            // Generate and convert only when the requested
            // text or language differs from the cached result.
            if (
                cached == null ||
                cached.text != text ||
                cached.language != language
            ) {
                val audio = ttsApi.generateSpeech(text, language)

                require(audio.samples.isNotEmpty()) {
                    "Audio samples are empty."
                }

                val pcm = withContext(Dispatchers.Default) {
                    floatSamplesToPcm16(audio.samples)
                }

                cached = CachedAudio(
                    text = text,
                    language = language,
                    audio = audio,
                    pcm = pcm
                )

                cachedAudio = cached
            }

            play(cached)
        } finally {
            synchronized(playbackLock) {
                speechActive = false
                pauseRequested = false
            }
        }
    }

    private suspend fun play(cached: CachedAudio) =
        withContext(Dispatchers.IO) {
            val audio = cached.audio
            val pcm = cached.pcm

            val playbackRate =
                (audio.sampleRate * audio.playbackSpeed)
                    .roundToInt()
                    .coerceAtLeast(1)

            val minBufferSize = AudioTrack.getMinBufferSize(
                playbackRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            check(minBufferSize > 0) {
                "Invalid AudioTrack buffer size: $minBufferSize"
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(
                            AudioAttributes.CONTENT_TYPE_SPEECH
                        )
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(playbackRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(maxOf(minBufferSize, 4096))
                .build()

            var completedNormally = false
            try {
                check(track.state == AudioTrack.STATE_INITIALIZED) {
                    "AudioTrack failed to initialize."
                }

                synchronized(playbackLock) {
                    currentTrack = track

                    track.playbackParams =
                        PlaybackParams().setSpeed(requestedPlaybackSpeed)

                    track.play()

                    if (pauseRequested) {
                        track.pause()
                    }
                }

                var offset = 0

                while (offset < pcm.size) {
                    currentCoroutineContext().ensureActive()

                    while (pauseRequested) {
                        currentCoroutineContext().ensureActive()
                        delay(50)
                    }

                    val written = track.write(
                        pcm,
                        offset,
                        pcm.size - offset,
                        AudioTrack.WRITE_BLOCKING
                    )

                    check(written > 0) {
                        "AudioTrack write failed: $written"
                    }

                    offset += written
                }

                while (
                    track.playbackHeadPosition.toLong() <
                    audio.samples.size.toLong()
                ) {
                    currentCoroutineContext().ensureActive()

                    while (pauseRequested) {
                        currentCoroutineContext().ensureActive()
                        delay(50)
                    }

                    delay(20)
                }

                Log.d(
                    TAG,
                    "Playback completed: ${audio.sentenceCount} sentences"
                )
                completedNormally = true
            } finally {
                synchronized(playbackLock) {
                    if (currentTrack === track) {
                        currentTrack = null
                    }
                }

                try {
                    track.stop()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "AudioTrack already stopped", e)
                }

                track.release()

                if (completedNormally) {
                    withContext(Dispatchers.Main) {
                        onPlaybackCompleted?.invoke()
                    }
                }
            }
        }

    fun pause() {
        synchronized(playbackLock) {
            pauseRequested = true

            val track = currentTrack ?: return

            try {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Could not pause playback", e)
            }
        }
    }

    fun resume() {
        synchronized(playbackLock) {
            pauseRequested = false

            val track = currentTrack ?: return

            try {
                if (track.playState == AudioTrack.PLAYSTATE_PAUSED) {
                    track.play()
                }
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Could not resume playback", e)
            }
        }
    }

    private fun floatSamplesToPcm16(
        samples: FloatArray
    ): ByteArray {
        val output = ByteArray(samples.size * 2)
        val gain = 2.5f

        for (i in samples.indices) {
            val amplified = samples[i] * gain
            val limited = tanh(amplified.toDouble()).toFloat()

            val pcm = (
                    limited.coerceIn(-1f, 1f) * 32767f
                    ).roundToInt().coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt()
                )

            output[i * 2] = (pcm and 0xFF).toByte()
            output[i * 2 + 1] = ((pcm shr 8) and 0xFF).toByte()
        }

        return output
    }
}
