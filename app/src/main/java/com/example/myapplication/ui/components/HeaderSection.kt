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
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.*
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppTheme
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

private fun buildHighlightLeftPath(w: Float, h: Float): Path {
    val sx = w / 452f; val sy = h / 307f
    return Path().apply {
        // M0 114 C1.0116 157.323 56.4394 192.139 100 192.139 H285.671 V198.466 H100 C55.8173 198.466 0 162.648 0 118.466 V114 Z
        moveTo(0f, 114f * sy)
        cubicTo(1.0116f * sx, 157.323f * sy, 56.4394f * sx, 192.139f * sy, 100f * sx, 192.139f * sy)
        lineTo(285.671f * sx, 192.139f * sy)
        lineTo(285.671f * sx, 198.466f * sy)
        lineTo(100f * sx, 198.466f * sy)
        cubicTo(55.8173f * sx, 198.466f * sy, 0f, 162.648f * sy, 0f, 118.466f * sy)
        close()

        // M290 198.466 H289.868 V191.844 H290 V198.466 Z
        moveTo(290f * sx, 198.466f * sy)
        lineTo(289.868f * sx, 198.466f * sy)
        lineTo(289.868f * sx, 191.844f * sy)
        lineTo(290f * sx, 191.844f * sy)
        close()
    }
}

private fun buildHighlightRightPath(w: Float, h: Float): Path {
    val sx = w / 452f; val sy = h / 307f
    return Path().apply {
        // M451.978 276.466 C450.988 233.143 415.561 198.327 372 198.327 H285.389 V192 H372 C416.183 192 452 227.817 452 272 V276.466 H451.978 Z
        moveTo(451.978f * sx, 276.466f * sy)
        cubicTo(450.988f * sx, 233.143f * sy, 415.561f * sx, 198.327f * sy, 372f * sx, 198.327f * sy)
        lineTo(285.389f * sx, 198.327f * sy)
        lineTo(285.389f * sx, 192f * sy)
        lineTo(372f * sx, 192f * sy)
        cubicTo(416.183f * sx, 192f * sy, 452f * sx, 227.817f * sy, 452f * sx, 272f * sy)
        lineTo(452f * sx, 276.466f * sy)
        close()

        // M283 192 H283.073 V198.622 H283 V192 Z
        moveTo(283f * sx, 192f * sy)
        lineTo(283.073f * sx, 192f * sy)
        lineTo(283.073f * sx, 198.622f * sy)
        lineTo(283f * sx, 198.622f * sy)
        close()
    }
}

private fun buildShadowOverlayLeftPath(w: Float, h: Float): Path {
    val sx = w / 452f; val sy = h / 307f
    return Path().apply {
        // M0 107 C1.0116 150.323 56.4394 185.139 100 185.139 H285.671 V191.466 H100 C55.8173 191.466 0 155.648 0 111.466 V107 Z
        moveTo(0f, 107f * sy)
        cubicTo(1.0116f * sx, 150.323f * sy, 56.4394f * sx, 185.139f * sy, 100f * sx, 185.139f * sy)
        lineTo(285.671f * sx, 185.139f * sy)
        lineTo(285.671f * sx, 191.466f * sy)
        lineTo(100f * sx, 191.466f * sy)
        cubicTo(55.8173f * sx, 191.466f * sy, 0f, 155.648f * sy, 0f, 111.466f * sy)
        close()

        // M290 191.466 H289.868 V184.844 H290 V191.466 Z
        moveTo(290f * sx, 191.466f * sy)
        lineTo(289.868f * sx, 191.466f * sy)
        lineTo(289.868f * sx, 184.844f * sy)
        lineTo(290f * sx, 184.844f * sy)
        close()
    }
}

private fun buildShadowOverlayRightPath(w: Float, h: Float): Path {
    val sx = w / 452f; val sy = h / 307f
    return Path().apply {
        // M451.978 269.466 C450.988 226.143 415.561 191.327 372 191.327 H285.389 V185 H372 C416.183 185 452 220.817 452 265 V269.466 H451.978 Z
        moveTo(451.978f * sx, 269.466f * sy)
        cubicTo(450.988f * sx, 226.143f * sy, 415.561f * sx, 191.327f * sy, 372f * sx, 191.327f * sy)
        lineTo(285.389f * sx, 191.327f * sy)
        lineTo(285.389f * sx, 185f * sy)
        lineTo(372f * sx, 185f * sy)
        cubicTo(416.183f * sx, 185f * sy, 452f * sx, 220.817f * sy, 452f * sx, 265f * sy)
        lineTo(452f * sx, 269.466f * sy)
        close()

        // M283 185 H283.073 V191.622 H283 V185 Z
        moveTo(283f * sx, 185f * sy)
        lineTo(283.073f * sx, 185f * sy)
        lineTo(283.073f * sx, 191.622f * sy)
        lineTo(283f * sx, 191.622f * sy)
        close()
    }
}

