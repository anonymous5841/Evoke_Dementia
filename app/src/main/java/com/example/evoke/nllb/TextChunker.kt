package com.example.evoke.nllb

internal object TextChunker {

    data class RawChunk(val paragraph: Int, val line: Int, val text: String)

    enum class OnOverflowAction {
        THROW_EXCEPTION,
        FORCE_SLICE_CHARS
    }

    private val PARAGRAPH_SPLIT = Regex("\\n\\s*\\n")
    private val SENTENCE_END = Regex("[.!?][\"”')]?")

    // Splits after clause punctuation while retaining the punctuation with the preceding clause token
    private val CLAUSE_SPLIT = Regex("(?<=[;:,])\\s+")

    private val ABBREVIATIONS = setOf(
        "e.g.", "i.e.", "vs.", "dr.", "mr.", "mrs.", "ms.",
        "fig.", "no.", "approx.", "inc.", "st.", "cf."
    )

    fun chunk(
        text: String,
        countTokens: (String) -> Int,
        targetTokens: Int,
        maxTokens: Int,
        maxSentences: Int,
        onOverflow: OnOverflowAction = OnOverflowAction.THROW_EXCEPTION
    ): List<RawChunk> {
        require(targetTokens > 0) { "targetTokens must be > 0" }
        require(maxTokens >= targetTokens) { "maxTokens must be >= targetTokens" }
        require(maxSentences > 0) { "maxSentences must be > 0" }

        val out = ArrayList<RawChunk>()
        val paragraphs = text.split(PARAGRAPH_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }

        for ((p, paragraph) in paragraphs.withIndex()) {
            val lines = paragraph.split('\n').map { it.trim() }.filter { it.isNotEmpty() }


            for ((l, line) in lines.withIndex()) {
                val sentences = splitSentences(line)
                for (chunkText in pack(sentences, countTokens, targetTokens, maxTokens, maxSentences, onOverflow)) {
                    out.add(RawChunk(p, l, chunkText))
                }
            }
        }

        // Final Validation Guard: Rejects any chunk exceeding maxTokens
        for (chunk in out) {
            val tokenCount = countTokens(chunk.text)
            if (tokenCount > maxTokens) {
                throw IllegalStateException(
                    "Chunk validation failed! Final chunk exceeds maxTokens ($tokenCount > $maxTokens): '${chunk.text}'"
                )
            }
        }


        val reconstructed = out.joinToString(" ") { it.text }

        check(
            normalizeWhitespace(reconstructed) ==
                    normalizeWhitespace(text)
        ) {
            "Chunking changed or lost source text. " +
                    "Original characters=${text.length}, " +
                    "reconstructed characters=${reconstructed.length}"
        }

        return out
    }

    private fun splitSentences(line: String): List<String> {
        val tokens = line.split(Regex("\\s+"))
        val sentences = ArrayList<String>()
        val current = StringBuilder()

        for (token in tokens) {
            if (current.isNotEmpty()) current.append(" ")
            current.append(token)

            val cleanToken = token.lowercase().trimEnd(',', ';', ':', '"', '\'')
            if (SENTENCE_END.containsMatchIn(token) && cleanToken !in ABBREVIATIONS) {
                sentences.add(current.toString())
                current.setLength(0)
            }
        }
        if (current.isNotEmpty()) {
            sentences.add(current.toString())
        }
        return sentences
    }

    private fun pack(
        sentences: List<String>,
        countTokens: (String) -> Int,
        target: Int,
        maxTokens: Int,
        maxSentences: Int,
        onOverflow: OnOverflowAction
    ): List<String> {
        val chunks = ArrayList<String>()
        val current = StringBuilder()
        var currentTokens = 0
        var currentCount = 0

        fun flush() {
            if (current.isNotEmpty()) {
                chunks.add(current.toString())
                current.setLength(0)
                currentTokens = 0
                currentCount = 0
            }
        }

        for (sentence in sentences) {
            val tokens = countTokens(sentence)

            if (tokens > maxTokens) {
                flush()
                chunks.addAll(splitLongSentence(sentence, countTokens, target, maxTokens, onOverflow))
                continue
            }

            if (currentCount >= maxSentences || (currentCount > 0 && currentTokens + tokens > target)) {
                flush()
            }

            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
            currentTokens += tokens
            currentCount++
        }
        flush()
        return chunks
    }

    private fun splitLongSentence(
        sentence: String,
        countTokens: (String) -> Int,
        target: Int,
        maxTokens: Int,
        onOverflow: OnOverflowAction
    ): List<String> {
        val pieces = ArrayList<String>()
        val clauses = sentence.split(CLAUSE_SPLIT).filter { it.isNotEmpty() }

        val current = StringBuilder()
        var currentTokens = 0

        fun flush() {
            if (current.isNotEmpty()) {
                pieces.add(current.toString())
                current.setLength(0)
                currentTokens = 0
            }
        }

        for (clause in clauses) {
            val tokens = countTokens(clause)

            if (tokens > maxTokens) {
                flush()
                val words = clause.split(Regex("\\s+")).filter { it.isNotEmpty() }
                val tempPiece = StringBuilder()

                for (word in words) {
                    val wordTokens = countTokens(word)

                    if (wordTokens > maxTokens) {
                        if (tempPiece.isNotEmpty()) {
                            pieces.add(tempPiece.toString())
                            tempPiece.setLength(0)
                        }
                        pieces.addAll(splitOverlongWord(word, countTokens, maxTokens, onOverflow))
                        continue
                    }

                    val test = if (tempPiece.isEmpty()) word else "$tempPiece $word"
                    if (countTokens(test) > maxTokens && tempPiece.isNotEmpty()) {
                        pieces.add(tempPiece.toString())
                        tempPiece.setLength(0)
                        tempPiece.append(word)
                    } else {
                        if (tempPiece.isNotEmpty()) tempPiece.append(" ")
                        tempPiece.append(word)
                    }
                }
                if (tempPiece.isNotEmpty()) pieces.add(tempPiece.toString())
                continue
            }

            if (currentTokens > 0 && currentTokens + tokens > target) flush()
            if (current.isNotEmpty()) current.append(' ')
            current.append(clause)
            currentTokens += tokens
        }
        flush()
        return pieces
    }

    private fun splitOverlongWord(
        word: String,
        countTokens: (String) -> Int,
        maxTokens: Int,
        onOverflow: OnOverflowAction
    ): List<String> {
        val subPieces = ArrayList<String>()
        var start = 0

        while (start < word.length) {
            var end = start + 1
            var lastValidEnd = start

            while (end <= word.length) {
                val sub = word.substring(start, end)
                if (countTokens(sub) <= maxTokens) {
                    lastValidEnd = end
                    end++
                } else {
                    break
                }
            }

            if (lastValidEnd == start) {
                if (onOverflow == OnOverflowAction.THROW_EXCEPTION) {
                    throw IllegalStateException("Single character '${word[start]}' exceeds maxTokens limit ($maxTokens)")
                } else {
                    // Force slice at least 1 character to avoid infinite loop
                    lastValidEnd = start + 1
                }
            }

            subPieces.add(word.substring(start, lastValidEnd))
            start = lastValidEnd
        }

        return subPieces
    }

    private fun normalizeWhitespace(text: String): String =
        text.trim().replace(Regex("\\s+"), " ")
}