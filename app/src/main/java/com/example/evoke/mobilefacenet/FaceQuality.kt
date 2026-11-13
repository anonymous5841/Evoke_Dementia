package com.example.evoke.mobilefacenet

import android.graphics.Bitmap
import com.google.mlkit.vision.face.Face
import kotlin.math.min

/**
 * Cheap photo checks that run on the aligned 112x112 face before embedding.
 *
 * ENROLL is strict: a bad reference photo hurts every later match, so ask for a retake.
 * RECOGNIZE is lenient: it only rejects hopeless photos, so ordinary hard photos still get a result.
 *
 * IMPORTANT: the limits below are STARTING VALUES, not validated. Log the Report for real photos
 * (good ones and the ones that fail) and tune them.
 */
object FaceQuality {

    enum class Profile { ENROLL, RECOGNIZE }

    class Report(
        val ok: Boolean,
        val message: String?,
        val faceSizePx: Int,
        val meanLuma: Float,
        val eyeGlareFraction: Float
    )

    private class Limits(
        val minFacePx: Int,
        val minLuma: Float,
        val maxLuma: Float,
        val maxEyeGlare: Float
    )

    private val ENROLL_LIMITS = Limits(minFacePx = 120, minLuma = 60f, maxLuma = 200f, maxEyeGlare = 0.20f)
    private val RECOGNIZE_LIMITS = Limits(minFacePx = 60, minLuma = 35f, maxLuma = 230f, maxEyeGlare = 0.60f)

    // Luminance at or above this counts as blown out.
    private const val GLARE_LUMA = 240f

    // Boxes in the aligned 112x112 image as [x0, y0, x1, y1). The template eyes sit at (38, 52) and (74, 52).
    private val LEFT_EYE_BOX = intArrayOf(26, 44, 50, 62)
    private val RIGHT_EYE_BOX = intArrayOf(62, 44, 86, 62)

    // Central face region used for the brightness average (avoids black padding at photo edges).
    private val FACE_REGION = intArrayOf(24, 30, 88, 100)

    fun check(face: Face, aligned: Bitmap, profile: Profile): Report {
        val limits = if (profile == Profile.ENROLL) ENROLL_LIMITS else RECOGNIZE_LIMITS

        val box = face.boundingBox
        val facePx = min(box.width(), box.height())

        val width = aligned.width
        val pixels = IntArray(width * aligned.height)
        aligned.getPixels(pixels, 0, width, 0, 0, width, aligned.height)

        val meanLuma = meanLuma(pixels, width, FACE_REGION)
        val glare = maxOf(
            glareFraction(pixels, width, LEFT_EYE_BOX),
            glareFraction(pixels, width, RIGHT_EYE_BOX)
        )

        val message = when {
            facePx < limits.minFacePx -> "The face is too small. Move closer and try again."
            meanLuma > limits.maxLuma -> "The photo is too bright. Try softer light."
            meanLuma < limits.minLuma -> "The photo is too dark. Try more light."
            glare > limits.maxEyeGlare ->
                "There is glare over the eyes. Tilt the head slightly or change the lighting."
            else -> null
        }

        return Report(message == null, message, facePx, meanLuma, glare)
    }

    private fun luma(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    private fun meanLuma(pixels: IntArray, width: Int, box: IntArray): Float {
        var sum = 0f
        var count = 0
        for (y in box[1] until box[3]) {
            for (x in box[0] until box[2]) {
                sum += luma(pixels[y * width + x])
                count++
            }
        }
        return if (count == 0) 0f else sum / count
    }

    private fun glareFraction(pixels: IntArray, width: Int, box: IntArray): Float {
        var blown = 0
        var count = 0
        for (y in box[1] until box[3]) {
            for (x in box[0] until box[2]) {
                if (luma(pixels[y * width + x]) >= GLARE_LUMA) blown++
                count++
            }
        }
        return if (count == 0) 0f else blown.toFloat() / count
    }
}
