//package com.example.myapplication.storage
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import java.io.File
//import java.io.FileOutputStream
//
//class PersonImageStorage(
//    context: Context
//) {
//
//    private val appFilesDirectory = context.filesDir
//
//    private val imageDirectory =
//        File(appFilesDirectory, "person_images").apply {
//            if (!exists()) {
//                mkdirs()
//            }
//        }
//
//    fun saveImage(
//        personId: Long,
//        bitmap: Bitmap
//    ): String {
//
//        val file = File(
//            imageDirectory,
//            "$personId.jpg"
//        )
//
//        FileOutputStream(file).use { outputStream ->
//
//            bitmap.compress(
//                Bitmap.CompressFormat.JPEG,
//                90,
//                outputStream
//            )
//        }
//
//        return "person_images/${file.name}"
//    }
//
//    fun getImage(
//        imagePath: String
//    ): Bitmap? {
//
//        val file = File(
//            appFilesDirectory,
//            imagePath
//        )
//
//        return if (file.exists()) {
//            BitmapFactory.decodeFile(file.absolutePath)
//        } else {
//            null
//        }
//    }
//
//    fun deleteImage(
//        imagePath: String?
//    ) {
//
//        if (imagePath.isNullOrBlank()) {
//            return
//        }
//
//        val file = File(
//            appFilesDirectory,
//            imagePath
//        )
//
//        if (file.exists()) {
//            file.delete()
//        }
//    }
//
//    fun deleteImage(
//        personId: Long
//    ) {
//
//        val file = File(
//            imageDirectory,
//            "$personId.jpg"
//        )
//
//        if (file.exists()) {
//            file.delete()
//        }
//    }
//}


package com.example.evoke.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class PersonImageStorage(
    private val context: Context
) {

    companion object {
        private const val PEOPLE_DIRECTORY = "people"

        // Profile images are only for display.
        private const val PROFILE_IMAGE_SIZE = 800

        // Good balance between quality and file size.
        private const val JPEG_QUALITY = 80

        private const val IMAGE_EXTENSION = ".jpg"
    }

    private val peopleDirectory: File by lazy {
        File(context.filesDir, PEOPLE_DIRECTORY).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Saves a profile image using the detected face bounding box.
     *
     * The original image is NOT stored.
     *
     * Processing:
     * 1. Crop around the detected face
     * 2. Add a margin around the face
     * 3. Make the crop square
     * 4. Resize to 800 x 800
     * 5. Compress as JPEG
     *
     * Returns a RELATIVE path such as:
     *
     * people/person_1.jpg
     */
    fun saveProfileImage(
        personId: Long,
        bitmap: Bitmap,
        faceBox: Rect,
        marginFraction: Float = 0.25f
    ): String {

        val croppedBitmap = cropToFace(
            bitmap = bitmap,
            faceBox = faceBox,
            marginFraction = marginFraction
        )

        val resizedBitmap = resizeBitmap(
            bitmap = croppedBitmap,
            size = PROFILE_IMAGE_SIZE
        )

        val imageFile = File(
            peopleDirectory,
            "person_$personId$IMAGE_EXTENSION"
        )

        FileOutputStream(imageFile).use { outputStream ->
            resizedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                JPEG_QUALITY,
                outputStream
            )
        }

        if (resizedBitmap !== croppedBitmap) {
            resizedBitmap.recycle()
        }

        croppedBitmap.recycle()

        // Store only the relative path in Room.
        return "$PEOPLE_DIRECTORY/${imageFile.name}"
    }

    /**
     * Loads a profile image using the relative path stored in Person.
     *
     * Example:
     *
     * people/person_1.jpg
     */
    fun loadProfileImage(
        imagePath: String
    ): Bitmap? {

        val imageFile = resolveImageFile(imagePath)
            ?: return null

        if (!imageFile.exists()) {
            return null
        }

        return BitmapFactory.decodeFile(
            imageFile.absolutePath
        )
    }

    /**
     * Deletes a profile image using the person ID.
     */
    fun deleteProfileImage(
        personId: Long
    ): Boolean {

        val imageFile = File(
            peopleDirectory,
            "person_$personId$IMAGE_EXTENSION"
        )

        return !imageFile.exists() || imageFile.delete()
    }

    /**
     * Deletes a profile image using the relative path
     * stored in Person.profileImagePath.
     */
    fun deleteProfileImage(
        imagePath: String
    ): Boolean {

        val imageFile = resolveImageFile(imagePath)
            ?: return false

        return !imageFile.exists() || imageFile.delete()
    }

    /**
     * Deletes every stored person profile image.
     *
     * Useful when clearing all local application data.
     */
    fun deleteAllProfileImages() {

        if (!peopleDirectory.exists()) {
            return
        }

        peopleDirectory.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }
    }

    /**
     * Returns the application's private people directory.
     * this function is giving JVM error uncomment at your own risk!!!
     */
