package com.example.evoke.mobilefacenet

import android.util.Log

// DEBUG ONLY – remove before release (embeddings are biometric data)
fun logFullEmbedding(result: FaceRecognitionResult) {
    val values = result.embedding
    Log.d("FaceEmbedding", "===== START size=${values.size} source=${result.sourceImageUri} =====")
    val chunkSize = 20
    values.toList().chunked(chunkSize).forEachIndexed { i, chunk ->
        val from = i * chunkSize
        val to = from + chunk.size - 1
        Log.d("FaceEmbedding", "[$from..$to] " + chunk.joinToString(", "))
    }
    Log.d("FaceEmbedding", "===== END =====")
}