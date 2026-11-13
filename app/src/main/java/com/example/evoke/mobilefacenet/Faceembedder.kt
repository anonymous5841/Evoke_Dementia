package com.example.evoke.mobilefacenet

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Locale
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Fully offline MobileFaceNet inference wrapper.
 *
 * Loads mobilefacenet.tflite from:
 * app/src/main/assets/mobilefacenet.tflite
 *
 * Uses TensorFlow Lite Interpreter to generate face embeddings.
 *
 * No network calls, no Play Services model downloads, and no
 * external services are required. All inference happens locally.
 *
 * ------------------------------------------------------------
 * MODEL EXPECTATIONS
 * ------------------------------------------------------------
 *
 * Model: sirius-ai MobileFaceNet
 *
 * Input:
 *  - Shape: 1 x 112 x 112 x 3
 *  - Data type: Float32
 *  - Color format: RGB
 *  - Normalization: (pixel - 127.5) / 128
 *
 * Output:
 *  - Shape: 1 x 128
 *  - Data type: Float32
 *  - Represents a 128-dimensional face embedding.
 *
 * The model graph already performs L2 normalization internally.
 * We normalize the output again to ensure consistent unit-length
 * embeddings before calculating cosine similarity.
 *
 * ------------------------------------------------------------
 * FACE PROCESSING PIPELINE
 * ------------------------------------------------------------
 *
 * This class handles embedding generation only.
 *
 * Face detection and face selection happen in FaceDetector.kt.
 *
 * The complete pipeline is:
 *
 * 1. Receive a bitmap containing the selected face.
 *
 * 2. The bitmap is already cropped using ML Kit's face bounding
 *    box, with additional margin around the face.
 *
 * 3. Ensure the bitmap is square.
 *
 * 4. Resize the bitmap to 112 x 112 pixels.
 *
 * 5. Extract RGB pixel values.
 *
 * 6. Normalize each channel using:
 *
 *       (pixel - 127.5) / 128
 *
 * 7. Pass the Float32 input buffer to MobileFaceNet.
 *
 * 8. Run TensorFlow Lite inference.
 *
 * 9. Retrieve the 128-dimensional output.
 *
 * 10. Apply L2 normalization.
 *
 * 11. Print the generated embedding in Logcat for debugging.
 *
 * 12. Return the normalized embedding for face matching.
 *
 * IMPORTANT:
 * This class does NOT detect faces, select between multiple
 * faces, or perform face alignment.
 *
 * Those operations are handled separately by FaceDetector.kt.
 *
 * The same preprocessing must be used for reference images
 * and newly captured camera/gallery images.
 */
