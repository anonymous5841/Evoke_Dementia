package com.example.evoke.mobilefacenet

import android.graphics.Bitmap

/**
 * Result of running the offline MobileFaceNet embedding extraction (see
 * com.example.myapplication.mobilefacenet.FaceEmbedder) on a captured or gallery-picked
 * photo. Kept separate from FaceEmbedder itself so UI code can depend on a
 * plain data model without pulling in TensorFlow Lite types directly.
 *
 * alignedFace and profilePhoto have defaults so existing calls with two arguments still compile.
 */
data class FaceRecognitionResult(
    val embedding: FloatArray,
    val sourceImageUri: String,
    /** Aligned 112x112 face, needed later to save this person as a new enrolment. */
    val alignedFace: Bitmap? = null,
    /** Square, upright crop of just this person, for the "Register" screen's picture. */
    val profilePhoto: Bitmap? = null
) {
    // FloatArray doesn't get structural equals/hashCode for free from a data
    // class — override so two results with identical content compare equal
    // (useful in tests / state comparisons) instead of comparing by identity.
    // The bitmaps are deliberately not compared.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceRecognitionResult) return false
        return embedding.contentEquals(other.embedding) && sourceImageUri == other.sourceImageUri
    }

    override fun hashCode(): Int {
        var result = embedding.contentHashCode()
        result = 31 * result + sourceImageUri.hashCode()
        return result
    }
}