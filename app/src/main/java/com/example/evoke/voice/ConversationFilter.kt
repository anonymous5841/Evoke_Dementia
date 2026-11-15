package com.example.evoke.voice

import android.content.Context
import java.io.File

enum class Speaker { PATIENT, PARTNER, UNKNOWN }

/** One analysed piece of speech (times are in the ORIGINAL recording). */
data class ClassifiedPiece(
    val startSample: Long,
    val endSample: Long,
    val speaker: Speaker,
    /** Best cosine score against the patient / partner; null if too short to embed. */
    val patientScore: Float?,
    val partnerScore: Float?,
    /** True if the piece was too short and took its label from a neighbour. */
    val inherited: Boolean
) {
    val startMs: Long get() = startSample * 1000 / VoiceConfig.SAMPLE_RATE
    val endMs: Long get() = endSample * 1000 / VoiceConfig.SAMPLE_RATE
    val durationMs: Long get() = endMs - startMs
}

/** A kept segment, as written to the filtered audio. */
data class FilteredSegment(
    val speaker: Speaker,
    val startMs: Long,          // position in the original recording
    val endMs: Long,
    val filteredStartMs: Long,  // position in the filtered WAV
    val similarity: Float       // best score of the pieces merged into it
)

data class FilterResult(
    val filteredFile: File,
    val originalDurationMs: Long,
    val filteredDurationMs: Long,
    val segments: List<FilteredSegment>,
    /** Every analysed piece, kept or removed — useful for debugging/tuning. */
    val pieces: List<ClassifiedPiece>,
    val patientSpeechMs: Long,
    val partnerSpeechMs: Long,
    val removedSpeechMs: Long
) {
    val hasKeptSpeech: Boolean get() = segments.isNotEmpty()
}

/**
 * Step:
 *   Recorded Conversation → Silero VAD → Speech Segments → speaker model
 *   → compare each segment with patient + partner → KEEP / REMOVE
 *   → Filtered Conversation
 *
 * Long segments are split into ~2 s windows so a turn change without a pause
 * is still detected. Very short segments (< 1 s) can't be verified reliably;
 * they inherit the label of a nearby verified piece, otherwise they are removed.
 *
 * This class filters a finished recording in one go. The same work can run
 * while the conversation is being recorded — see [LiveConversationFilter].
 * Both use [RegionClassifier] and [FilterAssembler], so they give identical
 * results for the same audio.
 */
class ConversationFilter(context: Context) {

    private val segmenter = SpeechSegmenter(context)
    private val embedder = SpeakerEmbedder(context)

    fun filter(
        rawWav: File,
        patientEmbeddings: List<FloatArray>,
        partnerEmbeddings: List<FloatArray>,
        outputWav: File,
        threshold: Float = VoiceConfig.SPEAKER_MATCH_THRESHOLD
    ): FilterResult {
        require(patientEmbeddings.isNotEmpty()) { "No patient voice sample enrolled" }
        require(partnerEmbeddings.isNotEmpty()) { "No partner voice sample enrolled" }

        val regions = segmenter.detect(rawWav)
        val classifier = RegionClassifier(embedder, patientEmbeddings, partnerEmbeddings, threshold)

        // 1. Split long regions into windows and classify each window.
        val raw = mutableListOf<ClassifiedPiece>()
        WavReader(rawWav).use { reader ->
            for (region in regions) {
                val samples = reader.read(region.startSample, region.lengthSamples.toInt())
                raw += classifier.classify(region.startSample, region.endSample, samples)
            }
        }

        // 2.–4. Short pieces, merging, filtered WAV.
        return FilterAssembler.assemble(rawWav, raw, outputWav)
    }
}

/**
 * Splits one VAD speech region into windows and labels each window
 * PATIENT / PARTNER / UNKNOWN by comparing its voiceprint with the stored ones.
 */
internal class RegionClassifier(
    private val embedder: SpeakerEmbedder,
    private val patientEmbeddings: List<FloatArray>,
    private val partnerEmbeddings: List<FloatArray>,
    private val threshold: Float
) {

    /**
     * @param regionStart first sample of the region in the recording
     * @param regionEnd   end sample (exclusive) of the region in the recording
     * @param samples     the region's audio, starting at [regionStart]
     *                    (may be a little shorter than the region at the very
     *                    end of a file)
     */
    fun classify(regionStart: Long, regionEnd: Long, samples: FloatArray): List<ClassifiedPiece> {
        val sr = VoiceConfig.SAMPLE_RATE
        val out = mutableListOf<ClassifiedPiece>()

        for ((start, end) in splitRegion(regionStart, regionEnd)) {
            val len = (end - start).toInt()
            if (len < VoiceConfig.MIN_EMBED_SEC * sr) {
                out += ClassifiedPiece(start, end, Speaker.UNKNOWN, null, null, inherited = true)
                continue
            }
            val to = (end - regionStart).toInt().coerceIn(0, samples.size)
            val from = (start - regionStart).toInt().coerceIn(0, to)
            val embedding = embedder.embed(samples.copyOfRange(from, to))
            if (embedding == null) {
                out += ClassifiedPiece(start, end, Speaker.UNKNOWN, null, null, inherited = true)
                continue
            }
            val p = EmbeddingMath.bestScore(embedding, patientEmbeddings)
            val q = EmbeddingMath.bestScore(embedding, partnerEmbeddings)
            val speaker = when {
                maxOf(p, q) < threshold -> Speaker.UNKNOWN
                p >= q -> Speaker.PATIENT
                else -> Speaker.PARTNER
            }
            out += ClassifiedPiece(start, end, speaker, p, q, inherited = false)
        }
        return out
    }

    /** Splits a region longer than MAX_WINDOW_SEC into equal ~TARGET_WINDOW_SEC pieces. */
    private fun splitRegion(regionStart: Long, regionEnd: Long): List<Pair<Long, Long>> {
        val sr = VoiceConfig.SAMPLE_RATE
        val maxLen = (VoiceConfig.MAX_WINDOW_SEC * sr).toLong()
        val target = (VoiceConfig.TARGET_WINDOW_SEC * sr).toLong()
        val len = regionEnd - regionStart
        if (len <= maxLen) return listOf(regionStart to regionEnd)

        val count = ((len + target - 1) / target).toInt()  // ceil
        val pieceLen = len / count
        return (0 until count).map { i ->
            val s = regionStart + i * pieceLen
            val e = if (i == count - 1) regionEnd else s + pieceLen
            s to e
        }
    }
}

