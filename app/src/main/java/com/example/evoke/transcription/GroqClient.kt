package com.example.evoke.transcription

import android.util.Log
import com.example.evoke.BuildConfig
import com.example.evoke.voice.FilteredSegment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ONLINE transcription + summary with Groq (free tier).
 *
 *   filtered audio (ByteArray, WAV 16 kHz mono)
 *     ──► Groq Whisper large-v3            → what was said (Urdu script, mixed Urdu/English)
 *     ──► Groq gpt-oss-120b (translation)  → English transcript   (variable, no database yet)
 *     ──► Groq gpt-oss-120b (summary)      → English summary      (variable, no database yet)
 *
 * Why two steps instead of Whisper's own "translate" mode: in the Colab test the direct
 * translation invented sentences and echoed its hint ("Translated to English."), while
 * Whisper's Urdu transcription was accurate. METHOD switches between the two.
 */
object GroqConfig {

    // ── [API KEY] ─────────────────────────────────────────────────────────────
    /** From local.properties: GROQ_API_KEY=gsk_...  (never put the key in code / GitHub) */
    val API_KEY: String get() = BuildConfig.GROQ_API_KEY

    // ── [ENDPOINTS] ───────────────────────────────────────────────────────────
    const val TRANSCRIBE_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
    const val TRANSLATE_URL = "https://api.groq.com/openai/v1/audio/translations"
    const val CHAT_URL = "https://api.groq.com/openai/v1/chat/completions"

    // ── [MODELS] ──────────────────────────────────────────────────────────────
    /** whisper-large-v3 (turbo cannot translate, and is less accurate for Urdu). */
    const val WHISPER_MODEL = "whisper-large-v3"

    /** Translation + summary model. Backup if it is removed/busy: "openai/gpt-oss-20b". */
    const val TEXT_MODEL = "openai/gpt-oss-120b"

    // ── [TRANSCRIPTION METHOD] ────────────────────────────────────────────────
    enum class Method {
        /** Whisper writes Urdu → gpt-oss translates to English (recommended, faithful). */
        TWO_STEP,

        /** Whisper translates directly to English (faster, but invented text in tests). */
        DIRECT
    }
    val METHOD = Method.TWO_STEP

    /** Spoken language for Whisper (mixed Urdu/English is written in Urdu script). */
    const val WHISPER_LANGUAGE = "ur"

    // ── [UNSURE PARTS] ────────────────────────────────────────────────────────
    /** Whisper parts it is unsure about are removed (fan noise, silence, loops). */
    const val DROP_IF_NO_SPEECH_PROB = 0.6     // … together with avg_logprob below -1.0
    const val DROP_IF_AVG_LOGPROB = -1.0
    const val DROP_IF_COMPRESSION_RATIO = 2.4  // text repeating itself

    // ── [AUDIO UPLOAD] ────────────────────────────────────────────────────────
    /**
     * Groq accepts files up to 25 MB. WAV 16 kHz mono = 1.92 MB per minute,
     * so audio longer than 10 minutes is sent in pieces of at most 10 minutes,
     * cut between speaker turns (never inside a word).
     */
    const val MAX_PIECE_SEC = 600

    // ── [TRANSLATION] ─────────────────────────────────────────────────────────
    /** Long transcripts are translated in pieces (keeps under 8,000 tokens per minute). */
    const val TRANSLATE_PIECE_WORDS = 600

    // ── [RETRIES] ─────────────────────────────────────────────────────────────
    /** Groq busy / rate limit (429, 5xx): retry this many times, waiting 10 s, 20 s, … */
    const val MAX_RETRIES = 5

    /** Low temperature = faithful, no creativity. */
    const val TEMPERATURE = 0.0   // 0 = same input gives (almost) the same output

    // ── [SUMMARY LENGTH] ──────────────────────────────────────────────────────
    /** Longest conversation of the FYP: 30 minutes. */
    const val MAX_CONVERSATION_MINUTES = 30

    /** Summary is at most 1/4 of the English transcript … */
    const val SUMMARY_RATIO = 4

    /** … but never more than 250 words (the limit for a full 30-minute conversation). */
    const val SUMMARY_MAX_WORDS = 250

    /** Very short conversations still get at least this many words. */
    const val SUMMARY_MIN_WORDS = 25

    fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    fun summaryWordLimit(transcript: String): Int =
        (wordCount(transcript) / SUMMARY_RATIO).coerceIn(SUMMARY_MIN_WORDS, SUMMARY_MAX_WORDS)

