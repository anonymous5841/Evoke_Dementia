package com.example.myapplication.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.width
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
import com.airbnb.lottie.compose.*
import androidx.compose.ui.draw.blur
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.components.ShadowButton


@Composable
fun RecordScreen(
    onBack: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    var isRecording by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // ── Header ────────────────────────────────────
        HeaderSection(
            title = "Record",
            null,
            308.dp,
            33.sp,
            72.dp,
            (42).dp,
            (-9).dp,
            onBack = { }
        )

        Box(
            modifier = Modifier
                .width(340.dp)
                .padding(horizontal = 18.dp, vertical = 4.dp)
                .offset(y = (-76).dp)
                .height(410.dp)
                .shadow(
                    elevation = 40.dp, // ✅ strong shadow
                    shape = RoundedCornerShape(60.dp),
                    ambientColor = Color.DarkGray.copy(alpha = 0.8f),
                    spotColor = Color.DarkGray.copy(alpha = 0.9f)
                )
                // ❌ remove .clip() here
                .background(
                    Color(0xFF3E634F),
                    shape = RoundedCornerShape(60.dp)
                ), // ✅ background with shape
            contentAlignment = Alignment.Center
        ) {

            // Column for animation + icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                // Animation + Icon (unchanged)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(380.dp)
                ) {
                    val composition by rememberLottieComposition(
                        LottieCompositionSpec.RawRes(R.raw.recording_animation)
                    )
                    val progress by animateLottieCompositionAsState(
                        composition = composition,
                        iterations = LottieConstants.IterateForever,
                        speed = 1.3f
                    )


                    LottieAnimation(
                        composition = composition,
                        progress = { progress },
                        modifier = Modifier
                            .size(380.dp)
                            .offset(y = (-70).dp)
                            .blur(20.dp)   // ✅ Apply blur here
                    )

                    Icon(
                        painter = painterResource(id = R.drawable.recording_icon),
                        contentDescription = "Recording",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(50.dp).offset(y = (-65).dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)    // [BUTTON POSITION] bottom center
                    .padding(bottom = 55.dp)          // [MOVE DOWN] decrease to move button down
            )

            {
                ShadowButton(
                    width = 180.dp,            // [BUTTON WIDTH]
                    height = 50.dp,             // [BUTTON HEIGHT]
                    color = Color(0xFFFFC107), // [BUTTON COLOR]
                    cornerRadius = 50.dp,             // [BUTTON CORNER]
                    onClick = { onDoneClick() }
                ) {
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = OutfitFont
                    )
                }
            }

        }

    }
}