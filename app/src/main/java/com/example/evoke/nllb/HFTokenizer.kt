package com.example.evoke.nllb

class HFTokenizer {

    companion object {
        init {
            System.loadLibrary("nllb_tokenizer")
        }
    }

    private var nativePtr: Long = 0

    external fun createTokenizer(json: String): Long

    external fun encode(
        tokenizerPtr: Long,
        text: String
    ): IntArray

    external fun decode(
        tokenizerPtr: Long,
        ids: IntArray
    ): String

    external fun deleteTokenizer(
        tokenizerPtr: Long
    )

    fun load(tokenizerJson: String) {
        nativePtr = createTokenizer(tokenizerJson)

        check(nativePtr != 0L) {
            "Failed to create NLLB tokenizer"
        }
    }

    fun encode(text: String): IntArray {
        check(nativePtr != 0L) {
            "Tokenizer has not been loaded"
        }

        return encode(nativePtr, text)
    }

    fun decode(ids: IntArray): String {
        check(nativePtr != 0L) {
            "Tokenizer has not been loaded"
        }

        return decode(nativePtr, ids)
    }

    fun close() {
        if (nativePtr != 0L) {
            deleteTokenizer(nativePtr)
            nativePtr = 0L
        }
    }
}