package com.example.evoke.ui

import com.example.evoke.R
import androidx.compose.ui.res.stringResource
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.evoke.mobilefacenet.FacePipeline
import com.example.evoke.mobilefacenet.FaceQuality
import com.example.evoke.mobilefacenet.FaceRecognitionResult
import com.example.evoke.mobilefacenet.decodeUprightBitmap
import com.example.evoke.ui.components.BackIconButton
import com.example.evoke.ui.components.CameraPreviewPlaceholder
import com.example.evoke.ui.components.ShadowButton
import com.example.evoke.ui.components.rememberBottomContentPadding
import com.example.evoke.ui.theme.AppTheme
import com.example.evoke.ui.theme.BlueTheme
import com.example.evoke.ui.theme.GreenTheme
import com.example.evoke.ui.theme.OutfitFont
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.activity.compose.BackHandler

class CapturedScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageUri = intent.getStringExtra("image_uri")?.let { Uri.parse(it) }
        setContent {
            GreenTheme {
                CapturedScreen(
                    imageUri = imageUri,
                    onBack = { finish() }
                )
            }
        }
    }
}

@Composable
fun CapturedScreen(
    imageUri: Uri? = null,
    onBack: () -> Unit = {},
    onProcessingComplete: (FaceRecognitionResult) -> Unit = {}
) {
    val appColors = AppTheme.colors
    val navBarPadding = rememberBottomContentPadding()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Created lazily on "Start processing" so the model isn't loaded just to show a preview.
    var pipeline by remember { mutableStateOf<FacePipeline?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Set only when the photo has several faces and the user must pick one.
    var pickerBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pickerFaces by remember { mutableStateOf<List<Face>>(emptyList()) }
    val isPicking = pickerBitmap != null && pickerFaces.isNotEmpty()

    DisposableEffect(Unit) {
        onDispose {
            pipeline?.close()
            pipeline = null
        }
    }

    // Inner BackHandlers win over the one in AppNavigation.
    // 1) While processing: ignore back, so the TFLite interpreter isn't closed mid-inference.
    BackHandler(enabled = isProcessing) { }
    // 2) While picking: back returns to the plain preview, same as the on-screen button.
    BackHandler(enabled = isPicking && !isProcessing) {
        pickerBitmap = null
        pickerFaces = emptyList()
        errorMessage = null
    }

    suspend fun runPipeline(active: FacePipeline, bitmap: Bitmap, face: Face?) {
        when (val outcome = active.process(bitmap, FaceQuality.Profile.RECOGNIZE, face)) {
            is FacePipeline.Outcome.Failure -> errorMessage = context.getString(outcome.messageRes)
            is FacePipeline.Outcome.Success -> {
                onProcessingComplete(
                    FaceRecognitionResult(
                        embedding = outcome.embedding,
                        sourceImageUri = imageUri.toString(),
                        alignedFace = outcome.alignedFace,
                        profilePhoto = outcome.profilePhoto
                    )
                )
            }
        }
    }

    val onStart: () -> Unit = start@{
        if (isProcessing || imageUri == null) return@start
        errorMessage = null
        isProcessing = true
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) { decodeUprightBitmap(context, imageUri) }
                val active = pipeline ?: withContext(Dispatchers.Default) {
                    FacePipeline(context.applicationContext)
                }.also { pipeline = it }

                val faces = active.findFaces(bitmap)
                when {
                    faces.isEmpty() ->
                        errorMessage = context.getString(R.string.face_no_face)
                    faces.size == 1 -> runPipeline(active, bitmap, null)
                    else -> {
                        pickerBitmap = bitmap
                        pickerFaces = faces
                    }
                }
            } catch (e: Exception) {
                Log.e("FaceRecognition", "Face processing failed", e)
                errorMessage = context.getString(R.string.face_process_offline_failed, e.message ?: "")
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            val horizontalPadding = (maxWidth * 0.06f)
                .coerceIn(20.dp, 32.dp)

            val topSpacing = (maxHeight * 0.025f)
                .coerceIn(12.dp, 20.dp)

            val backToCameraSpacing = (maxHeight * 0.035f)
                .coerceIn(20.dp, 32.dp) + 20.dp

            val cameraHeight = (maxHeight * 0.58f)
                .coerceIn(360.dp, 520.dp)

            val bottomSpacing = (maxHeight * 0.035f)
                .coerceIn(20.dp, 32.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
                    .padding(bottom = navBarPadding)
            ) {
                Spacer(Modifier.height(topSpacing))

                BackIconButton(
                    onBack = {
                        if (!isProcessing) {
                            if (isPicking) {
                                pickerBitmap = null
                                pickerFaces = emptyList()
                                errorMessage = null
                            } else onBack()
                        }
                    }
                )

                Spacer(Modifier.height(backToCameraSpacing))

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .height(cameraHeight)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isPicking -> FacePicker(
                            bitmap = pickerBitmap!!,
                            faces = pickerFaces,
                            enabled = !isProcessing,
                            onPick = pick@{ face ->
                                if (isProcessing) return@pick
                                errorMessage = null
                                isProcessing = true
                                scope.launch {
                                    try {
                                        val active = pipeline ?: return@launch
                                        runPipeline(active, pickerBitmap!!, face)
                                    } catch (e: Exception) {
                                        Log.e("FaceRecognition", "Face processing failed", e)
                                        errorMessage = context.getString(R.string.face_process_offline_failed, e.message ?: "")
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }
                        )
                        imageUri != null -> AsyncImage(
                            model = imageUri,
                            contentDescription = stringResource(R.string.face_captured_image),
                            contentScale = ContentScale.Fit,   // Fit so the whole photo is visible
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> CameraPreviewPlaceholder(height = cameraHeight)
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 13.sp,
                        fontFamily = OutfitFont,
                        color = Color(0xFFB3261E)
                    )
                }

                Spacer(Modifier.height(if (errorMessage != null) 16.dp else 44.dp))

                if (isPicking) {
                    Text(
                        text = if (isProcessing) stringResource(R.string.face_checking_person) else stringResource(R.string.face_tap_person),
                        fontSize = 16.sp,
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.Medium,
                        color = appColors.pagesText,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    CameraActions(isProcessing = isProcessing, onStart = onStart)
                }

                Spacer(Modifier.height(bottomSpacing))
            }
        }
    }
}

@Composable
private fun CameraActions(
    isProcessing: Boolean,
    onStart: () -> Unit
) {
    val appColors = AppTheme.colors

    ShadowButton(
        modifier = Modifier.fillMaxWidth(),
        width = null,
        height = 56.dp,
        color = appColors.pagesText,
        cornerRadius = 28.dp,
        shadowColor = appColors.backButton,
        shadowAlpha = 50,
        onClick = onStart
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                color = appColors.popupText,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.face_start_processing),
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = OutfitFont,
                color = appColors.popupText
            )
        }
    }
}

