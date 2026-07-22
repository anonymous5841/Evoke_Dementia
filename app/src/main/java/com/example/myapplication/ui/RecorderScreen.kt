package com.example.myapplication.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.HeaderSection
import androidx.compose.ui.draw.blur
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.example.animation.VoiceRecorderAnimation
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme


private fun Modifier.blurIfSupported(radius: Dp): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius = radius)
    } else {
        this
    }
}
@Composable
fun RecordScreen(
    onBack: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    var isRecording by remember { mutableStateOf(true) }
    val appColors = AppTheme.colors
    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // insets already handled at root
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Header ────────────────────────────────────
            HeaderSection(
                title = "Record",
                spacing = 68.dp,
                bottomspace = 40.dp,
                onBack = { onBack()}
            )

            // ── Recording Card ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)        // [CARD MARGIN] matches Recorder.svg side margin (24dp)
                    .height(496.dp)                       // [CARD HEIGHT] matches Recorder.svg (496dp)
                    .shadow(
                        elevation = 40.dp, // ✅ strong shadow
                        shape = RoundedCornerShape(62.dp), // [CARD CORNER] matches Recorder.svg rx=62
                        ambientColor = Color.DarkGray.copy(alpha = 0.8f),
                        spotColor = Color.DarkGray.copy(alpha = 0.9f)
                    )
                    .background(
                        appColors.pagesText,
                        shape = RoundedCornerShape(62.dp)
                    ), // ✅ background with shape
                contentAlignment = Alignment.Center
            ) {

                // Column for animation + icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Animation + Icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.
                        fillMaxWidth(1f).
                        fillMaxHeight(0.7f)
                    ) {
                        VoiceRecorderAnimation(
                            modifier = Modifier
                                .size(400.dp, 300.dp)
                        )

                        Box(contentAlignment = Alignment.Center) {
                            // White shadow - same icon shape, blurred, 30% opacity, drawn behind
                            Icon(
                                painter = painterResource(id = R.drawable.recording_icon),
                                contentDescription = null, // decorative, real description is on the icon above
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(50.dp)
                                    .blurIfSupported(6.dp)
                            )

                            // The actual icon, drawn on top - unchanged
                            Icon(
                                painter = painterResource(id = R.drawable.recording_icon),
                                contentDescription = "Recording",
                                tint = appColors.popupText, // [MIC COLOR] matches Recorder.svg mic fill
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                }

                // Done button
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)    // [BUTTON POSITION] bottom center
                        .padding(bottom = 52.dp)           // [BUTTON BOTTOM MARGIN] matches Recorder.svg spacing from card bottom
                ) {

                    ShadowButton(
                        width = 252.dp,             // [BUTTON WIDTH] matches Recorder.svg (252dp)
                        height = 48.dp,              // [BUTTON HEIGHT] matches Recorder.svg (48dp)
                        color = appColors.popupText,  // [BUTTON COLOR] matches Recorder.svg (#FFC107)
                        cornerRadius = 24.dp,              // full pill — matches Recorder.svg rx=24
                        shadowColor  = Color.White,
                        onClick = { onDoneClick() }
                    ) {
                        Text(
                            text = "Done",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = OutfitFont
                        )
                    }
                }

            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecorderPreview() {
    GreenTheme {
        RecordScreen()
    }
}