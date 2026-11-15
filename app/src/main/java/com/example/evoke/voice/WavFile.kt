package com.example.evoke.voice

import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal WAV helpers for 16-bit PCM mono audio.
 *
 * Audio is streamed to / from disk, so a 30-minute conversation never has to
 * fit in memory at once.
 */

/** Writes 16-bit PCM mono WAV. The header is filled in on close(). */
class WavWriter(
    file: File,
    private val sampleRate: Int = VoiceConfig.SAMPLE_RATE
) : Closeable {

    private val raf = RandomAccessFile(file, "rw")
    private var dataBytes = 0L
    private var closed = false

    init {
        raf.setLength(0)
        raf.write(ByteArray(HEADER_SIZE)) // placeholder, rewritten in close()
    }

    fun writePcm16(buffer: ShortArray, count: Int = buffer.size) {
        val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until count) bytes.putShort(buffer[i])
        raf.write(bytes.array())
        dataBytes += count * 2L
    }

    /** Writes float samples in -1..1 (clamped). */
    fun writeFloat(samples: FloatArray, offset: Int = 0, count: Int = samples.size - offset) {
        val bytes = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in offset until offset + count) {
            val v = (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt()
            bytes.putShort(v.toShort())
        }
        raf.write(bytes.array())
        dataBytes += count * 2L
    }

    fun writeSilence(sampleCount: Int) {
        if (sampleCount <= 0) return
        raf.write(ByteArray(sampleCount * 2))
        dataBytes += sampleCount * 2L
    }

    val samplesWritten: Long get() = dataBytes / 2

    override fun close() {
        if (closed) return
        closed = true
        try {
            raf.seek(0)
            raf.write(buildHeader(dataBytes, sampleRate))
        } finally {
            raf.close()
        }
    }

    companion object {
        const val HEADER_SIZE = 44

        private fun buildHeader(dataBytes: Long, sampleRate: Int): ByteArray {
            val channels = 1
            val bitsPerSample = 16
            val byteRate = sampleRate * channels * bitsPerSample / 8
            return ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt((36 + dataBytes).toInt())
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16)                                   // fmt chunk size
                putShort(1)                                  // PCM
                putShort(channels.toShort())
                putInt(sampleRate)
                putInt(byteRate)
                putShort((channels * bitsPerSample / 8).toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(dataBytes.toInt())
            }.array()
        }
    }
}

/** Reads 16-bit PCM mono WAV written by [WavWriter] (or any compatible file). */
class WavReader(file: File) : Closeable {

    private val raf = RandomAccessFile(file, "r")
    private val dataOffset: Long
    val sampleRate: Int
    val numSamples: Long

    init {
        try {
            val riff = ByteArray(12)
            raf.readFully(riff)
            if (String(riff, 0, 4, Charsets.US_ASCII) != "RIFF" ||
                String(riff, 8, 4, Charsets.US_ASCII) != "WAVE"
            ) throw IOException("Not a WAV file: ${file.name}")

            var foundRate = -1
            var foundData = -1L
            var foundDataSize = 0L

            // Walk the chunks to find "fmt " and "data".
            val chunkHeader = ByteArray(8)
            while (raf.filePointer + 8 <= raf.length()) {
                raf.readFully(chunkHeader)
                val id = String(chunkHeader, 0, 4, Charsets.US_ASCII)
                val size = ByteBuffer.wrap(chunkHeader, 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
                when (id) {
                    "fmt " -> {
                        val fmt = ByteArray(size.toInt())
                        raf.readFully(fmt)
                        val b = ByteBuffer.wrap(fmt).order(ByteOrder.LITTLE_ENDIAN)
                        val format = b.short.toInt()
                        val channels = b.short.toInt()
                        foundRate = b.int
                        b.int // byte rate
                        b.short // block align
                        val bits = b.short.toInt()
                        if (format != 1 || channels != 1 || bits != 16) {
                            throw IOException(
                                "Only 16-bit PCM mono WAV is supported " +
                                    "(format=$format, channels=$channels, bits=$bits)"
                            )
                        }
                    }
                    "data" -> {
                        foundData = raf.filePointer
                        // Some writers leave size 0 / wrong; trust the file length.
                        val available = raf.length() - foundData
                        foundDataSize = if (size in 1..available) size else available
                        break
                    }
                    else -> raf.seek(raf.filePointer + size + (size and 1))
                }
            }

            if (foundRate <= 0 || foundData < 0) throw IOException("Invalid WAV file: ${file.name}")
            sampleRate = foundRate
            dataOffset = foundData
            numSamples = foundDataSize / 2
        } catch (e: Exception) {
            raf.close()
            throw e
        }
    }

    val durationMs: Long get() = numSamples * 1000L / sampleRate

    /** Reads [count] samples starting at [startSample] as floats in -1..1. */
    fun read(startSample: Long, count: Int): FloatArray {
        val start = startSample.coerceIn(0, numSamples)
        val n = count.toLong().coerceAtMost(numSamples - start).toInt().coerceAtLeast(0)
        val bytes = ByteArray(n * 2)
        raf.seek(dataOffset + start * 2)
        raf.readFully(bytes)
        val shorts = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        return FloatArray(n) { shorts.get(it) / 32768f }
    }

    fun readAll(): FloatArray {
        require(numSamples <= Int.MAX_VALUE) { "File too large to read at once" }
        return read(0, numSamples.toInt())
    }

    /**
     * Streams the whole file in chunks of exactly [chunkSize] samples
     * (the last chunk is zero-padded).
     */
    inline fun forEachChunk(chunkSize: Int, block: (FloatArray) -> Unit) {
        var pos = 0L
        while (pos < numSamples) {
            val chunk = read(pos, chunkSize)
            block(if (chunk.size == chunkSize) chunk else chunk.copyOf(chunkSize))
            pos += chunkSize
        }
    }

    override fun close() = raf.close()
}
