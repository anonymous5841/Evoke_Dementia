package com.example.evoke.nllb

import java.text.Normalizer

internal object UrduPostProcessor {

    private const val COMMA = "\u060C"        // ،
    private const val FULL_STOP = "\u06D4"    // ۔
    private const val QUESTION = "\u061F"     // ؟
    private const val SEMICOLON = "\u061B"    // ؛
    private const val URDU_RANGE = "\\u0600-\\u06FF\\u0750-\\u077F"

    // Explicit allowlist of known NLLB model tokens and tags
    private val NLLB_SPECIAL_TOKENS = Regex("</?[sS]>|<pad>|<unk>|<mask_\\d+>|__urd_Arab__|__eng_Latn__")
    private val HAS_URDU_SCRIPT = Regex("[$URDU_RANGE]")

    // Protects guillemets («...»), double quotes ("..."), single quotes ('...'), and diacritic-heavy Arabic
    private val ARABIC_QUOTE_BLOCK = Regex("(«[^»]+»|\"[^\"]*[\u064B-\u0652][^\"]*\"|'[^'*]*[\u064B-\u0652][^'*]*')")

    private val URDU_COMMA_RULE = Regex("(?<=[" + URDU_RANGE + "]),|,(?=[" + URDU_RANGE + "])")
    private val QUESTION_RULE = Regex("\\s*\\?(?=\\s|$)")
    private val URDU_STOP_RULE = Regex("(?<=[" + URDU_RANGE + "])\\.(?=\\s+|$)")

    private val SPACE_BEFORE_PUNCT = Regex("\\s+([${COMMA}${FULL_STOP}${QUESTION}${SEMICOLON}!:])")
    private val SPACE_AFTER_PUNCT = Regex("([${COMMA}${FULL_STOP}${QUESTION}${SEMICOLON}])(?=[A-Za-z$URDU_RANGE])")

    private val SPACE_AFTER_OPEN = Regex("([(\\[])\\s+")
    private val SPACE_BEFORE_CLOSE = Regex("\\s+([)\\]])")
    private val MULTI_SPACE = Regex("[ \\t\\u00A0]{2,}")

    fun clean(raw: String): String {
        var s = Normalizer.normalize(raw, Normalizer.Form.NFC)

        // Protect quoted Arabic blocks before stripping model tokens or altering character forms
        val protectedBlocks = ArrayList<String>()
        s = ARABIC_QUOTE_BLOCK.replace(s) { match ->
            protectedBlocks.add(match.value)
            "__ARABIC_BLOCK_${protectedBlocks.size - 1}__"
        }

        // Strip model special tokens (Will not affect __ARABIC_BLOCK_X__ due to strict allowlist regex)
        s = NLLB_SPECIAL_TOKENS.replace(s, " ")
        s = s.replace('\u2581', ' ')

        // Normalize character forms for non-quoted Urdu text
        s = s.replace('\u064A', '\u06CC') // ي -> ی
        s = s.replace('\u0649', '\u06CC') // ى -> ی
        s = s.replace('\u0643', '\u06A9') // ك -> ک

        // Restore protected Arabic text untouched
        protectedBlocks.forEachIndexed { idx, block ->
            s = s.replace("__ARABIC_BLOCK_${idx}__", block)
        }

        if (HAS_URDU_SCRIPT.containsMatchIn(s)) {
            s = URDU_COMMA_RULE.replace(s, COMMA)
            s = QUESTION_RULE.replace(s, QUESTION)
            s = URDU_STOP_RULE.replace(s, FULL_STOP)
            s = SPACE_BEFORE_PUNCT.replace(s, "$1")
            s = SPACE_AFTER_PUNCT.replace(s, "$1 ")
        }

        s = SPACE_AFTER_OPEN.replace(s, "$1")
        s = SPACE_BEFORE_CLOSE.replace(s, "$1")
        s = MULTI_SPACE.replace(s, " ")

        return s.trim()
    }
}