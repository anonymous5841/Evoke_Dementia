package com.example.evoke.mobilefacenet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/** Decodes [uri] into a Bitmap with EXIF rotation/mirroring already applied,
 *  so downstream face detection + embedding see the photo the way a person
 *  would. Works for both file:// (camera capture) and content:// (gallery
 *  pick) uris — everything reads from the local content resolver, no
 *  network involved. */
fun decodeUprightBitmap(context: Context, uri: Uri): Bitmap {
    val raw = context.contentResolver.openInputStream(uri)
        ?.use { BitmapFactory.decodeStream(it) }
        ?: error("Couldn't decode image")

    val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
        ExifInterface(stream).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } ?: ExifInterface.ORIENTATION_NORMAL

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        else -> return raw // ORIENTATION_NORMAL / undefined — nothing to do
    }

    return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
}