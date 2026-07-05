package com.example.myapplication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState        // ADDED — needed for scrollable summary card
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll              // ADDED — needed for scrollable summary card
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.ui.theme.MargarineFont

@Composable
fun DetailedSummaryScreen(
    onCloseClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val summaryPoints = listOf(
        "Dementia is a progressive brain disorder causing cognitive decline,Memory loss,impaired thinking" +
                " and behavioural changes_____that interferes eith daily life. It requires " +
                "escalating care fr occasional prompts to 24/7 support.Effective management focuses on safety," +
                "consistent routines,calm communication,and addressing behavioral triggers.Stages of progression:early stage" +
                "involve mild memory "
    )

    // ══════════════════════════════════════════════════════
    // REMOVED — the full-screen blur + translucent tint Box
    // that used to wrap everything here. Blurring the screen
    // BEHIND this popup must happen at the call site
    // (e.g. in AppNavigation.kt, where ViewMoreScreen is the
    // actual visible background), not inside this composable.
    // This composable is now ONLY the popup card itself.
    // ══════════════════════════════════════════════════════

    // ── OUTER WRAPPER — NOT clipped, lets yellow ellipse and X text escape freely ──
    Box(
        modifier = Modifier
            .fillMaxSize()              // ← changed: fill the WHOLE screen, not just width
            .padding(32.dp),
        contentAlignment = Alignment.Center   // ← centers everything inside (the card + ellipse + X)
    ) {
        // ── POPUP CARD — clipping happens HERE only, on the card background ──
        // [CARD MAX HEIGHT] change heightIn(max = 400.dp) — controls when scrolling kicks in
        Box(
            modifier = Modifier
                .fillMaxWidth(1f)
                .heightIn(max = 600.dp)                 // ADDED — caps card height so content scrolls instead of growing forever
                .shadow(
                    elevation = 16.dp,                    // lowered from 100.dp — 100.dp is extreme and may behave oddly/clip weirdly on some devices
                    shape = RoundedCornerShape(20.dp),
                    clip = false,
                    ambientColor = Color.DarkGray,
                    spotColor = Color.DarkGray
                )
                 // ← ADD THIS — gives the shadow a surface to render against

                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF3E634F))
                .align(Alignment.Center)
                .padding(20.dp)
        ) {
            // ── SCROLLABLE CONTENT — header + bullet points ──
            // [SCROLL] verticalScroll added so large summaries can be scrolled instead of overflowing
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .verticalScroll(rememberScrollState())   // ADDED — makes content scrollable when it exceeds card height
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discussion Summary",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MartelFont,
                        color = Color(0xFFFFC006)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bullet points
                summaryPoints.forEach { point ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = "•",
                            fontSize = 16.sp,
                            fontFamily = MartelFont,
                            color = Color.White,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = point,
                            fontSize = 18.sp,
                            fontFamily = MartelFont,
                            color = Color.White
                        )
                    }
                }
            }
        }   // ← closes POPUP CARD Box

        // ══════════════════════════════════════════════════════
        // YELLOW ELLIPSE — background shape only, fully independent
        // Moved OUT of the popup card (was a child of it before),
        // now a sibling living in the unclipped outer wrapper —
        // same fix pattern used for the green ellipse in ViewMoreScreen.
        // [ELLIPSE SIZE] change size(58.dp)
        // [ELLIPSE OFFSET X] positive = right, negative = left
        // [ELLIPSE OFFSET Y] negative = up, positive = down
        // ══════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.TopEnd)
                .offset(
                    x = (11).dp,
                    y = (48).dp
                )
        ) {
            // ── SHADOW LAYER — separate, offset down so it doesn't show on top ──
            // [SHADOW SIZE] change size(72.dp) — independent of the real shape's size
            // [SHADOW OFFSET] change y — higher = shadow pushed further down/away from top
            Box(
                modifier = Modifier
                    .size(92.dp)                      // matches the new 72.dp size from your update
                    .offset(
                        x = 0.dp,                      // [SHADOW OFFSET X]
                        y = 6.dp                        // [SHADOW OFFSET Y] — pushes shadow down so it's hidden behind the real shape on top
                    )
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0xFF000000),
                        spotColor = Color(0xFF000000)
                    )
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
                    .clickable { onCloseClick() }
            )
        }
        // ══════════════════════════════════════════════════════
        // X TEXT — replaces the old Icon(R.drawable.x), now a Text
        // composable instead, fully independent of the yellow ellipse
        // (same approach used for the "+" plus text in ViewMoreScreen).
        // [X TEXT SIZE] change fontSize 18.sp
        // [X TEXT OFFSET X] positive = right, negative = left
        // [X TEXT OFFSET Y] negative = up, positive = down
        // ══════════════════════════════════════════════════════
        Text(
            text = "X",
            color = Color(0xFF3E634F),
            fontSize = 38.sp,                          // [X TEXT SIZE] — matches old icon size(18.dp)
            fontFamily = MargarineFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)               // [X TEXT ALIGNMENT] change freely
                .offset(
                    x = (-17).dp,                          // [X TEXT OFFSET X] tune to land on ellipse center
                    y = (56).dp                         // [X TEXT OFFSET Y] tune to land on ellipse center
                )
                .clickable { onCloseClick() }          // X itself is also clickable to close, independent of ellipse
        )
    }   // ← closes OUTER WRAPPER Box
}