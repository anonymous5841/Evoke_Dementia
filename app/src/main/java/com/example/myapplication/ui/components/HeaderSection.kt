package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.myapplication.R
import com.example.myapplication.ui.theme.BaumansFont
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.theme.PompiereFont

// ── Header shadow paths (from header_shadow.xml, viewport 452×307) ───────────
// Main body: M0 0 H452 V197.851 H100 C55.8172,197.851 0,162.034 0,117.851 V0 Z
private fun buildHeaderMainPath(w: Float, h: Float): Path {
    val sx = w / 452f; val sy = h / 307f
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(452f * sx, 0f)
        lineTo(452f * sx, 197.851f * sy)
        lineTo(100f  * sx, 197.851f * sy)
        cubicTo(
            55.8172f * sx, 197.851f * sy,
            0f,            162.034f * sy,
            0f,            117.851f * sy
        )
        close()
    }
}

// Corner piece: M452 273 C452,228.817 416.183,193 372,193 H452 V273 Z
private fun buildHeaderCornerPath(w: Float, h: Float): Path {
    val sx = w / 452f; val sy = h / 307f
    return Path().apply {
        moveTo(452f * sx, 273f * sy)
        cubicTo(
            452f    * sx, 228.817f * sy,
            416.183f* sx, 193f     * sy,
            372f    * sx, 193f     * sy
        )
        lineTo(452f * sx, 193f * sy)
        lineTo(452f * sx, 273f * sy)
        close()
    }
}

private fun DrawScope.drawHeaderShadow() {
    val w = size.width; val h = size.height
    drawPath(buildHeaderMainPath(w, h),   Color.Black)
    drawPath(buildHeaderCornerPath(w, h), Color.Black)
}

@Composable
fun HeaderSection(
    title          : String            = "Location",
    secondaryTitle : String?           = null,
    headerHeight   : Dp                = 260.dp,
    textSize       : TextUnit          = 40.sp,
    spacing        : Dp                = 91.dp,
    bottomspace    : Dp                = 37.dp,
    leaves         : Dp                = (-9).dp,
    onBack         : (() -> Unit)?     = null,
    centerButton   : String?           = null,
    onCenterButton : (() -> Unit)?     = null
) {
    Box {

        // ── Shaped shadow — same blur/alpha as bottom XML shadow ──────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .offset(y = 16.dp)          // shift down so shadow peeks below header
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    renderEffect = BlurEffect(
                        radiusX       = 10f,
                        radiusY       = 50f,
                        edgeTreatment = TileMode.Decal
                    )
                    alpha = 0.65f
                }
                .drawBehind { drawHeaderShadow() }
        )

        // ── Header image ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .paint(
                    painterResource(id = R.drawable.headers),
                    contentScale = ContentScale.Crop
                )
        ) {
            // ── Leaves with shadow ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(390.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 0.dp, y = leaves)
            ) {
                // Shadow layer — same image, blurred + darkened + shifted down
                Box(
                    modifier = Modifier
                        .size(390.dp)
                        .offset(x = 2.dp, y = 6.dp)   // shift right+down for shadow
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            renderEffect = BlurEffect(
                                radiusX       = 9f,
                                radiusY       = 9f,
                                edgeTreatment = TileMode.Decal
                            )
                            alpha = 0.4f               // shadow intensity
                        }
                        .paint(
                            painterResource(id = R.drawable.leaves),
                            contentScale = ContentScale.Fit,
                            colorFilter  = ColorFilter.tint(
                                Color.Black,
                                BlendMode.SrcIn          // replace all colors with black
                            )
                        )
                )

                // Real leaves on top
                Box(
                    modifier = Modifier
                        .size(390.dp)
                        .paint(
                            painterResource(id = R.drawable.leaves),
                            contentScale = ContentScale.Fit
                        )
                )
            }

            if (centerButton != null) {
                // ── Center button mode: no back, no title ─────────────────
                Box(
                    modifier         = Modifier.fillMaxWidth(0.8f) .align(Alignment.Center).padding(bottom = 80.dp),
                    contentAlignment = Alignment.BottomCenter


                ) {
                    ShadowButton(
                        height       = 56.dp,
                        color        = MaterialTheme.colorScheme.onPrimary,  // swapped: white bg
                        cornerRadius = 30.dp,
                        onClick      = { onCenterButton?.invoke() }
                    ) {
                        Text(
                            text       = centerButton,
                            color      = Color.Black,  // swapped: green text
                            fontSize   = 29.sp,            // changed from 26.sp
                            fontWeight = FontWeight.Medium, // changed from SemiBold
                            fontFamily = OutfitFont
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
                        text     = "Back",
                        color    = MaterialTheme.colorScheme.secondary,
                        fontSize = 30.sp,          // changed from 30.sp
                        fontFamily = PompiereFont
                    )
                }

                val titleModifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = spacing, bottom = bottomspace)

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

@Composable
private fun ShadowedText(
    text     : String,
    color    : Color,
    fontSize : TextUnit,
    modifier : Modifier = Modifier
) {
    Box(modifier = modifier) {
        Text(
            text       = text,
            color      = color,
            fontSize   = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = BaumansFont,
            modifier   = Modifier
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
        Text(
            text       = text,
            color      = color,
            fontSize   = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = BaumansFont
        )
    }
}