    // ── [TRANSLATION PROMPT] ──────────────────────────────────────────────────
    fun translationPrompt(urduText: String) = """
You are translating a transcript of a spoken conversation between two
people in Pakistan. They speak mixed Urdu and English. The transcript is written
in Urdu script, and English words may also be written in Urdu script
(for example "ٹریولنگ" = "travelling", "اپلائے" = "apply").

Translate it into natural, simple ENGLISH.

RULES:
- Translate faithfully, sentence by sentence. Do NOT summarize.
- Do NOT add anything that is not in the transcript. Do NOT remove information.
- Keep people's names and place names as they are.
- Keep numbers, times and dates exactly as they were said.
- If a part is unclear or makes no sense, translate it as literally as you can; do not guess.
- NEVER add notes, comments, explanations, guesses or ANY text in brackets,
  not even to explain a word (no "(clinic)", "(cabinet)", "(unsure)").
  Output only the translated speech itself.
- Keep "I" / "you" / "we" exactly as the speaker said them (who did what).
- Keep family words and titles exactly as spoken, attached to the name, and do
  NOT turn them into English relations: "Bilal Bhai" stays "Bilal Bhai",
  "Khalid Uncle" stays "Uncle Khalid", also Abu, Ammi, Nana, Nani, Baji, Aunty.
- Return ONLY the English translation.

TRANSCRIPT:
$urduText
""".trimIndent()

    // ── [SUMMARY PROMPT] ──────────────────────────────────────────────────────
    fun summaryPrompt(transcript: String, maxWords: Int = summaryWordLimit(transcript)) = """
You are a memory assistant for a person with dementia.

Summarize the following conversation transcript into a short,
clear and accurate summary.

Keep ALL the important information that would help the
person remember the conversation later. Do NOT miss any important detail.

Prioritize:
- Who the person is
- Their relationship to the user
- Important people mentioned
- Important events
- Future plans
- Dates or times
- Appointments, medicines and money
- Important commitments
- Important personal information
- Important things discussed

Ignore:
- Greetings
- Repeated statements
- Small talk
- Unimportant details

IMPORTANT RULES:
- Do not invent information.
- Do not make assumptions.
- Use only information explicitly present in the transcript.
- Keep important names, relationships, dates, times, places and plans.
- NEVER assume anyone's gender or relationship. Use names and relations ONLY
  exactly as they are said in the transcript; if the speaker's name or
  relation is not said, call them "the speaker".
- Keep exactly WHO did WHAT. The transcript is spoken to the listener ("you"):
  things the speaker did ("I paid", "I gave") were done by the speaker, not by
  the listener. Write them as "The speaker paid…", "The speaker gave…".
- Write the summary TO the listener ("you"), and NEVER use "I", "me", "my",
  "we", "us" or "our" for the speaker: say "the speaker", "the speaker's house",
  "you and the speaker will go together" (or use the speaker's name if it was said).
- Keep family words and titles exactly as in the transcript (e.g. "Bilal Bhai",
  "Uncle Khalid"); do NOT change them into a relation such as "your brother".
- Make the summary easy for an older adult to understand.
- Keep it concise but do not remove important information.
- Write ALL numbers, amounts, times and dates in words, never as digits.
  For example: 700 must be written as "seven hundred",
  3:15 must be written as "three fifteen o'clock",
  10 AM must be written as "ten o'clock in the morning",
  15 December must be written as "fifteenth December",
  2 must be written as "two".
- LANGUAGE: write the summary in simple ENGLISH only. The conversation was
  spoken in mixed Urdu and English, so the transcript may still contain Urdu
  words (in Urdu script or Roman Urdu, e.g. "ammi", "kal", "dawai").
  Translate their meaning into English; keep people's names as they are.
- Write in short, COMPLETE, simple sentences (not notes or headlines), and say
  the time of day for times ("nine o'clock at night", "three o'clock at night").
- LENGTH: the summary must be AT MOST $maxWords words
  (about one quarter of the transcript, never more than $SUMMARY_MAX_WORDS words).
  If the transcript has little important information, write fewer words.

Return ONLY the final summary.

TRANSCRIPT:
$transcript
""".trimIndent()
}

/** Groq answered with an error that retrying will not fix (wrong key, unknown model, file too big …). */
class GroqException(message: String) : IOException(message)

/** Result of [GroqClient.transcribeToEnglish]. */
data class GroqTranscript(
    /** English transcript (what goes to the summary). */
    val english: String,
    /** What Whisper heard (Urdu script) — empty for METHOD = DIRECT. */
    val original: String,
    /** Number of unsure Whisper parts that were removed. */
    val removedParts: Int,
    val pieces: Int,
    val whisperMs: Long,
    val translateMs: Long,
) {
    val totalMs: Long get() = whisperMs + translateMs
}

