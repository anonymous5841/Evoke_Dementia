package com.example.evoke.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evoke.ui.components.BackIconButton
import com.example.evoke.ui.components.CameraPreviewPlaceholder
import com.example.evoke.ui.components.GlowCaptureButton
import com.example.evoke.ui.theme.AppTheme
import com.example.evoke.ui.theme.GreenTheme
import com.example.evoke.ui.theme.OutfitFont
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.evoke.ui.components.rememberBottomContentPadding
import com.example.evoke.R


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class CameraPreviewScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreenTheme {
                CameraPreviewContent(
                    onBack = { finish() },
                    onImageReady = { uri ->
                        startActivity(
                            Intent(this, CapturedScreenActivity::class.java)
                                .putExtra("image_uri", uri.toString())
                        )
                    }
                )
            }
        }
    }
}


@Composable
fun CameraPreviewContent(
    onBack: () -> Unit = {},
    onImageReady: (Uri) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val appColors = AppTheme.colors

    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

// ── NEW: re-check permission when returning from system Settings ──
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasCameraPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> uri?.let { onImageReady(it) } }

    // ── NEW: capture guard (replaces the old onCapture) ──
    var isCapturing by remember { mutableStateOf(false) }

    val onCapture: () -> Unit = {
        if (hasCameraPermission && !isCapturing) {
            isCapturing = true
            val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        isCapturing = false
                        onImageReady(Uri.fromFile(file))
                    }
                    override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                        Log.e("CameraPreview", "Capture failed", exception)
                    }
                }
            )
        }
    }

    val onGallery: () -> Unit = {
        galleryLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    // The bottom nav bar is shown on this screen (flat style, no bubble),
    // so keep the controls above its visible part.
    val navBarPadding = rememberBottomContentPadding()

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * ---------------------------------------------------------
             * RESPONSIVE DIMENSIONS
             * ---------------------------------------------------------
             */

            // Horizontal margin for the whole screen
            val horizontalPadding = (maxWidth * 0.06f)
                .coerceIn(20.dp, 32.dp)

            // Space above back button
            val topSpacing = (maxHeight * 0.025f)
                .coerceIn(12.dp, 20.dp)

            // Space between back button and camera preview
            val backToCameraSpacing = (maxHeight * 0.035f)
                .coerceIn(20.dp, 32.dp)

            /*
             * Camera preview should occupy most of the available
             * vertical space, but must leave room for the controls.
             */
            val cameraHeight = (maxHeight * 0.58f)
                .coerceIn(360.dp, 520.dp)

            // Space after camera preview
            val bottomSpacing = (maxHeight * 0.035f)
                .coerceIn(20.dp, 32.dp)


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
                    .padding(bottom = navBarPadding)
            ) {

                /*
                 * -----------------------------------------------------
                 * BACK BUTTON
                 * -----------------------------------------------------
                 */

                Spacer(
                    modifier = Modifier.height(topSpacing)
                )

                BackIconButton(
                    onBack = onBack
                )


                /*
                 * -----------------------------------------------------
                 * BACK BUTTON → CAMERA
                 * -----------------------------------------------------
                 */

                Spacer(
                    modifier = Modifier.height(backToCameraSpacing)
                )


                /*
                 * -----------------------------------------------------
                 * CAMERA PREVIEW
                 * -----------------------------------------------------
                 */

                // Keeps the design height (cameraHeight) when there is room.
                // On short screens it shrinks to the space left, so the
                // capture / gallery / refresh controls are never pushed
                // under the nav bar.
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .height(cameraHeight)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    if (hasCameraPermission) {
                        LiveCameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            imageCapture = imageCapture
                        )
                    } else {
                        CameraPreviewPlaceholder(height = cameraHeight)
                    }
                }


                /*
                 * -----------------------------------------------------
                 * CAMERA ACTIONS
                 * -----------------------------------------------------
                 */

                CameraActions(
                    onCapture = onCapture,
                    onGallery = onGallery,
                    onRefresh = onRefresh
                )


                /*
                 * -----------------------------------------------------
                 * BOTTOM SPACE
                 * -----------------------------------------------------
                 */

                Spacer(
                    modifier = Modifier.height(bottomSpacing)
                )
            }
        }
    }
}

@Composable
private fun CameraActions(
    onCapture: () -> Unit,
    onGallery: () -> Unit,
    onRefresh: () -> Unit
) {

    val appColors = AppTheme.colors

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                170.dp
            )
    ) {

        /*
         * Responsive sizes
         */

        val sidePadding = (maxWidth * 0.06f)
            .coerceIn(16.dp, 28.dp)

        val iconButtonSize = (maxWidth * 0.17f)
            .coerceIn(58.dp, 65.dp)

        val iconSize = (maxWidth * 0.145f)
            .coerceIn(48.dp, 55.dp)

        val captureSize = (maxWidth * 0.23f)
            .coerceIn(78.dp, 90.dp)

        val labelSpacing = (maxWidth * 0.025f)
            .coerceIn(6.dp, 10.dp)


        /*
         * ---------------------------------------------------------
         * GALLERY
         * ---------------------------------------------------------
         */

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = sidePadding,
                    top = 18.dp
                )
                .clickable {
                    onGallery()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            IconButton(
                onClick = {
                    onGallery()
                },
                modifier = Modifier.size(
                    iconButtonSize
                )
            ) {

                Icon(
                    painter = painterResource(
                        R.drawable.gallery_icon
                    ),
                    contentDescription = stringResource(
                        R.string.gallery_icon
                    ),
                    modifier = Modifier.size(
                        iconSize
                    ),
                    tint = appColors.backButton
                )
            }

            Spacer(
                modifier = Modifier.height(
                    labelSpacing
                )
            )

            Text(
                text = stringResource(
                    R.string.gallery
                ),
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = Color.Black
            )
        }


        /*
         * ---------------------------------------------------------
         * CAPTURE
         * ---------------------------------------------------------
         */

        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            GlowCaptureButton(
                size = captureSize,
                onClick = onCapture
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = stringResource(
                    R.string.capture
                ),
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = Color.Black
            )
        }


        /*
         * ---------------------------------------------------------
         * REFRESH
         * ---------------------------------------------------------
         */

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    end = sidePadding,
                    top = 18.dp
                )
                .clickable {
                    onRefresh()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            IconButton(
                onClick = {
                    onRefresh()
                },
                modifier = Modifier.size(
                    iconButtonSize
                )
            ) {

                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(
                        R.string.refresh_icon
                    ),
                    modifier = Modifier.size(
                        iconSize
                    ),
                    tint = appColors.backButton
                )
            }

            Spacer(
                modifier = Modifier.height(
                    labelSpacing
                )
            )

            Text(
                text = stringResource(
                    R.string.refresh
                ),
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun LiveCameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }.also { previewView ->
                val future = ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    val provider = future.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        }
    )
}
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun CameraPreviewScreenPreview() {

    GreenTheme {
        CameraPreviewContent()
    }

}
