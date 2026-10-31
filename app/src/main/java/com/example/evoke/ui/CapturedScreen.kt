package com.example.evoke.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evoke.ui.components.BackIconButton
import com.example.evoke.ui.components.CameraPreviewPlaceholder
import com.example.evoke.ui.components.ShadowButton
import com.example.evoke.ui.components.rememberBottomContentPadding
import com.example.evoke.ui.theme.AppTheme
import com.example.evoke.ui.theme.GreenTheme
import com.example.evoke.ui.theme.BlueTheme
import com.example.evoke.ui.theme.OutfitFont
import androidx.compose.ui.text.font.FontWeight


import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage

class CapturedScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUri = intent.getStringExtra("image_uri")
            ?.let { Uri.parse(it) }

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
    onCapture: () -> Unit = {}
) {
    val appColors = AppTheme.colors
    val navBarPadding = rememberBottomContentPadding()

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

                Spacer(modifier = Modifier.height(topSpacing))

                BackIconButton(onBack = onBack)

                Spacer(modifier = Modifier.height(backToCameraSpacing))

                // Keeps the design height (cameraHeight) when there is room.
                // On short screens it shrinks to the space left, so the
                // Capture button is never pushed under the nav bar.
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .height(cameraHeight)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Captured image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CameraPreviewPlaceholder(
                            height = cameraHeight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(44.dp))

                CameraActions(onCapture = onCapture)

                Spacer(modifier = Modifier.height(bottomSpacing))
            }
        }
    }
}

@Composable
private fun CameraActions(
    onCapture: () -> Unit
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
        onClick = onCapture
    ) {
        Text(
            text = "Start processing",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = OutfitFont,
            color = appColors.popupText
        )
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun CapturedScreenPreview() {
    BlueTheme {
        CapturedScreen()
    }
}