/** Minimal Groq REST client (HttpURLConnection, no extra library). */
object GroqClient {

    private const val TAG = "GroqClient"
    private const val WAV_HEADER = 44
    private const val BOUNDARY = "----EvokeGroqBoundary7MA4YWxkTrZu0gW"

    private fun now() = System.currentTimeMillis()

    // ── 1. TRANSCRIPTION ────────────────────────────────────────────────────

    /**
     * [filteredAudio] = the whole filtered WAV file in memory (16 kHz, mono, 16-bit).
     * [segments] = speaker turns inside it (used to cut long audio between turns).
     * Returns the English transcript. Throws [GroqException] / [IOException] on failure.
     */
    suspend fun transcribeToEnglish(
        filteredAudio: ByteArray,
        segments: List<FilteredSegment>,
        sampleRate: Int = 16_000,
        onProgress: (String) -> Unit = {}
    ): GroqTranscript = withContext(Dispatchers.IO) {
        requireKey()
        val pcm = findPcm(filteredAudio)
        val bytesPerSec = sampleRate * 2
        val pieces = planPieces(pcm.second, bytesPerSec, segments)

        // Step 1: Whisper
        val whisperStart = now()
        val texts = mutableListOf<String>()
        var removed = 0
        pieces.forEachIndexed { i, (start, length) ->
            onProgress("Groq Whisper: piece ${i + 1} of ${pieces.size} (${length / bytesPerSec} s of audio)…")
            val (text, dropped) = whisper(filteredAudio, pcm.first + start, length, sampleRate)
            texts += text
            removed += dropped
        }
        val whisperText = texts.filter { it.isNotBlank() }.joinToString(" ")
        val whisperMs = now() - whisperStart
        Log.i(TAG, "Whisper (${GroqConfig.METHOD}) heard: $whisperText")

        if (GroqConfig.METHOD == GroqConfig.Method.DIRECT || whisperText.isBlank()) {
            return@withContext GroqTranscript(whisperText, "", removed, pieces.size, whisperMs, 0)
        }

        // Step 2: gpt-oss translation (Urdu → English)
        val translateStart = now()
        val parts = splitWords(whisperText, GroqConfig.TRANSLATE_PIECE_WORDS)
        val english = parts.mapIndexed { i, part ->
            onProgress("Groq translation to English: part ${i + 1} of ${parts.size}…")
            chat(GroqConfig.translationPrompt(part), maxTokens = 4096)
        }.joinToString(" ")
        GroqTranscript(english, whisperText, removed, pieces.size, whisperMs, now() - translateStart)
    }

    // ── 2. SUMMARY ──────────────────────────────────────────────────────────

    /** English transcript → English summary (≤ 1/4 of the transcript, ≤ 250 words). */
    suspend fun summarize(transcript: String): String = withContext(Dispatchers.IO) {
        requireKey()
        val limit = GroqConfig.summaryWordLimit(transcript)
        var summary = chat(GroqConfig.summaryPrompt(transcript), maxTokens = 2048)

        // [LENGTH CHECK] too long (10 % tolerance) -> write the summary again FROM THE
        // TRANSCRIPT with a lower word target (same rules, so nothing is lost or
        // mis-attributed), at most twice. "medium" reasoning keeps the word count better.
        var target = limit
        repeat(2) { attempt ->
            val words = GroqConfig.wordCount(summary)
            if (words <= limit + limit / 10) return@repeat
            target = (target * 0.8).toInt().coerceAtLeast(GroqConfig.SUMMARY_MIN_WORDS)
            Log.i(TAG, "Summary has $words words (limit $limit) - rewriting with target $target, attempt ${attempt + 1}")
            summary = chat(GroqConfig.summaryPrompt(transcript, target), maxTokens = 4096, reasoning = "medium")
        }
        Log.i(TAG, "Final summary: ${GroqConfig.wordCount(summary)} words (limit $limit)")

        // Safety: never more than SUMMARY_MAX_WORDS (the database rejects longer summaries)
        trimToWords(summary, GroqConfig.SUMMARY_MAX_WORDS)
    }

    /** Cuts the text to at most [maxWords] words, at the end of a sentence where possible. */
    private fun trimToWords(text: String, maxWords: Int): String {
        if (GroqConfig.wordCount(text) <= maxWords) return text
        val words = text.trim().split(Regex("\\s+"))
        val cut = words.take(maxWords).joinToString(" ")
        val lastEnd = cut.lastIndexOfAny(charArrayOf('.', '!', '?'))
        return if (lastEnd > cut.length / 2) cut.substring(0, lastEnd + 1) else cut
    }

