package com.example.evoke.nllb

import android.content.Context
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

/**
 * Memory-safe, staged NLLB translator.
 *
 * Only ONE ONNX model is loaded at any time:
 *   Stage 1: encoder          -> run for every sentence chunk, then close
 *   Stage 2: first decoder    -> run for every sentence chunk, then close
 *   Stage 3: cached decoder   -> generate for every sentence chunk, then close
 *
 * Per-chunk caches are small (sentence-sized), so keeping them between
 * stages is cheap compared with keeping a model loaded.
 */
class
NLLBTranslator(
    private val context: Context
) {

    @Volatile
    private var closed = false

    private val ortEnvironment = OrtEnvironment.getEnvironment()

    private val ortSessionOptions = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(1)
        setInterOpNumThreads(1)
        setOptimizationLevel(
            OrtSession.SessionOptions.OptLevel.ALL_OPT
        )
    }

    private val modelDir: File

    init {
        modelDir = File(
            context.getExternalFilesDir(null),
            "models/nllb/int8"
        )

        Log.d("NLLB_PATH", "================================")
        Log.d("NLLB_PATH", "externalFilesDir = ${context.getExternalFilesDir(null)}")
        Log.d("NLLB_PATH", "modelDir = ${modelDir.absolutePath}")
        Log.d("NLLB_PATH", "modelDir canonical = ${modelDir.canonicalPath}")
        Log.d("NLLB_PATH", "modelDir exists = ${modelDir.exists()}")
        Log.d("NLLB_PATH", "modelDir isDirectory = ${modelDir.isDirectory}")
        Log.d("NLLB_PATH", "modelDir canRead = ${modelDir.canRead()}")
        Log.d("NLLB_PATH", "================================")

        require(modelDir.isDirectory) {
            "NLLB model directory does not exist: ${modelDir.absolutePath}"
        }

        require(File(modelDir, "encoder_model.onnx").exists()) {
            "Encoder model not found"
        }

        require(File(modelDir, "decoder_model_embedded.onnx").exists()) {
            "Embedded Decoder model not found"
        }

        require(File(modelDir, "decoder_with_past_model_embedded.onnx").exists()) {
            "Embedded cached decoder model not found"
        }
    }

    // =============================================================
    // HELPERS
    // =============================================================

    private class ChunkState(
        val paragraphIndex: Int,
        val lineIndex: Int,
        val inputIds: LongArray
    ) {
        var hiddenStates: Array<Array<FloatArray>>? = null
        val decoderCache = arrayOfNulls<OnnxTensor>(24)
        val encoderCache = arrayOfNulls<OnnxTensor>(24)
        var translation: String = ""

        fun closeCaches() {
            for (i in decoderCache.indices) {
                try {
                    decoderCache[i]?.close()
                } catch (_: Exception) {
                }
                decoderCache[i] = null
            }

            for (i in encoderCache.indices) {
                try {
                    encoderCache[i]?.close()
                } catch (_: Exception) {
                }
                encoderCache[i] = null
            }
        }
    }
    private fun float4dToTensor(value: Any?): OnnxTensor {
        @Suppress("UNCHECKED_CAST")
        val data = value as Array<Array<Array<FloatArray>>>

        return OnnxTensor.createTensor(ortEnvironment, data)
    }

    private fun createSession(fileName: String): OrtSession {
        return ortEnvironment.createSession(
            File(modelDir, fileName).absolutePath,
            ortSessionOptions
        )
    }

    // =============================================================
    // MAIN TEST ENTRY
    // =============================================================
    @Synchronized
    fun translate(englishText: String): String {
        check(!closed) {
            "NLLBTranslator has already been closed"
        }

        require(englishText.isNotBlank()) {
            "English text must not be empty."
        }
        val tokenizer = HFTokenizer()
        val chunks = ArrayList<ChunkState>()

        try {

            val tokenizerJson = context.assets
                .open("nllb/tokenizer.json")
                .bufferedReader()
                .use { it.readText() }

            tokenizer.load(tokenizerJson)


// Cache repeated token-count calculations during chunking.
            val tokenCountCache = HashMap<String, Int>()

            val countTokens: (String) -> Int = { candidate ->
                tokenCountCache.getOrPut(candidate) {
                    tokenizer.encode(candidate).size
                }
            }

// Conservative initial limits. Verify these against the encoder model's
// actual input dimensions before treating them as final.
            val targetTokens = 400
            val maxTokens = 512

            val rawChunks = TextChunker.chunk(
                text = englishText,
                countTokens = countTokens,
                targetTokens = targetTokens,
                maxTokens = maxTokens,
                maxSentences = 5,
                onOverflow = TextChunker.OnOverflowAction.THROW_EXCEPTION
            )

            for (rawChunk in rawChunks) {
                val ids = tokenizer.encode(rawChunk.text)
                val idsLong = LongArray(ids.size) { ids[it].toLong() }

                // Verify the actual encoded input as well.
                check(idsLong.size <= maxTokens) {
                    "Encoded chunk exceeds limit: ${idsLong.size} > $maxTokens"
                }

                chunks.add(
                    ChunkState(
                        paragraphIndex = rawChunk.paragraph,
                        lineIndex = rawChunk.line,
                        inputIds = idsLong
                    )
                )
            }

            // ---------------- STAGE 1 ----------------
            runEncoderStage(chunks)

            // ---------------- STAGE 2 ----------------
            runFirstDecoderStage(chunks)

            // ---------------- STAGE 3 ----------------
            runCachedDecoderStage(chunks, tokenizer)


            check(chunks.isNotEmpty()) {
                "No chunks were created from the input."
            }

            val untranslatedChunks = chunks.withIndex()
                .filter { (_, chunk) -> chunk.translation.isBlank() }

            check(untranslatedChunks.isEmpty()) {
                "Some chunks have empty translations: " +
                        untranslatedChunks.joinToString { (index, _) -> (index + 1).toString() }
            }

            val finalTranslation = chunks
                .groupBy { it.paragraphIndex }
                .toSortedMap()
                .values
                .joinToString("\n\n") { paragraphChunks ->

                    paragraphChunks
                        .groupBy { it.lineIndex }
                        .toSortedMap()
                        .values
                        .joinToString("\n") { lineChunks ->
                            lineChunks.joinToString(" ") { it.translation }
                        }
                }
                .trim()
            Log.d("NLLB_CHECK", "Expected chunks: ${chunks.size}")
            Log.d(
                "NLLB_CHECK",
                "Non-empty translations: ${chunks.count { it.translation.isNotBlank() }}"
            )
            Log.d("NLLB_CHECK", "Final translation characters: ${finalTranslation.length}")
            Log.d(
                "NLLB_CHECK",
                "Translation start: ${finalTranslation.take(200)}"
            )
            Log.d(
                "NLLB_CHECK",
                "Translation end: ${finalTranslation.takeLast(200)}"
            )

            check(finalTranslation.isNotBlank()) {
                "Final translation is empty."
            }


            return finalTranslation
        } finally {
            // Release any cache tensors still held if a stage failed midway.
            chunks.forEach { it.closeCaches() }

            try {
                tokenizer.close()
            } catch (e: Exception) {
                Log.w("NLLB", "Tokenizer cleanup failed", e)
            }
        }
    }

    // =============================================================
    // STAGE 1: ENCODER
    // =============================================================

    private fun runEncoderStage(chunks: List<ChunkState>) {
        val session = createSession("encoder_model.onnx")

        try {
            for ((index, chunk) in chunks.withIndex()) {
                var idsTensor: OnnxTensor? = null
                var maskTensor: OnnxTensor? = null
                var result: OrtSession.Result? = null

                try {
                    idsTensor = OnnxTensor.createTensor(
                        ortEnvironment,
                        arrayOf(chunk.inputIds)
                    )

                    maskTensor = OnnxTensor.createTensor(
                        ortEnvironment,
                        arrayOf(LongArray(chunk.inputIds.size) { 1L })
                    )

                    result = session.run(
                        mapOf(
                            "input_ids" to idsTensor,
                            "attention_mask" to maskTensor
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    chunk.hiddenStates =
                        result[0].value as Array<Array<FloatArray>>

                } finally {
                    result?.close()
                    maskTensor?.close()
                    idsTensor?.close()
                }
            }
        } finally {
            session.close()
        }
    }

    // =============================================================
    // STAGE 2: FIRST DECODER (builds the initial caches)
    // =============================================================

    private fun runFirstDecoderStage(chunks: List<ChunkState>) {
        val session = createSession("decoder_model_embedded.onnx")

        try {
            for ((index, chunk) in chunks.withIndex()) {
                var maskTensor: OnnxTensor? = null
                var hiddenTensor: OnnxTensor? = null
                var decoderInputTensor: OnnxTensor? = null
                var result: OrtSession.Result? = null

                try {
                    maskTensor = OnnxTensor.createTensor(
                        ortEnvironment,
                        arrayOf(LongArray(chunk.inputIds.size) { 1L })
                    )

                    hiddenTensor = OnnxTensor.createTensor(
                        ortEnvironment,
                        chunk.hiddenStates
                            ?: error("Missing encoder hidden states for chunk $index")
                    )

                    decoderInputTensor = OnnxTensor.createTensor(
                        ortEnvironment,
                        arrayOf(longArrayOf(2L))
                    )

                    result = session.run(
                        mapOf(
                            "encoder_attention_mask" to maskTensor,
                            "input_ids" to decoderInputTensor,
                            "encoder_hidden_states" to hiddenTensor
                        )
                    )

                    for (layer in 0 until 12) {
                        val base = 1 + layer * 4

                        chunk.decoderCache[layer * 2] =
                            float4dToTensor(result[base].value)
                        chunk.decoderCache[layer * 2 + 1] =
                            float4dToTensor(result[base + 1].value)
                        chunk.encoderCache[layer * 2] =
                            float4dToTensor(result[base + 2].value)
                        chunk.encoderCache[layer * 2 + 1] =
                            float4dToTensor(result[base + 3].value)
                    }

                    // Hidden states are no longer needed.
                    chunk.hiddenStates = null

                } finally {
                    result?.close()
                    decoderInputTensor?.close()
                    hiddenTensor?.close()
                    maskTensor?.close()
                }
            }
        } finally {
            session.close()
        }
    }

    // =============================================================
    // STAGE 3: CACHED DECODER (token generation)
    // =============================================================

    private fun runCachedDecoderStage(
        chunks: List<ChunkState>,
        tokenizer: HFTokenizer
    ) {
        val session = createSession(
            "decoder_with_past_model_embedded.onnx")

        try {
            for ((index, chunk) in chunks.withIndex()) {
                var maskTensor: OnnxTensor? = null

                try {
                    maskTensor = OnnxTensor.createTensor(
                        ortEnvironment,
                        arrayOf(LongArray(chunk.inputIds.size) { 1L })
                    )

                    val rawTranslation = generate(
                        session = session,
                        attentionMaskTensor = maskTensor,
                        decoderCache = chunk.decoderCache,
                        encoderCache = chunk.encoderCache,
                        urduLanguageToken = 256190,
                        inputTokenCount = chunk.inputIds.size,
                        tokenizer = tokenizer
                    )

                    chunk.translation = UrduPostProcessor.clean(rawTranslation)

                } finally {
                    maskTensor?.close()
                    // generate() releases the caches, this is a safety net.
                    chunk.closeCaches()
                }
            }
        } finally {
            session.close()
        }
    }

    private fun generate(
        session: OrtSession,
        attentionMaskTensor: OnnxTensor,
        decoderCache: Array<OnnxTensor?>,
        encoderCache: Array<OnnxTensor?>,
        urduLanguageToken: Int,
        inputTokenCount: Int,
        tokenizer: HFTokenizer
    ): String {

        // The current cache output tensors are owned by this result.
        // Keep the result open while its tensors are used by the next run.
        var activeCacheResult: OrtSession.Result? = null

        // False means decoderCache still contains standalone tensors.
        var decoderCacheOwnedByResult = false

        val generatedTokens = mutableListOf<Int>()

        var currentToken = urduLanguageToken
        var stopReason = "Safety limit reached before EOS"

        try {
            // Per-chunk safety ceiling (chunks are single sentences).
            val maxNewTokens = minOf(256, inputTokenCount * 3 + 10)

// Build the input map once per chunk and reuse it for each token.
            val cachedInputs = HashMap<String, OnnxTensor>(50).apply {
                put("encoder_attention_mask", attentionMaskTensor)

                for (layer in 0 until 12) {
                    put(
                        "past_key_values.$layer.decoder.key",
                        decoderCache[layer * 2]
                            ?: error("Missing decoder key cache for layer $layer")
                    )
                    put(
                        "past_key_values.$layer.decoder.value",
                        decoderCache[layer * 2 + 1]
                            ?: error("Missing decoder value cache for layer $layer")
                    )
                    put(
                        "past_key_values.$layer.encoder.key",
                        encoderCache[layer * 2]
                            ?: error("Missing encoder key cache for layer $layer")
                    )
                    put(
                        "past_key_values.$layer.encoder.value",
                        encoderCache[layer * 2 + 1]
                            ?: error("Missing encoder value cache for layer $layer")
                    )
                }
            }

            var step = 0

            while (step < maxNewTokens) {
                val targetTokenTensor = OnnxTensor.createTensor(
                    ortEnvironment,
                    arrayOf(longArrayOf(currentToken.toLong()))
                )

                var cachedDecoderResult: OrtSession.Result? = null

                try {
                    cachedInputs["input_ids"] = targetTokenTensor

                    cachedDecoderResult = session.run(cachedInputs)


                    @Suppress("UNCHECKED_CAST")
                    val cachedLogits =
                        cachedDecoderResult[0].value as Array<Array<FloatArray>>

                    val logits = cachedLogits[0][0]


                    var nextToken = 0
                    var bestLogit = Float.NEGATIVE_INFINITY

                    for (i in logits.indices) {
                        if (logits[i] > bestLogit) {
                            bestLogit = logits[i]
                            nextToken = i
                        }
                    }

                    // EOS is checked before adding it to the output sequence.
                    if (nextToken == 2) {
                        stopReason = "EOS token generated"
                        break
                    }

                    generatedTokens.add(nextToken)

                    // Reuse cache output tensors directly.
                    activeCacheResult?.close()
                    activeCacheResult = null

                    if (!decoderCacheOwnedByResult) {
                        for (i in decoderCache.indices) {
                            decoderCache[i]?.close()
                            decoderCache[i] = null
                        }
                    }

                    for (i in 0 until 24) {
                        decoderCache[i] = cachedDecoderResult[i + 1] as OnnxTensor
                    }
                    for (layer in 0 until 12) {
                        cachedInputs["past_key_values.$layer.decoder.key"] =
                            decoderCache[layer * 2]!!

                        cachedInputs["past_key_values.$layer.decoder.value"] =
                            decoderCache[layer * 2 + 1]!!
                    }
                    // Transfer result ownership; null the local reference so
                    // the inner finally will not close the transferred result.
                    activeCacheResult = cachedDecoderResult
                    cachedDecoderResult = null

                    decoderCacheOwnedByResult = true

                    currentToken = nextToken
                    step++
                } finally {
                    // Do not close activeCacheResult here; the next iteration
                    // still needs its tensors.
                    cachedDecoderResult?.close()
                    targetTokenTensor.close()
                }
            }

            check(stopReason == "EOS token generated") {
                "Translation did not finish naturally. " +
                        "Input tokens: $inputTokenCount; " +
                        "generated tokens: ${generatedTokens.size}; " +
                        "reason: $stopReason"
            }

            val translatedText = tokenizer
                .decode(generatedTokens.toIntArray())
                .trim()

            return translatedText
        } finally {
            // The result owns the current decoder cache output tensors.
            activeCacheResult?.close()
            activeCacheResult = null

            if (decoderCacheOwnedByResult) {
                // Result-owned tensors were closed with the result.
                for (i in decoderCache.indices) {
                    decoderCache[i] = null
                }
            } else {
                for (i in decoderCache.indices) {
                    decoderCache[i]?.close()
                    decoderCache[i] = null
                }
            }

            // Encoder cache tensors are standalone tensors.
            for (i in encoderCache.indices) {
                encoderCache[i]?.close()
                encoderCache[i] = null
            }
        }
    }

    @Synchronized
    fun close() {
        if (closed) return
        closed = true

        try {
            ortSessionOptions.close()
        } catch (e: Exception) {
            Log.w("NLLB", "Session options cleanup failed", e)
        }
    }
}
