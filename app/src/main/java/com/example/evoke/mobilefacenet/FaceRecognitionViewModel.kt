package com.example.evoke.mobilefacenet

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel

class FaceRecognitionViewModel(app: Application) : AndroidViewModel(app) {

    /** The ONE store instance for the whole app (camera lookup now, enrolment later). */
    val store = FaceTemplateStore(app)

    var lastResult by mutableStateOf<FaceRecognitionResult?>(null)
    var matchResult by mutableStateOf<FaceMatchResult?>(null)

    private var cachedBitmap: Bitmap? = null
    private var cachedImage: ImageBitmap? = null

    val profileBitmap: ImageBitmap?
        get() {
            val bmp = lastResult?.profilePhoto ?: return null
            if (bmp !== cachedBitmap) {
                cachedBitmap = bmp
                cachedImage = bmp.asImageBitmap()
            }
            return cachedImage
        }

    fun clear() {
        lastResult = null
        matchResult = null
        cachedBitmap = null
        cachedImage = null
    }
}