/**
 * Whole photo fitted inside the available space, with a numbered box on each face.
 * Taps are handled on the photo (not the small boxes), picking the nearest face within reach.
 */
@Composable
private fun FacePicker(
    bitmap: Bitmap,
    faces: List<Face>,
    enabled: Boolean,
    onPick: (Face) -> Unit
) {
    val appColors = AppTheme.colors
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val density = LocalDensity.current
    val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = constraints.maxWidth.toFloat()
        val availableHeight = constraints.maxHeight.toFloat()

        val fittedWidth = min(availableWidth, availableHeight * aspect)
        val fittedHeight = fittedWidth / aspect
        val scale = fittedWidth / bitmap.width
        val minReachPx = with(density) { 48.dp.toPx() }

        Box(
            modifier = Modifier
                .size(
                    width = with(density) { fittedWidth.toDp() },
                    height = with(density) { fittedHeight.toDp() }
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            Image(
                bitmap = image,
                contentDescription = stringResource(R.string.face_photo_several_people),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(faces, enabled, scale) {
                        detectTapGestures { tap ->
                            if (!enabled) return@detectTapGestures

                            var best: Face? = null
                            var bestDistance = Float.MAX_VALUE

                            faces.forEach { face ->
                                val box = face.boundingBox
                                val centerX = box.exactCenterX() * scale
                                val centerY = box.exactCenterY() * scale
                                val distance = hypot(tap.x - centerX, tap.y - centerY)
                                val reach = max(
                                    max(box.width(), box.height()) * scale * 0.75f,
                                    minReachPx
                                )
                                if (distance <= reach && distance < bestDistance) {
                                    best = face
                                    bestDistance = distance
                                }
                            }
                            Log.d("FacePicker", "tap at (${tap.x}, ${tap.y}) -> picked=${best != null}")
                            best?.let { onPick(it) }
                        }
                    }
            )

            faces.forEachIndexed { index, face ->
                val box = face.boundingBox
                Box(
                    modifier = Modifier
                        .absoluteOffset {
                            IntOffset(
                                (box.left * scale).roundToInt(),
                                (box.top * scale).roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { (box.width() * scale).toDp() },
                            height = with(density) { (box.height() * scale).toDp() }
                        )
                        .border(3.dp, appColors.pagesText, RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 14.sp,
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.Bold,
                        color = appColors.popupText,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(bottomEnd = 8.dp))
                            .background(appColors.pagesText)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CapturedScreenPreview() {
    BlueTheme { CapturedScreen() }
}