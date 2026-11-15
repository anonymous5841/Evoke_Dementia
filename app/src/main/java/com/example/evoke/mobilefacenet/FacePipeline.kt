package com.example.evoke.mobilefacenet

import com.example.evoke.R
import androidx.annotation.StringRes
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import android.util.Log

/**
 * The ONE place where a photo becomes an embedding. Enrolment and recognition both call process(),
 * so a reference and a query always get exactly the same treatment:
 *
 *   detect -> choose face -> align (eyes + mouth corners) -> quality check -> embed (+ mirrored view)
 *
 * Create it lazily when a screen needs it and call close() when the screen goes away (the model is large).
 */
class FacePipeline(context: Context) {

    companion object {
        /** Faces whose shortest side is below this many pixels are ignored in the face picker (background people). */
        private const val MIN_PICKABLE_FACE_PX = 48

        /** Extra space around the face box for the profile photo, as a fraction of the face size on each side. */
        private const val PROFILE_MARGIN = 0.40f

        /** Profile photos are downscaled to at most this many pixels per side. */
        private const val PROFILE_MAX_PX = 512
    }

    enum class Reason { NO_FACE, NO_FRONT_FACING_FACE, LANDMARKS_MISSING, POOR_QUALITY }

    sealed class Outcome {
        class Success(
            val embedding: FloatArray,
            /** The aligned 112x112 face. Store it next to the embedding so it can be re-embedded after a model change. */
            val alignedFace: Bitmap,
            val quality: FaceQuality.Report,
            /** A square, upright, nicely framed photo of just this person, for showing as a profile picture. */
            val profilePhoto: Bitmap
        ) : Outcome()

        class Failure(val reason: Reason, @StringRes val messageRes: Int) : Outcome()
    }

    private val detector = FaceDetector()
    private val embedder = FaceEmbedder(context)

    /**
     * Every face worth offering in a "who do you want to check?" picker, left to right.
     * Tiny background faces are dropped.
     */
    suspend fun findFaces(bitmap: Bitmap): List<Face> =
        detector.detectFaces(bitmap)
            .filter { min(it.boundingBox.width(), it.boundingBox.height()) >= MIN_PICKABLE_FACE_PX }
            .sortedBy { it.boundingBox.left }

    /**
     * @param chosenFace a face the user picked by hand (from findFaces). When given, automatic face selection
     *                   and the head-pose filter are skipped, because the person has decided which face to use.
     *                   When null, the largest front-facing face is used.
     */
    suspend fun process(
        bitmap: Bitmap,
        profile: FaceQuality.Profile,
        chosenFace: Face? = null
    ): Outcome {

        val face: Face
        if (chosenFace != null) {
            face = chosenFace
        } else {
            val faces = detector.detectFaces(bitmap)
            if (faces.isEmpty()) {
                return Outcome.Failure(
                    Reason.NO_FACE,
                    R.string.face_no_face
                )
            }
            face = detector.selectBestFace(faces)
                ?: return Outcome.Failure(
                    Reason.NO_FRONT_FACING_FACE,
                    R.string.face_no_suitable
                )
        }

        val aligned = withContext(Dispatchers.Default) { FaceAligner.align(bitmap, face) }
            ?: return Outcome.Failure(
                Reason.LANDMARKS_MISSING,
                R.string.face_landmarks_missing
            )

        val quality = FaceQuality.check(face, aligned, profile)
        Log.d(
            "FaceQuality",
            "ok=${quality.ok} size=${quality.faceSizePx} luma=${quality.meanLuma} glare=${quality.eyeGlareFraction}"
        )
        if (!quality.ok) {
            return Outcome.Failure(
                Reason.POOR_QUALITY,
                quality.messageRes ?: R.string.face_quality_low
            )
        }

        val embedding = withContext(Dispatchers.Default) { embedder.getEmbedding(aligned) }
        val profilePhoto = withContext(Dispatchers.Default) { makeProfilePhoto(bitmap, face.boundingBox) }
        return Outcome.Success(embedding, aligned, quality, profilePhoto)
    }

    /** Embeds an already aligned 112x112 face (used to refresh stored faces after a model change). */
    suspend fun embedAligned(alignedFace: Bitmap): FloatArray =
        withContext(Dispatchers.Default) { embedder.getEmbedding(alignedFace) }

    /**
     * Square crop around the face with some room for hair and chin, taken from the full-resolution upright
     * photo (the 112x112 aligned face is too small and too tight to use as a picture).
     * The square is slid back inside the photo when the face is near an edge, so there is no black padding.
     */
    private fun makeProfilePhoto(source: Bitmap, box: Rect): Bitmap {
        val wanted = (max(box.width(), box.height()) * (1f + 2f * PROFILE_MARGIN)).roundToInt()
        val side = min(wanted, min(source.width, source.height)).coerceAtLeast(1)

        // Centre slightly above the box centre so the forehead and hair are not cut off.
        val centerX = box.centerX()
        val centerY = box.centerY() - (box.height() * 0.08f).roundToInt()

        val left = (centerX - side / 2).coerceIn(0, source.width - side)
        val top = (centerY - side / 2).coerceIn(0, source.height - side)

        val crop = Bitmap.createBitmap(source, left, top, side, side)
        return if (side > PROFILE_MAX_PX) {
            Bitmap.createScaledBitmap(crop, PROFILE_MAX_PX, PROFILE_MAX_PX, true)
        } else {
            crop
        }
    }

    fun close() {
        embedder.close()
        detector.close()
    }
}