    // ── Audio helpers ───────────────────────────────────────────────────────

    /** (offset, length) of the PCM data inside the WAV bytes. */
    private fun findPcm(wav: ByteArray): Pair<Int, Int> {
        if (wav.size < WAV_HEADER) throw GroqException("Filtered audio is empty")
        val buf = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12                                    // after "RIFF" size "WAVE"
        while (pos + 8 <= wav.size) {
            val id = String(wav, pos, 4, Charsets.US_ASCII)
            val size = buf.getInt(pos + 4)
            if (id == "data") {
                val length = minOf(size.toLong().takeIf { it > 0 } ?: Long.MAX_VALUE, (wav.size - pos - 8).toLong())
                return (pos + 8) to (length.toInt() and 1.inv())
            }
            pos += 8 + size + (size and 1)
        }
        return WAV_HEADER to ((wav.size - WAV_HEADER) and 1.inv())
    }

    /** Pieces of at most MAX_PIECE_SEC, cut at the start of a speaker turn when possible. */
    private fun planPieces(pcmBytes: Int, bytesPerSec: Int, segments: List<FilteredSegment>): List<Pair<Int, Int>> {
        val maxBytes = GroqConfig.MAX_PIECE_SEC * bytesPerSec
        if (pcmBytes <= maxBytes) return listOf(0 to pcmBytes)
        val turnStarts = segments.map { (it.filteredStartMs * bytesPerSec / 1000).toInt() and 1.inv() }.sorted()
        val pieces = mutableListOf<Pair<Int, Int>>()
        var start = 0
        while (start < pcmBytes) {
            val limit = start + maxBytes
            if (limit >= pcmBytes) {
                pieces += start to (pcmBytes - start); break
            }
            // last turn start inside (start + 1 minute, limit] — otherwise a hard cut at the limit
            val cut = turnStarts.lastOrNull { it > start + 60 * bytesPerSec && it <= limit } ?: limit
            pieces += start to (cut - start)
            start = cut
        }
        return pieces
    }