class FaceEmbedder(
    context: Context,
    modelAssetName: String = "facemodel/mobilefacenet.tflite") {

    companion object {

        private const val TAG = "FaceEmbedder"

        // MobileFaceNet input dimensions.
        private const val INPUT_SIZE = 112

        // sirius-ai MobileFaceNet output embedding dimensions.
        private const val EMBEDDING_SIZE = 128

        // Input normalization parameters.
        private const val PIXEL_MEAN = 127.5f
        private const val PIXEL_STD = 128.0f

        // Float32 occupies 4 bytes per channel.
        private const val BYTES_PER_CHANNEL = 4

        // RGB has three channels.
        private const val CHANNELS = 3
    }

    /**
     * TensorFlow Lite interpreter.
     *
     * The model is loaded once when FaceEmbedder is created,
     * rather than being loaded for every captured image.
     */
    private val interpreter: Interpreter =
        Interpreter(loadModelFile(context, modelAssetName))


    /**
     * Loads the .tflite model from the app's assets directory.
     *
     * Expected location:
     * app/src/main/assets/mobilefacenet.tflite
     *
     * Uses a memory-mapped model buffer for efficient loading.
     */
    private fun loadModelFile(
        context: Context,
        assetName: String
    ): ByteBuffer {

        val assetFileDescriptor = context.assets.openFd(assetName)

        FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->

            val fileChannel = inputStream.channel

            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength

            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
            )
        }
    }


    /**
     * Generates a normalized face embedding from a cropped face bitmap.
     *
     * @param faceBitmap Square face crop produced by cropToFace().
     *
     * @return A normalized FloatArray containing 128 values.
     *
     * The returned embedding can later be compared with stored
     * reference embeddings using cosine similarity.
     */
    fun getEmbedding(alignedFace: Bitmap): FloatArray {
        require(alignedFace.width == INPUT_SIZE && alignedFace.height == INPUT_SIZE) {
            "Expected a 112x112 aligned face."
        }
        val plain   = runModel(bitmapToInputBuffer(alignedFace, mirror = false))
        val flipped = runModel(bitmapToInputBuffer(alignedFace, mirror = true))
        // notebook: l2( l2(plain) + l2(flip) )
        return l2Normalize(FloatArray(EMBEDDING_SIZE) { plain[it] + flipped[it] })
    }




    /**
     * Converts a bitmap into MobileFaceNet's Float32 input buffer.
     *
     * Processing:
     *
     * 1. Resize the square bitmap to 112 x 112.
     * 2. Extract RGB pixel values.
     * 3. Normalize each channel:
     *
     *       normalized = (pixel - 127.5) / 128
     *
     * 4. Store the normalized values in a native-order ByteBuffer.
     *
     * Input tensor shape:
     * [1, 112, 112, 3]
     */


    private fun runModel(input: ByteBuffer): FloatArray {
        val output = Array(1) { FloatArray(EMBEDDING_SIZE) }
        interpreter.run(input, output)
        return l2Normalize(output[0])
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap, mirror: Boolean): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * CHANNELS * BYTES_PER_CHANNEL)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val sx = if (mirror) INPUT_SIZE - 1 - x else x
                val pixel = pixels[y * INPUT_SIZE + sx]
                buffer.putFloat((((pixel shr 16) and 0xFF) - PIXEL_MEAN) / PIXEL_STD)
                buffer.putFloat((((pixel shr 8) and 0xFF) - PIXEL_MEAN) / PIXEL_STD)
                buffer.putFloat(((pixel and 0xFF) - PIXEL_MEAN) / PIXEL_STD)
            }
        }
        buffer.rewind()
        return buffer
    }

    /**
     * Applies L2 normalization to the generated embedding.
     *
     * Formula:
     *
     *       normalized_embedding = embedding / ||embedding||2
     *
     * This ensures the embedding has unit length.
     *
     * It is useful for consistent cosine similarity calculations.
     */
    private fun l2Normalize(embedding: FloatArray): FloatArray {

        var sumSquares = 0f

        for (value in embedding) {
            sumSquares += value * value
        }

        val norm = sqrt(sumSquares).takeIf {
            it > 0f
        } ?: 1f

        return FloatArray(embedding.size) {
            embedding[it] / norm
        }
    }


    /**
     * Prints the generated embedding in Logcat.
     *
     * Expected output:
     *
     * FaceEmbedder: Generated embedding (128D):
     * [0.012345, -0.023456, ...]
     *
     * In Android Studio, filter Logcat using:
     *
     * FaceEmbedder
     *
     * DEBUGGING NOTE:
     * Face embeddings are biometric representations.
     * Remove or disable this logging in the production build.
     */
    private fun logEmbedding(embedding: FloatArray) {

        val formattedEmbedding = embedding.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ", "
        ) {
            String.format(Locale.US, "%.6f", it)
        }

        Log.d(
            TAG,
            "Generated embedding (${embedding.size}D): $formattedEmbedding"
        )
    }


    /**
     * Releases TensorFlow Lite native resources.
     *
     * Call this when the screen or recognition flow using
     * this FaceEmbedder is disposed.
     */
    fun close() {
        interpreter.close()
    }
}