private fun DrawScope.drawHeaderShadow() {
    val w = size.width; val h = size.height
    drawPath(buildHeaderMainPath(w, h),   Color.Black)
    drawPath(buildHeaderCornerPath(w, h), Color.Black)
}


private fun buildHeaderCombinedPath(w: Float, h: Float): Path {
    val sx = w / 452f
    val sy = h / 307f  // ← back to 307f
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(452f * sx, 0f)
        lineTo(452f * sx, 273f * sy)
        cubicTo(
            452f     * sx, 228.817f * sy,
            416.183f * sx, 193f     * sy,
            372f     * sx, 193f     * sy
        )
        lineTo(100f * sx, 197.851f * sy)
        cubicTo(
            55.8172f * sx, 197.851f * sy,
            0f,            162.034f * sy,
            0f,            117.851f * sy
        )
        close()
    }
}

@Composable
fun HeaderSection(
    title          : String            = "Location",
    secondaryTitle : String?           = null,
    headerHeight   : Dp                = 260.dp,
    textSize       : TextUnit          = 40.sp,
    spacing        : Dp                = 91.dp,
    bottomspace    : Dp                = 37.dp,
    leaves         : Dp                = AppTheme.colors.headerDecorOffset1,
    onBack         : (() -> Unit)?     = null,
    centerButton   : String?           = null,
    onCenterButton : (() -> Unit)?     = null
) {
    val appColors = AppTheme.colors
    Box {


        // ── Shaped shadow — same blur/alpha as bottom XML shadow ──────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ — BlurEffect version
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .offset(y = 16.dp)
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
        } else {
            var cachedPath by remember { mutableStateOf<Path?>(null) }
            var cachedSize by remember { mutableStateOf(Pair(0f, 0f)) }

            val shadowHeight = headerHeight * (307f / 260f)  // ← restore this

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(shadowHeight)              // ← restore inflated height
                    .offset(y = (-16).dp)                 // ← match API 31+ offset
                    .drawBehind {
                        val w = size.width
                        val h = size.height

                        if (cachedSize != Pair(w, h)) {
                            cachedPath = buildHeaderCombinedPath(w, h)  // original function unchanged
                            cachedSize = Pair(w, h)
                        }
                        val combinedPath = cachedPath!!

                        val spreadLayers = listOf(
                            Pair(8f,  0.06f),
                            Pair(14f, 0.05f),
                            Pair(20f, 0.05f),
                            Pair(28f, 0.04f),
                            Pair(36f, 0.03f),
                            Pair(44f, 0.02f),
                            Pair(52f, 0.01f),
                        )

                        spreadLayers.forEach { (yOff, alpha) ->
                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply {
                                    color = Color.Black.copy(alpha = alpha)
                                }
                                canvas.withSave {
                                    canvas.translate(0f, yOff)
                                    canvas.drawPath(combinedPath, paint)
                                }
                            }
                        }
                    }
            )
        }

        // ── Header image ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .drawBehind {
                    val w = size.width
                    val h = size.height

                    // main green fill
                    drawPath(buildHeaderMainPath(w, h), color = appColors.headerBg)
                    drawPath(buildHeaderCornerPath(w, h), color = appColors.headerBg)

                    // inner shadow overlay (semi-transparent black, always hardcoded)
                    drawPath(buildShadowOverlayLeftPath(w, h), color = appColors.headerinnershadow.copy(alpha = 0.063f))
                    drawPath(buildShadowOverlayRightPath(w, h), color = appColors.headerinnershadow.copy(alpha = 0.063f))

                    // highlight border
                    drawPath(buildHighlightLeftPath(w, h), color = appColors.headerShadow)
                    drawPath(buildHighlightRightPath(w, h), color = appColors.headerShadow)
                }
        ) {
            // leaves, back button, title etc. — all unchanged
            // ── Leaves with shadow ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(390.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = appColors.headerDecorX, y = leaves)
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
                            painterResource(id = appColors.headerDecorationRes),
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
                            painterResource(id = appColors.headerDecorationRes),
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
                        color        = appColors.headerButton,
                        shadowColor  = appColors.buttonshadow,
                        cornerRadius = 30.dp,
                        onClick      = { onCenterButton?.invoke() }
                    ) {
                        Text(
                            text       = centerButton,
                            color      = appColors.headerButtonText,  // swapped: green text
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
                        tint               = appColors.backSign,
                        modifier           = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.back),
                        color    = appColors.backText,
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
                            color    = appColors.headerText,
                            fontSize = textSize
                        )
                        ShadowedText(
                            text     = secondaryTitle,
                            color    = appColors.headerSecondaryText,
                            fontSize = textSize,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                } else {
                    ShadowedText(
                        text     = title,
                        color    = appColors.headerText,
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