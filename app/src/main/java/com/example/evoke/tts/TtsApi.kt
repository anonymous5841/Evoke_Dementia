package com.example.evoke.tts

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.BreakIterator
import java.util.Locale

enum class TtsLanguage {
    ENGLISH,
    URDU
}

data class TtsAudio(
    val samples: FloatArray,
    val sampleRate: Int,
    val playbackSpeed: Float,
    val sentenceCount: Int
)

class TtsApi(context: Context) {

    companion object {
        private const val TAG = "TtsApi"

        private const val ENGLISH_FOLDER =
            "vits-piper-en_US-lessac-medium"
        private const val ENGLISH_MODEL =
            "en_US-lessac-medium.onnx"

        private const val URDU_FOLDER =
            "vits-piper-ur_PK-fasih-medium"
        private const val URDU_MODEL =
            "ur_PK-fasih-medium.onnx"

        private const val ENGLISH_ASSET_PATH =
            "piper/vits-piper-en_US-lessac-medium"
        private const val URDU_ASSET_PATH =
            "piper/vits-piper-ur_PK-fasih-medium"

        private const val ENGLISH_PAUSE_SECONDS = 0.35f
        private const val URDU_PAUSE_SECONDS = 0.2f

        private const val ENGLISH_PLAYBACK_SPEED = 0.93f
        private const val URDU_PLAYBACK_SPEED = 1.0f
    }

    private val appContext = context.applicationContext

    private val engines = mutableMapOf<TtsLanguage, OfflineTts>()
    private val generationMutex = Mutex()

    /**
     * Generates paragraph audio without playing it.
     *
     * Call from a coroutine. The returned samples are mono floating-point
     * audio; use the returned sampleRate and playbackSpeed for playback.
     */
    suspend fun generateSpeech(
        text: String,
        language: TtsLanguage
    ): TtsAudio = withContext(Dispatchers.IO) {
        require(text.isNotBlank()) {
            "Input text must not be empty."
        }

        generationMutex.withLock {
            val engine = getOrCreateEngine(language)
            val sentences = splitIntoSentences(text, language)

            require(sentences.isNotEmpty()) {
                "No readable sentences were found in the input."
            }

            val combined = ArrayList<Float>()
            var outputSampleRate = 0

            sentences.forEachIndexed { index, sentence ->
                Log.d(TAG, "${language.name} sentence ${index + 1}: $sentence")

                val generated = engine.generate(sentence, 0)
                val samples = generated.samples
                val rate = generated.sampleRate

                require(samples.isNotEmpty()) {
                    "The TTS model returned empty audio."
                }

                if (outputSampleRate == 0) {
                    outputSampleRate = rate
                } else {
                    check(outputSampleRate == rate) {
                        "Sample rate changed while generating audio."
                    }
                }

                for (sample in samples) {
                    combined.add(sample)
                }

                if (index < sentences.lastIndex) {
                    val pauseSeconds = when (language) {
                        TtsLanguage.ENGLISH -> ENGLISH_PAUSE_SECONDS
                        TtsLanguage.URDU -> URDU_PAUSE_SECONDS
                    }

                    val silenceCount = (rate * pauseSeconds).toInt()
                    repeat(silenceCount) {
                        combined.add(0f)
                    }
                }
            }

            TtsAudio(
                samples = combined.toFloatArray(),
                sampleRate = outputSampleRate,
                playbackSpeed = when (language) {
                    TtsLanguage.ENGLISH -> ENGLISH_PLAYBACK_SPEED
                    TtsLanguage.URDU -> URDU_PLAYBACK_SPEED
                },
                sentenceCount = sentences.size
            )
        }
    }

    private fun getOrCreateEngine(language: TtsLanguage): OfflineTts {
        engines[language]?.let { return it }

        val folder: String
        val modelName: String
        val assetPath: String

        when (language) {
            TtsLanguage.ENGLISH -> {
                folder = ENGLISH_FOLDER
                modelName = ENGLISH_MODEL
                assetPath = ENGLISH_ASSET_PATH
            }

            TtsLanguage.URDU -> {
                folder = URDU_FOLDER
                modelName = URDU_MODEL
                assetPath = URDU_ASSET_PATH
            }
        }

        val modelDir = File(appContext.filesDir, "tts/$folder")
        copyModelIfNeeded(assetPath, modelDir, modelName)

        val config = getOfflineTtsConfig(
            modelDir = modelDir.absolutePath,
            modelName = modelName,
            acousticModelName = "",
            vocoder = "",
            voices = "",
            lexicon = "",
            dataDir = File(modelDir, "espeak-ng-data").absolutePath,
            dictDir = "",
            ruleFsts = "",
            ruleFars = "",
            isKitten = false,
            isSupertonic = false,
            durationPredictor = "",
            textEncoder = "",
            vectorEstimator = "",
            supertonicVocoder = "",
            ttsJson = "",
            unicodeIndexer = "",
            voiceStyle = ""
        )

        return OfflineTts(
            assetManager = null,
            config = config
        ).also {
            engines[language] = it
            Log.d(TAG, "$language model initialized")
        }
    }

    private fun splitIntoSentences(
        text: String,
        language: TtsLanguage
    ): List<String> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return emptyList()

        // Urdu uses Urdu punctuation as well as question and exclamation marks.
        if (language == TtsLanguage.URDU) {
            return cleaned
                .split(Regex("(?<=[۔؟!?])\\s+|\\n+"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        // Locale-aware sentence boundaries for English.
        // BreakIterator is better than splitting at every period, though
        // abbreviations and unusual formatting can still need special handling.
        val iterator = BreakIterator.getSentenceInstance(Locale.US)
        iterator.setText(cleaned)

        val result = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()

        while (end != BreakIterator.DONE) {
            val part = cleaned.substring(start, end).trim()
            if (part.isNotEmpty()) result.add(part)
            start = end
            end = iterator.next()
        }

        // Keep any trailing text if the iterator didn't return it.
        if (start < cleaned.length) {
            cleaned.substring(start).trim()
                .takeIf { it.isNotEmpty() }
                ?.let(result::add)
        }

        return result.ifEmpty { listOf(cleaned) }
    }

    private fun copyModelIfNeeded(
        assetPath: String,
        destination: File,
        modelName: String
    ) {
        val modelFile = File(destination, modelName)

        if (modelFile.exists() && modelFile.length() > 0L) return

        destination.mkdirs()
        copyAssetFolder(assetPath, destination)

        check(modelFile.exists() && modelFile.length() > 0L) {
            "Model file was not copied correctly: ${modelFile.absolutePath}"
        }
    }

    private fun copyAssetFolder(
        assetPath: String,
        destination: File
    ) {
        destination.mkdirs()

        val names = appContext.assets.list(assetPath) ?: emptyArray()

        for (name in names) {
            val source = "$assetPath/$name"
            val output = File(destination, name)
            val children = appContext.assets.list(source)

            if (!children.isNullOrEmpty()) {
                copyAssetFolder(source, output)
            } else {
                appContext.assets.open(source).use { input ->
                    FileOutputStream(output).use { out ->
                        input.copyTo(out)
                    }
                }
            }
        }
    }
}