//    fun getPeopleDirectory(): File {
//        return peopleDirectory
//    }

    /**
     * Converts a stored relative path into a file inside
     * the application's private people directory.
     *
     * Only paths belonging to the "people" directory are accepted.
     */
    private fun resolveImageFile(
        imagePath: String
    ): File? {

        if (imagePath.isBlank()) {
            return null
        }

        val normalizedPath =
            imagePath.replace('\\', '/')

        val expectedPrefix =
            "$PEOPLE_DIRECTORY/"

        if (!normalizedPath.startsWith(expectedPrefix)) {
            return null
        }

        val fileName =
            normalizedPath.removePrefix(expectedPrefix)

        // We only expect a filename, not another directory.
        if (
            fileName.isBlank() ||
            fileName.contains("/") ||
            fileName == "." ||
            fileName == ".."
        ) {
            return null
        }

        val imageFile =
            File(peopleDirectory, fileName)

        // Final safety check: ensure the resolved file
        // remains inside the people directory.
        val directoryPath =
            peopleDirectory.canonicalPath + File.separator

        return try {
            val canonicalPath =
                imageFile.canonicalPath

            if (canonicalPath.startsWith(directoryPath)) {
                imageFile
            } else {
                null
            }

        } catch (e: Exception) {
            null
        }
    }

    /**
     * Crops the bitmap around the detected face.
     *
     * A margin is added around the face so the result
     * is not a pixel-tight face crop.
     */
    private fun cropToFace(
        bitmap: Bitmap,
        faceBox: Rect,
        marginFraction: Float
    ): Bitmap {

        val marginX =
            (faceBox.width() * marginFraction).toInt()

        val marginY =
            (faceBox.height() * marginFraction).toInt()

        var left =
            faceBox.left - marginX

        var top =
            faceBox.top - marginY

        var right =
            faceBox.right + marginX

        var bottom =
            faceBox.bottom + marginY

        val width =
            right - left

        val height =
            bottom - top

        val side =
            max(width, height)

        val centerX =
            (left + right) / 2

        val centerY =
            (top + bottom) / 2

        left =
            centerX - side / 2

        top =
            centerY - side / 2

        right =
            left + side

        bottom =
            top + side

        // Clamp to bitmap boundaries.
        left =
            max(0, left)

        top =
            max(0, top)

        right =
            min(bitmap.width, right)

        bottom =
            min(bitmap.height, bottom)

        val cropWidth =
            right - left

        val cropHeight =
            bottom - top

        require(
            cropWidth > 0 && cropHeight > 0
        ) {
            "Invalid face crop dimensions."
        }

        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            cropWidth,
            cropHeight
        )
    }

    /**
     * Resizes the bitmap to the desired square size.
     */
    private fun resizeBitmap(
        bitmap: Bitmap,
        size: Int
    ): Bitmap {

        if (
            bitmap.width == size &&
            bitmap.height == size
        ) {
            return bitmap
        }

        return Bitmap.createScaledBitmap(
            bitmap,
            size,
            size,
            true
        )
    }
}