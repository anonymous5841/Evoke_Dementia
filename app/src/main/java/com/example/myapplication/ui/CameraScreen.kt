package com.example.myapplication.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.BackIconButton
import com.example.myapplication.ui.components.CameraPreviewPlaceholder
import com.example.myapplication.ui.components.GlowCaptureButton
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont

class CameraPreviewScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreenTheme {
                CameraPreviewContent()
            }
        }
    }
}

@Composable
fun CameraPreviewContent(
    onBack: () -> Unit = {},
    onCapture: () -> Unit = {},
    onGallery: () -> Unit = {}
) {

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {

            Spacer(modifier = Modifier.height(18.dp))

            BackIconButton(
                onBack
            )

            Spacer(modifier = Modifier.height(28.dp))

            CameraPreviewPlaceholder()

            Spacer(modifier = Modifier.weight(1f))

            CameraActions(
                onCapture = onCapture,
                onGallery = onGallery
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun CameraActions(
    onCapture: () -> Unit,
    onGallery: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {

        // Gallery Button
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .clickable { onGallery() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gallery",
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Capture Button
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            GlowCaptureButton(
                onClick = onCapture
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Capture",
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
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