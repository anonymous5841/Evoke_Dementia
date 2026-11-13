package com.example.evoke.mobilefacenet

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.util.Log
import kotlin.math.abs

class FaceDetector {

    companion object {
        private const val MAX_HEAD_Y_ANGLE = 15f
        private const val MAX_HEAD_X_ANGLE = 15f
        private const val MAX_HEAD_Z_ANGLE = 15f
    }

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(
            FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE
        )
        .setLandmarkMode(
            FaceDetectorOptions.LANDMARK_MODE_ALL
        )
        .build()

    private val detector = FaceDetection.getClient(options)

    /**
     * Detects all faces in the image.
     */
    suspend fun detectFaces(bitmap: Bitmap): List<Face> {

        val image = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { cont ->

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (cont.isActive) {
                        cont.resume(faces)
                    }
                }
                .addOnFailureListener { error ->
                    if (cont.isActive) {
                        cont.resumeWithException(error)
                    }
                }
        }
    }

    /**
     * Checks whether the face is approximately facing the camera.
     *
     * These angles measure head orientation, NOT actual eye gaze.
     */
    fun isFacingCamera(face: Face): Boolean {

        val yaw = face.headEulerAngleY
        val pitch = face.headEulerAngleX
        val roll = face.headEulerAngleZ

        val passes =
            abs(yaw) <= MAX_HEAD_Y_ANGLE &&
                    abs(pitch) <= MAX_HEAD_X_ANGLE &&
                    abs(roll) <= MAX_HEAD_Z_ANGLE

        Log.d(
            "FaceSelection",
            """
        Face pose:
        Yaw: $yaw
        Pitch: $pitch
        Roll: $roll
        Passes pose filter: $passes
        """.trimIndent()
        )

        return passes
    }
    /**
     * Selects the largest approximately front-facing face.
     *
     * Returns null if no suitable face is detected.
     */
    fun selectBestFace(faces: List<Face>): Face? {

        Log.d("FaceSelection", "Total detected faces: ${faces.size}")

        if (faces.isEmpty()) {
            Log.d("FaceSelection", "No faces detected.")
            return null
        }

        // Log the bounding box and pose of every detected face.
        faces.forEachIndexed { index, face ->

            val box = face.boundingBox
            val area = box.width().toLong() * box.height().toLong()

            Log.d(
                "FaceSelection",
                """
            Face ${index + 1}:
            Bounding box: $box
            Width: ${box.width()}
            Height: ${box.height()}
            Area: $area
            """.trimIndent()
            )
        }

        // Keep only faces that pass the existing head-pose filter.
        val eligibleFaces = faces.filter { face ->
            isFacingCamera(face)
        }

        Log.d(
            "FaceSelection",
            "Faces passing pose filter: ${eligibleFaces.size}"
        )

        // Select the eligible face with the largest bounding-box area.
        val selectedFace = eligibleFaces.maxByOrNull { face ->
            face.boundingBox.width().toLong() *
                    face.boundingBox.height().toLong()
        }

        if (selectedFace != null) {

            val box = selectedFace.boundingBox

            Log.d(
                "FaceSelection",
                """
            SELECTED FACE:
            Bounding box: $box
            Width: ${box.width()}
            Height: ${box.height()}
            Area: ${box.width().toLong() * box.height().toLong()}
            """.trimIndent()
            )

        } else {
            Log.d("FaceSelection", "No face passed the pose filter.")
        }

        return selectedFace
    }
    fun close() {
        detector.close()
    }
}

