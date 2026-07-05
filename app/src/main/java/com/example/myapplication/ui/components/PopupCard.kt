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
@Composable
fun PopupCard(
    messageText: String,
    showButton: Boolean = false,
    navController: NavController,
    onRecordClick: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
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
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // ── POPUP CARD — clipping happens HERE only ──
        Box(
            modifier = Modifier
                .width(300.dp)
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF3E634F))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = messageText,
                    color = Color(0xFFFFC006),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = OutfitFont,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Gray,
                            offset = Offset(-2f, 8f),
                            blurRadius = 5f
                        )
                    )
                )

                if (showButton) {
                    Spacer(modifier = Modifier.height(30.dp))  // [GAP ABOVE BUTTON] change dp
                    ShadowButton(
                        width        = 160.dp,                 // [BUTTON WIDTH] change dp
                        height       = 50.dp,                 // [BUTTON HEIGHT] change dp
                        color        = Color(0xFFFFC006),     // [BUTTON COLOR] = yellow
                        cornerRadius = 50.dp,                 // [BUTTON CORNER] = pill shape
                        onClick      = { navController.navigate("record_screen") }
                    ) {
                        Text(
                            text       = "Record",            // [BUTTON TEXT] change text
                            color      = Color.White,         // [TEXT COLOR] change color
                            fontSize   = 16.sp,               // [TEXT SIZE] change sp
                            fontWeight = FontWeight.Bold      // [TEXT WEIGHT] change weight
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp)) // [GAP BELOW BUTTON] change dp
                }
            }
        }   // ← closes POPUP CARD Box

        // ══════════════════════════════════════════════════════
        // YELLOW ELLIPSE — background shape only, fully independent
        // Moved OUT of the popup card, now a sibling living in the
        // unclipped outer wrapper — same fix pattern used in
        // ViewMoreScreen and DetailedSummaryScreen.
        // [ELLIPSE SIZE] change size(72.dp)
        // [ELLIPSE OFFSET X] positive = right, negative = left
        // [ELLIPSE OFFSET Y] negative = up, positive = down
        // ══════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.TopEnd)
                .offset(
                    x = (-20).dp,
                    y = (268).dp
                )
        ) {
            // ── SHADOW LAYER — separate, offset down so it doesn't show on top ──
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .offset(
                        x = 0.dp,
                        y = 6.dp
                    )
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        clip = false,                       // ADDED — was missing here, needed for shadow to render
                        ambientColor = Color(0xFF000000),
                        spotColor = Color(0xFF000000)
                    )
                    .background(Color.Transparent)          // ADDED — gives shadow a surface to render against
            )

            // ── REAL YELLOW SHAPE — on top, no shadow attached directly to it ──
            Image(
                painter = painterResource(id = R.drawable.add_shape),
                contentDescription = "Close",
                colorFilter = ColorFilter.tint(
                    color = Color(0xFFFFC006),         // [TINT COLOR] change to any color
                    blendMode = BlendMode.SrcIn        // SrcIn replaces all colors with tint color
                ),
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDismiss() }              // uses onDismiss — matches PopupCard's actual parameter
            )
        }

        // ══════════════════════════════════════════════════════
        // X TEXT — replaces the old Icon(R.drawable.x), now Text,
        // fully independent of the yellow ellipse.
        // [X TEXT SIZE] change fontSize 18.sp
        // [X TEXT OFFSET X] positive = right, negative = left
        // [X TEXT OFFSET Y] negative = up, positive = down
        // ══════════════════════════════════════════════════════
        Text(
            text = "X",
            color = Color(0xFF3E634F),
            fontSize = 38.sp,
            fontFamily = MargarineFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(
                    x = (-45).dp,
                    y = (280).dp
                )
                .clickable { onDismiss() }                  // uses onDismiss — matches PopupCard's actual parameter
        )
    }   // ← closes OUTER WRAPPER Box
}