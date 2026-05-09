package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind

@Composable
fun HeaderSection(
    title: String = "Location",
    secondaryTitle: String? = null,
    headerHeight: Dp = 260.dp,
    textSize: TextUnit = 39.sp,
    spacing: Dp = 91.dp,
    leaves: Dp = (-9).dp,
    onBack: (() -> Unit)? = null,
    centerButton   : String?           = null,        // ← new: pass button label e.g. "Add"
    onCenterButton : (() -> Unit)?     = null
) {
    Box {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .offset(y = (-90).dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    renderEffect = BlurEffect(
                        radiusX = 10f,
                        radiusY = 45f,
                        edgeTreatment = TileMode.Decal
                    )
                    alpha = 0.55f
                }
                .drawBehind {
                    drawRect(Color.Black)
                }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .paint(
                    painterResource(id = R.drawable.headers),
                    contentScale = ContentScale.Crop
                )
        ) {
            // Leaves decoration (always shown)
            Box(
                modifier = Modifier
                    .size(390.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 0.dp, y = leaves)
                    .paint(
                        painterResource(id = R.drawable.leaves),
                        contentScale = ContentScale.Fit
                    )
            )

            if (centerButton != null) {
                // ── Center button mode: no back, no title ─────────────────
                Box(
                    modifier         = Modifier.fillMaxWidth(0.8f) .align(Alignment.Center).padding(bottom = 75.dp),
                    contentAlignment = Alignment.BottomCenter


                ) {
                    ShadowButtonFull(
                        height       = 56.dp,
                        color        = MaterialTheme.colorScheme.onPrimary,  // swapped: white bg
                        cornerRadius = 30.dp,
                        onClick      = { onCenterButton?.invoke() }
                    ) {
                        Text(
                            text       = centerButton,
                            color      = Color.Black,  // swapped: green text
                            fontSize   = 26.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

            } else {
                // ── Normal mode: back button + title ──────────────────────
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                    modifier              = Modifier
                        .padding(start = 25.dp, top = 15.dp)
                        .align(Alignment.TopStart)
                        .clickable { onBack?.invoke() }   // ← add this

                ) {
                    Icon(
                        painter            = painterResource(id = R.drawable.back_icon),
                        contentDescription = "back icon",
                        tint               = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier           = Modifier.size(20.dp)
                    )
                    Text(
                        text      = "Back",
                        color     = MaterialTheme.colorScheme.secondary,
                        fontSize  = 30.sp,
                    )
                }

                val titleModifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = spacing, bottom = 27.dp)

                if (secondaryTitle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = titleModifier
                    ) {
                        ShadowedText(
                            text     = title,
                            color    = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = textSize
                        )
                        ShadowedText(
                            text     = secondaryTitle,
                            color    = MaterialTheme.colorScheme.inversePrimary,
                            fontSize = textSize,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                } else {
                    ShadowedText(
                        text     = title,
                        color    = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontSize = textSize,
                        modifier = titleModifier
                    )
                }
            }
        }
    }
}

// ShadowedText with EXACT same parameters as Language text
@Composable
private fun ShadowedText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Shadow Layer - Exact same as Language text
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(y = 6.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    renderEffect = BlurEffect(
                        radiusX = 18f,
                        radiusY = 18f,
                        edgeTreatment = TileMode.Decal
                    )
                    alpha = 0.3f
                }
        )

        // Main Text
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold
        )
    }
}