package com.example.evoke.mobilefacenet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import android.graphics.Color
import kotlin.math.hypot
import kotlin.math.roundToInt
/**
 * Aligns a detected face to the standard 112x112 MTCNN/ArcFace reference
 * template using a similarity transform (rotation + uniform scale +
 * translation — no shear) fit through ML Kit's facial landmarks.
 *
 * WHY THIS MATTERS: cropToFace() gets the face in frame, but does nothing
 * about head tilt or where the eyes sit within that frame. MobileFaceNet
 * was trained on faces where both eyes land on the same fixed pixel
 * positions every time. This step is what actually matches that training
 * distribution — it's the difference between "a photo of a face" and "the
 * kind of input the model was trained on".
 *
 * LEFT/RIGHT NOTE: ML Kit's FaceLandmark.LEFT_EYE means the SUBJECT'S
 * anatomical left eye, which in a normal (non-mirrored) photo appears on
 * the RIGHT side of the image. The standard template's first point
 * ("left eye") is conventionally the point on the LEFT side of the image —
 * i.e. the subject's RIGHT eye. The mapping below is by image side, not by
 * ML Kit's anatomical naming, to match the template correctly. If aligned
 * output ever looks mirrored, this is the first place to check.
 */
object FaceAligner {

    private const val TEMPLATE_SIZE = 112

    /** Returns a 112x112 aligned face bitmap, or null if too few landmarks
     *  were detected (caller should fall back to cropToFace in that case —
     *  e.g. for a strongly profile-angled face). */
// Same 4 points the notebook used (ALIGN_POINTS = 0,1,3,4): eyes + mouth corners, no nose.
    private val T_EYE_L   = PointF(38.2946f, 51.6963f)   // image-left eye
    private val T_EYE_R   = PointF(73.5318f, 51.5014f)
    private val T_MOUTH_L = PointF(41.5493f, 92.3655f)   // image-left mouth corner
    private val T_MOUTH_R = PointF(70.7299f, 92.2041f)

    fun align(source: Bitmap, face: Face): Bitmap? {
        val eyeA   = face.getLandmark(FaceLandmark.LEFT_EYE)?.position    ?: return null
        val eyeB   = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position   ?: return null
        val mouthA = face.getLandmark(FaceLandmark.MOUTH_LEFT)?.position  ?: return null
        val mouthB = face.getLandmark(FaceLandmark.MOUTH_RIGHT)?.position ?: return null

        // Decide left/right by x position, not by ML Kit's naming.
        val (eyeL, eyeR)     = if (eyeA.x <= eyeB.x) eyeA to eyeB else eyeB to eyeA
        val (mouthL, mouthR) = if (mouthA.x <= mouthB.x) mouthA to mouthB else mouthB to mouthA

        val dst = listOf(T_EYE_L, T_EYE_R, T_MOUTH_L, T_MOUTH_R)
        var bitmap = source
        var src = listOf(eyeL, eyeR, mouthL, mouthR)

        var t = solveSimilarityTransform(src, dst) ?: return null

        // Phone photos are huge. If the face must shrink a lot, pre-shrink once so the warp
        // doesn't alias (LFW images were small, so the calibration never saw this).
        val scale = hypot(t.sRe, t.sIm)
        if (scale < 0.5f) {
            val w = (source.width * scale * 2f).roundToInt().coerceAtLeast(1)
            val h = (source.height * scale * 2f).roundToInt().coerceAtLeast(1)
            bitmap = Bitmap.createScaledBitmap(source, w, h, true)
            val fx = w.toFloat() / source.width
            val fy = h.toFloat() / source.height
            src = src.map { PointF(it.x * fx, it.y * fy) }
            t = solveSimilarityTransform(src, dst) ?: return null
        }

        val matrix = Matrix().apply {
            setValues(floatArrayOf(t.sRe, -t.sIm, t.tx, t.sIm, t.sRe, t.ty, 0f, 0f, 1f))
        }
        val aligned = Bitmap.createBitmap(TEMPLATE_SIZE, TEMPLATE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(aligned)
        canvas.drawColor(Color.BLACK)                       // black outside the photo, like the notebook
        canvas.drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG))
        return aligned
    }
    /** sRe/sIm together form a complex scale-rotation factor; tx/ty are
     *  translation. dst = (sRe + i*sIm) * src + (tx + i*ty). */
    private data class Similarity(val sRe: Float, val sIm: Float, val tx: Float, val ty: Float)

    /**
     * Closed-form least-squares fit of a 2D similarity transform (uniform
     * scale + rotation + translation, no shear) mapping [src] onto [dst],
     * via the complex-number formulation: treating each 2D point as a
     * complex number, the optimal scale-rotation `s` and translation `t`
     * minimizing sum(|dst_i - (s*src_i + t)|^2) are:
     *   s = sum((dst_i - meanDst) * conj(src_i - meanSrc)) / sum(|src_i - meanSrc|^2)
     *   t = meanDst - s * meanSrc
     * Returns null for degenerate input (e.g. duplicate points).
     */
    private fun solveSimilarityTransform(src: List<PointF>, dst: List<PointF>): Similarity? {
        val n = src.size
        val meanSrcX = src.sumOf { it.x.toDouble() }.toFloat() / n
        val meanSrcY = src.sumOf { it.y.toDouble() }.toFloat() / n
        val meanDstX = dst.sumOf { it.x.toDouble() }.toFloat() / n
        val meanDstY = dst.sumOf { it.y.toDouble() }.toFloat() / n

        var numRe = 0f
        var numIm = 0f
        var denom = 0f

        for (i in 0 until n) {
            val usX = src[i].x - meanSrcX
            val usY = src[i].y - meanSrcY
            val udX = dst[i].x - meanDstX
            val udY = dst[i].y - meanDstY

            // ud * conj(us) = (udX*usX + udY*usY) + i*(udY*usX - udX*usY)
            numRe += udX * usX + udY * usY
            numIm += udY * usX - udX * usY
            denom += usX * usX + usY * usY
        }

        if (denom < 1e-6f) return null

        val sRe = numRe / denom
        val sIm = numIm / denom
        val tx = meanDstX - (sRe * meanSrcX - sIm * meanSrcY)
        val ty = meanDstY - (sIm * meanSrcX + sRe * meanSrcY)

        return Similarity(sRe, sIm, tx, ty)
    }
}