    private fun wavHeader(dataBytes: Int, sampleRate: Int): ByteArray =
        ByteBuffer.allocate(WAV_HEADER).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII)); putInt(36 + dataBytes)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII)); putInt(16)
            putShort(1); putShort(1)                        // PCM, mono
            putInt(sampleRate); putInt(sampleRate * 2)
            putShort(2); putShort(16)
            put("data".toByteArray(Charsets.US_ASCII)); putInt(dataBytes)
        }.array()

    // ── Whisper request (multipart/form-data) ───────────────────────────────

    /** Returns (text, number of unsure parts removed). */
    private suspend fun whisper(audio: ByteArray, offset: Int, length: Int, sampleRate: Int): Pair<String, Int> {
        val direct = GroqConfig.METHOD == GroqConfig.Method.DIRECT
        val fields = buildMap {
            put("model", GroqConfig.WHISPER_MODEL)
            put("response_format", "verbose_json")
            put("temperature", "0")
            if (!direct) put("language", GroqConfig.WHISPER_LANGUAGE)
        }
        val head = buildString {
            fields.forEach { (k, v) ->
                append("--$BOUNDARY\r\nContent-Disposition: form-data; name=\"$k\"\r\n\r\n$v\r\n")
            }
            append("--$BOUNDARY\r\nContent-Disposition: form-data; name=\"file\"; filename=\"conversation.wav\"\r\n")
            append("Content-Type: audio/wav\r\n\r\n")
        }.toByteArray(Charsets.UTF_8)
        val header = wavHeader(length, sampleRate)
        val tail = "\r\n--$BOUNDARY--\r\n".toByteArray(Charsets.UTF_8)
        val total = head.size.toLong() + header.size + length + tail.size

        val json = JSONObject(
            withRetry(if (direct) GroqConfig.TRANSLATE_URL else GroqConfig.TRANSCRIBE_URL,
                "multipart/form-data; boundary=$BOUNDARY", total
            ) { out ->
                out.write(head); out.write(header); out.write(audio, offset, length); out.write(tail)
            }
        )

        val segments = json.optJSONArray("segments")
            ?: return json.optString("text").trim() to 0
        val kept = mutableListOf<String>()
        var dropped = 0
        for (i in 0 until segments.length()) {
            val s = segments.getJSONObject(i)
            val unsure = s.optDouble("no_speech_prob", 0.0) > GroqConfig.DROP_IF_NO_SPEECH_PROB &&
                s.optDouble("avg_logprob", 0.0) < GroqConfig.DROP_IF_AVG_LOGPROB
            val looping = s.optDouble("compression_ratio", 0.0) > GroqConfig.DROP_IF_COMPRESSION_RATIO
            if (unsure || looping) {
                dropped++
                Log.i(TAG, "Removed unsure part ${s.optDouble("start")}–${s.optDouble("end")} s: ${s.optString("text")}")
            } else {
                kept += s.optString("text").trim()
            }
        }
        return kept.filter { it.isNotEmpty() }.joinToString(" ") to dropped
    }

    // ── Chat request (translation / summary) ────────────────────────────────

    private suspend fun chat(prompt: String, maxTokens: Int, reasoning: String = "low"): String {
        val body = JSONObject()
            .put("model", GroqConfig.TEXT_MODEL)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            .put("temperature", GroqConfig.TEMPERATURE)
            .put("reasoning_effort", reasoning)      // gpt-oss: "low" = less hidden thinking = faster, fewer tokens
            .put("max_completion_tokens", maxTokens)
            .toString().toByteArray(Charsets.UTF_8)

        val json = JSONObject(
            withRetry(GroqConfig.CHAT_URL, "application/json; charset=utf-8", body.size.toLong()) { it.write(body) }
        )
        val choice = json.optJSONArray("choices")?.optJSONObject(0)
            ?: throw GroqException("Groq returned no answer")
        val text = choice.optJSONObject("message")?.optString("content")?.trim().orEmpty()
        if (text.isEmpty()) {
            throw GroqException("Groq returned an empty answer (${choice.optString("finish_reason", "unknown")})")
        }
        return text
    }

    /** Pieces of about [size] words, cut at the end of a sentence where possible. */
    private fun splitWords(text: String, size: Int): List<String> {
        val sentences = text.split(Regex("(?<=[۔.!?؟])\\s+"))
        val pieces = mutableListOf<String>()
        val current = StringBuilder()
        var words = 0
        for (sentence in sentences) {
            val n = GroqConfig.wordCount(sentence)
            if (words > 0 && words + n > size) {
                pieces += current.toString().trim(); current.clear(); words = 0
            }
            current.append(sentence).append(' ')
            words += n
        }
        if (current.isNotBlank()) pieces += current.toString().trim()
        return pieces
    }

    // ── HTTP with retries ───────────────────────────────────────────────────

    private class GroqBusyException(message: String, val waitMs: Long?) : IOException(message)

    private suspend fun withRetry(
        url: String, contentType: String, length: Long, writeBody: (OutputStream) -> Unit
    ): String {
        var attempt = 1
        while (true) {
            try {
                return post(url, contentType, length, writeBody)
            } catch (e: GroqBusyException) {
                if (attempt >= GroqConfig.MAX_RETRIES) {
                    throw IOException("Groq busy/limited after $attempt attempts (${e.message})")
                }
                val wait = e.waitMs ?: (10_000L * attempt)
                Log.w(TAG, "Groq busy (${e.message}), retry $attempt in ${wait / 1000} s")
                delay(wait)
                attempt++
            } catch (e: CancellationException) {
                throw e
            }
        }
    }

    private fun post(url: String, contentType: String, length: Long, writeBody: (OutputStream) -> Unit): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 180_000
            doOutput = true
            setFixedLengthStreamingMode(length)          // streams the audio, no extra copy in memory
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("Authorization", "Bearer ${GroqConfig.API_KEY}")
        }
        try {
            connection.outputStream.use { writeBody(it) }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            when (code) {
                in 200..299 -> return text
                429, 500, 502, 503, 504 -> {
                    val retryAfter = connection.getHeaderField("retry-after")?.toDoubleOrNull()
                    throw GroqBusyException("HTTP $code: ${errorMessage(text)}", retryAfter?.let { (it * 1000).toLong() + 500 })
                }
                401 -> throw GroqException("HTTP 401: Groq API key is not valid (check GROQ_API_KEY in local.properties)")
                413 -> throw GroqException("HTTP 413: audio piece too large for Groq (lower MAX_PIECE_SEC)")
                else -> throw GroqException("HTTP $code: ${errorMessage(text)}")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(text: String): String = try {
        JSONObject(text).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() } ?: text.take(300)
    } catch (e: Exception) {
        text.take(300)
    }

    private fun requireKey() {
        val key = GroqConfig.API_KEY
        if (key.isBlank() || key == "null") {
            throw GroqException("GROQ_API_KEY is missing in local.properties")
        }
    }
}
