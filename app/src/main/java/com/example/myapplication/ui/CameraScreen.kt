package com.example.myapplication.ui

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
import com.example.myapplication.R
import com.example.myapplication.ui.components.BackIconButton
import com.example.myapplication.ui.components.CameraPreviewPlaceholder
import com.example.myapplication.ui.components.GlowCaptureButton
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.graphics.Color

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
    onGallery: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val appColors = AppTheme.colors

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // insets already handled at root

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

            CameraActions(
                onCapture = onCapture,
                onGallery = onGallery,
                onRefresh = onRefresh,
            )

            Spacer(modifier = Modifier.height(30.dp))
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .padding(top = 15.dp)

    ) {

        // Gallery Button
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp, top = 18.dp)
                .clickable { onGallery() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            IconButton(
                onClick = { onGallery() },
                modifier = Modifier.size(65.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.gallery_icon),
                    contentDescription = "Gallery",
                    modifier = Modifier.size(55.dp),
                    tint = appColors.backButton,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Gallery",
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = Color.Black
            )
        }

        // Capture Button
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 10.dp)
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            GlowCaptureButton(
                onClick = onCapture
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Capture",
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = Color.Black
            )
        }

        // Refresh Button
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp, top = 18.dp)
                .clickable { onRefresh() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            IconButton(
                onClick = { onRefresh() },
                modifier = Modifier.size(65.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    modifier = Modifier.size(55.dp),
                    tint = appColors.backButton,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Refresh",
                fontSize = 16.sp,
                fontFamily = OutfitFont,
                color = Color.Black
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