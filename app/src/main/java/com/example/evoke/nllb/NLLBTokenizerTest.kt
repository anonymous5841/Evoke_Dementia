package com.example.evoke.nllb

import android.content.Context

object NLLBTokenizerTest {

    fun run(context: Context): String {
        val json = context.assets
            .open("nllb/tokenizer.json")
            .bufferedReader()
            .use { it.readText() }

        val tokenizer = HFTokenizer()

        return try {
            tokenizer.load(json)

            val text = "Hello, how are you?"

            val ids = tokenizer.encode(text)
            val decoded = tokenizer.decode(ids)

            buildString {
                appendLine("INPUT:")
                appendLine(text)
                appendLine()
                appendLine("TOKEN COUNT:")
                appendLine(ids.size.toString())
                appendLine()
                appendLine("TOKEN IDS:")
                appendLine(ids.joinToString(", "))
                appendLine()
                appendLine("DECODED:")
                appendLine(decoded)
            }
        } finally {
            tokenizer.close()
        }
    }
}