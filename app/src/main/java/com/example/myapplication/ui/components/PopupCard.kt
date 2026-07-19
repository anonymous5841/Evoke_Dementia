package com.example.myapplication.ui.components


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import androidx.navigation.NavController
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.theme.MartelFont
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import com.example.myapplication.ui.theme.MargarineFont
import com.example.myapplication.ui.components.ShadowButton
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.offsetShadow
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset

@Composable
fun PopupCard(
    messageText: String,
    buttonText: String = "Record",
    height: Float = 0.3f,
    upperPadding: Dp = 20.dp,
    showButton: Boolean = false,
    textOffset: DpOffset = DpOffset(x = (-34).dp, y = 293.dp),
    shapeOffset: DpOffset = DpOffset(x = (-6).dp, y = 285.dp),
    navController: NavController,
    onDismiss: () -> Unit = {},
    onButtonClick: () -> Unit = {},
) {
    val appColors = AppTheme.colors
    // ══════════════════════════════════════════════════════
    // REMOVED — the full-screen dark overlay Box that used to
    // live here. Blurring/dimming the screen BEHIND this popup
    // must happen at the call site (in AppNavigation.kt, where
    // the real background screen is visible), same pattern as
    // DetailedSummaryScreen. This composable is now ONLY the
    // popup card itself — see explanation below this code block.
    // ══════════════════════════════════════════════════════

    // ── OUTER WRAPPER — NOT clipped, lets yellow ellipse and X text escape freely ──
    Box(
        modifier = Modifier
            .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.6f)),   // ← dim scrim, added here
    contentAlignment = Alignment.Center
    ) {
        // ── POPUP CARD — clipping happens HERE only ──
        Box(
            modifier = Modifier
                .fillMaxWidth(0.93f)   // ← 80% of screen width, replaces fixed 300.dp
                .fillMaxHeight(height)
                .clip(RoundedCornerShape(20.dp))
                .background(appColors.pagesText)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(top = upperPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = messageText,
                    color = appColors.popupText,
                    fontSize = 33.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = OutfitFont,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Gray,
                            offset = Offset(-2f, 8f),
                            blurRadius = 5f
                        )
                    )
                )

                if (showButton) {
                    Spacer(modifier = Modifier.height(34.dp))  // [GAP ABOVE BUTTON] change dp
                    ShadowButton(
                        width        = 200.dp,                 // [BUTTON WIDTH] change dp
                        height       = 52.dp,                 // [BUTTON HEIGHT] change dp
                        color = appColors.popupText,
                        cornerRadius = 50.dp,                 // [BUTTON CORNER] = pill shape
                        onClick      = { onButtonClick()}
                    ) {
                        Text(
                            text       = buttonText,            // [BUTTON TEXT] change text
                            color      = appColors.pagesText,         // [TEXT COLOR] change color
                            fontSize   = 27.sp,               // [TEXT SIZE] change sp
                            fontWeight = FontWeight.Medium,
                            fontFamily = OutfitFont
                        )
                    }
                }
            }
        }   // ← closes POPUP CARD Box

        Image(
            painter = painterResource(id = R.drawable.add_shape),
            contentDescription = "Close",
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(
                color =  appColors.popupText,
                blendMode = BlendMode.SrcIn
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = shapeOffset.x, y = shapeOffset.y)
                .size(height = 70.dp, width = 78.dp)
                .offsetShadow(
                    offsetX = (-14).dp,
                    offsetY = 14.dp
                )
                .size(72.dp)                     // if not already applied here
                .clip(RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 0.dp,
                    bottomStart = 30.dp,
                    bottomEnd = 0.dp
                ))
                .clickable { onDismiss() }
        )

        // ── X TEXT ──
        Text(
            text = "X",
            color = appColors.pagesText,
            fontSize = 38.sp,
            fontFamily = MargarineFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = textOffset.x, y = textOffset.y)
                .clickable { onDismiss() }
        )
    }   // ← closes OUTER WRAPPER Box
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PopupCardNoButtonPreview() {
    val navController = rememberNavController()
    GreenTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            PopupCard(
                messageText = "Record Deleted Successfully!",
                height = 0.25f,
                upperPadding = 10.dp,
                textOffset = DpOffset(x = (-36).dp, y = 312.dp),
                shapeOffset = DpOffset(x = (-8).dp, y = 304.dp),
                showButton = false,
                navController = navController,
                onDismiss = {}
            )
        }
    }
}

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun PopupCardPreview() {
//    val navController = rememberNavController()
//    GreenTheme {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black.copy(alpha = 0.5f))  // simulate dimmed background behind the popup
//        ) {
//            PopupCard(
//                messageText = "Delete Record?",
//                buttonText = "Delete",
//                showButton = true,
//                navController = navController,
//                onDismiss = {}
//            )
//        }
//    }
//}