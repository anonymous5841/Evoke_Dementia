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
import com.example.animation.LiquidBlobAnimation
import com.example.animation.VoiceRecorderAnimation
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import androidx.compose.ui.res.stringResource


private fun Modifier.blurIfSupported(radius: Dp): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius = radius)
    } else {
        this
    }
}

/**
 * The blur/glow-heavy [VoiceRecorderAnimation] uses Modifier.blur (via its
 * own blurIfSupported helper) and layered translucent gradients that are
 * tuned to look right with real blur behind them. Below API 31 that blur
 * silently becomes a no-op, so those rings would render sharp-edged and
 * flat instead of glassy — it still "works", it just doesn't look right.
 *
 * [LiquidBlobAnimation] was built from the ground up with no blur/RenderEffect
 * dependency at all, so it looks correct and intentional on every version
 * below that cutoff.
 *
 * This flag is the single switch point for that decision — check it once
 * here rather than duplicating the SDK check anywhere else in the screen.
 */
private val supportsBlurAndGlow: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
fun RecordScreen(
    onBack: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    var isRecording by remember { mutableStateOf(true) }
    val appColors = AppTheme.colors
    var Colory = Color.Unspecified
    if (supportsBlurAndGlow) {
        Colory= Color.White
    } else {
        Colory = Color.Black
    }
        Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // insets already handled at root
    ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                val cardWidth = maxWidth * 0.88f
                val cardHeight = minOf(
                    cardWidth * 1.50f,
                    (maxHeight - 260.dp - 56.dp) * 0.95f
                )
                val voiceAnimationSize = cardWidth * 1.20f
                val blobAnimationSize = cardWidth * 1.00f


                // Calculate spacing
                val headerHeight = 260.dp
                val bottomPadding = 28.dp
                val availableHeight = maxHeight - headerHeight - bottomPadding
                val topPadding = (availableHeight - cardHeight) / 2f

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {

                        // Same idea as your LocationSearch screen
                        // Header space
                        Spacer(modifier = Modifier.height(headerHeight))

                        // Top padding to center card
                        Spacer(modifier = Modifier.height(topPadding.coerceAtLeast(12.dp)))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = (-20).dp), // tweak this if needed
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // ── Recording Card ─────────────────────────────────
                            Box(
                                modifier = Modifier
                                    .width(cardWidth)
                                    .height(cardHeight)
                                    .shadow(
                                        elevation = 40.dp,
                                        shape = RoundedCornerShape(62.dp),
                                        ambientColor = Color.DarkGray.copy(alpha = 0.8f),
                                        spotColor = Color.DarkGray.copy(alpha = 0.9f)
                                    )
                                    .background(
                                        appColors.pagesText,
                                        RoundedCornerShape(62.dp)
                                    ),
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
                                        modifier = Modifier.fillMaxWidth(1f).fillMaxHeight(0.7f)
                                    ) {
                                        // API 31+ gets the blur/glow version (it's built to
                                        // rely on real Modifier.blur); everything below that
                                        // gets the no-blur liquid blob so it still looks
                                        // intentional instead of flat.
                                        if (supportsBlurAndGlow) {
                                            VoiceRecorderAnimation(
                                                modifier = Modifier.size(voiceAnimationSize),
                                                isAnimating = isRecording
                                            )
                                        } else {
                                            LiquidBlobAnimation(
                                                modifier = Modifier.size(blobAnimationSize)
                                            )
                                        }

                                        Box(contentAlignment = Alignment.Center) {
                                            // White shadow - same icon shape, blurred, drawn behind
                                            Icon(
                                                painter = painterResource(id = R.drawable.recording_icon),
                                                contentDescription = null, // decorative
                                                tint = Colory.copy(alpha = 0.4f),
                                                modifier = Modifier
                                                    .size(50.dp)
                                                    .blurIfSupported(6.dp)
                                            )

                                            // Actual microphone icon
                                            Icon(
                                                painter = painterResource(id = R.drawable.recording_icon),
                                                contentDescription = stringResource(R.string.recording),
                                                tint = appColors.popupText,   // Use the themed color from main
                                                modifier = Modifier
                                                    .size(50.dp)
                                            )
                                        }

                                    }
                                }

                                // Done button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)    // [BUTTON POSITION] bottom center
                                        .padding(bottom = cardHeight * 0.10f)
                                ) {

                                    ShadowButton(
                                        width = cardWidth * 0.70f,
                                        height = 48.dp,              // [BUTTON HEIGHT] matches Recorder.svg (48dp)
                                        color = appColors.popupText,  // [BUTTON COLOR] matches Recorder.svg (#FFC107)
                                        cornerRadius = 24.dp,              // full pill — matches Recorder.svg rx=24
                                        shadowColor = Color.White,
                                        onClick = { onDoneClick() }
                                    ) {
                                        Text(
                                            text = stringResource(R.string.done_button),
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = OutfitFont
                                        )
                                    }
                                }
                            }
                        }
                        // Bottom padding to center card
                        Spacer(modifier = Modifier.height(topPadding.coerceAtLeast(12.dp)))

                        // Minimum bottom padding
                        Spacer(modifier = Modifier.height(bottomPadding))
                    }

                    HeaderSection(
                        title = stringResource(R.string.record),
                        spacing = 68.dp,
                        bottomspace = 40.dp,
                        onBack = { onBack() }
                    )
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