/**
 * Turns the classified pieces of a whole recording into the filtered WAV:
 * short pieces inherit a neighbour's label, kept pieces are merged, and the
 * kept audio is copied from [rawWav] into the output file.
 */
internal object FilterAssembler {

    fun assemble(rawWav: File, raw: List<ClassifiedPiece>, outputWav: File): FilterResult {
        val sr = VoiceConfig.SAMPLE_RATE

        WavReader(rawWav).use { reader ->

            // 2. Short pieces inherit the label of the nearest verified piece.
            val pieces = resolveShortPieces(raw)

            // 3. Merge neighbouring kept pieces of the same speaker.
            val merged = mergeKept(pieces)

            // 4. Write the filtered WAV.
            outputWav.parentFile?.mkdirs()
            val gapSamples = VoiceConfig.FILTERED_GAP_MS * sr / 1000
            val segments = mutableListOf<FilteredSegment>()
            WavWriter(outputWav, sr).use { writer ->
                merged.forEachIndexed { index, m ->
                    if (index > 0) writer.writeSilence(gapSamples)
                    val filteredStartMs = writer.samplesWritten * 1000 / sr
                    copySamples(reader, writer, m.startSample, m.endSample)
                    segments += FilteredSegment(
                        speaker = m.speaker,
                        startMs = m.startSample * 1000 / sr,
                        endMs = m.endSample * 1000 / sr,
                        filteredStartMs = filteredStartMs,
                        similarity = m.similarity
                    )
                }
            }
            val filteredDurationMs = WavReader(outputWav).use { it.durationMs }

            val patientMs = pieces.filter { it.speaker == Speaker.PATIENT }.sumOf { it.durationMs }
            val partnerMs = pieces.filter { it.speaker == Speaker.PARTNER }.sumOf { it.durationMs }
            val removedMs = pieces.filter { it.speaker == Speaker.UNKNOWN }.sumOf { it.durationMs }

            return FilterResult(
                filteredFile = outputWav,
                originalDurationMs = reader.durationMs,
                filteredDurationMs = filteredDurationMs,
                segments = segments,
                pieces = pieces,
                patientSpeechMs = patientMs,
                partnerSpeechMs = partnerMs,
                removedSpeechMs = removedMs
            )
        }
    }

    private fun resolveShortPieces(raw: List<ClassifiedPiece>): List<ClassifiedPiece> {
        val maxGap = (VoiceConfig.INHERIT_MAX_GAP_SEC * VoiceConfig.SAMPLE_RATE).toLong()
        val verified = raw.filter { !it.inherited && it.speaker != Speaker.UNKNOWN }

        return raw.map { piece ->
            if (!piece.inherited) return@map piece
            val nearest = verified.minByOrNull { gapBetween(piece, it) }
            if (nearest != null && gapBetween(piece, nearest) <= maxGap) {
                piece.copy(speaker = nearest.speaker)
            } else {
                piece.copy(speaker = Speaker.UNKNOWN)
            }
        }
    }

    private fun gapBetween(a: ClassifiedPiece, b: ClassifiedPiece): Long = when {
        a.endSample <= b.startSample -> b.startSample - a.endSample
        b.endSample <= a.startSample -> a.startSample - b.endSample
        else -> 0L
    }

    private data class MergedSegment(
        val speaker: Speaker,
        val startSample: Long,
        val endSample: Long,
        val similarity: Float
    )

    private fun mergeKept(pieces: List<ClassifiedPiece>): List<MergedSegment> {
        val maxGap = (VoiceConfig.MERGE_GAP_SEC * VoiceConfig.SAMPLE_RATE).toLong()
        val result = mutableListOf<MergedSegment>()

        for (p in pieces.filter { it.speaker != Speaker.UNKNOWN }.sortedBy { it.startSample }) {
            val score = when (p.speaker) {
                Speaker.PATIENT -> p.patientScore
                Speaker.PARTNER -> p.partnerScore
                Speaker.UNKNOWN -> null
            } ?: 0f
            val last = result.lastOrNull()
            if (last != null && last.speaker == p.speaker && p.startSample - last.endSample <= maxGap) {
                result[result.lastIndex] = last.copy(
                    endSample = maxOf(last.endSample, p.endSample),
                    similarity = maxOf(last.similarity, score)
                )
            } else {
                result += MergedSegment(p.speaker, p.startSample, p.endSample, score)
            }
        }
        return result
    }

    private fun copySamples(reader: WavReader, writer: WavWriter, start: Long, end: Long) {
        val chunk = VoiceConfig.SAMPLE_RATE // 1 s at a time
        var pos = start
        while (pos < end) {
            val n = minOf(chunk.toLong(), end - pos).toInt()
            writer.writeFloat(reader.read(pos, n))
            pos += n
        }
    }
}
