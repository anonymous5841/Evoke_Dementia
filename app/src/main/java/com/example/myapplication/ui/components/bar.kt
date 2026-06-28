package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

data class MeowBottomNavigationModel(
    val id   : Int,
    val icon : ImageVector,
    val count: String = ""
)

@Composable
fun MeowBottomNavigation(
    models            : List<MeowBottomNavigationModel>,
    selectedId        : Int,
    onTabSelected     : (MeowBottomNavigationModel) -> Unit,
    modifier          : Modifier = Modifier,
    backgroundColor   : Color    = Color(0xFF3E634F),
    selectedIconColor : Color    = Color(0xFFFFCD38),
    defaultIconColor  : Color    = Color.White,
    circleColor       : Color    = Color(0xFF3E634F),
    hasAnimation      : Boolean  = true
) {
    val density      = LocalDensity.current
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val systemGestureInset = WindowInsets.systemGestures.asPaddingValues().calculateBottomPadding()

// Only add padding if it's button navigation (not gesture)
// Gesture nav inset is typically >= navigationBars inset
    val extraBottomPadding = if (systemGestureInset <= navBarInset) navBarInset * 0.4f else 0.dp

    val designHeight = 108.dp
    val totalHeight  = designHeight + extraBottomPadding
    val barTopFrac   = 38f   / 108f
    val circleCYFrac = 33f   / 108f
    val circleRFrac  = 33f   / 108f
    val notchBotFrac = 79.5f / 108f
    val notchHWFrac  = 65f   / 412f
    val cornerRFrac  = 25f   / 412f
    val iconBarYFrac = 64.5f / 108f
    val iconPopYFrac = 33.5f / 108f

    val isNone = selectedId < 0 || models.none { it.id == selectedId }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)          // ← outer box is taller
            .graphicsLayer { renderEffect = null }
    ) {
        val W  = with(density) { maxWidth.toPx() }
        val H  = with(density) { designHeight.toPx() }   // ← design draws against 108dp only
        val HH = with(density) { totalHeight.toPx() }    // ← full height for background fill

        val selectedIndex = models.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        val targetX       = W * (selectedIndex + 0.5f) / models.size

        val animX by animateFloatAsState(
            targetValue   = targetX,
            animationSpec = if (hasAnimation) spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessLow
            ) else snap(),
            label = "meowX"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val bTop  = H * barTopFrac
            val cY    = H * circleCYFrac
            val cR    = H * circleRFrac
            val nBot  = H * notchBotFrac
            val nHalf = W * notchHWFrac
            val cRad  = W * cornerRFrac
            val cx    = animX

            if (isNone) {
                val barPath = Path().apply {
                    moveTo(0f, HH)              // ← bottom goes to full height
                    lineTo(0f, bTop + cRad)
                    quadraticBezierTo(0f, bTop, cRad, bTop)
                    lineTo(W - cRad, bTop)
                    quadraticBezierTo(W, bTop, W, bTop + cRad)
                    lineTo(W, HH)               // ← bottom goes to full height
                    close()
                }

                drawIntoCanvas { canvas ->
                    canvas.drawPath(
                        barPath,
                        androidx.compose.ui.graphics.Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                color       = android.graphics.Color.TRANSPARENT
                                setShadowLayer(20f, 0f, -8f,
                                    android.graphics.Color.argb(100, 0, 0, 0))
                            }
                        }
                    )
                }
                drawPath(barPath, backgroundColor)

            } else {
                val lx = cx - nHalf
                val rx = cx + nHalf

                val barWithNotch = Path().apply {
                    moveTo(0f, HH)              // ← bottom goes to full height
                    lineTo(0f, bTop + cRad)
                    quadraticBezierTo(0f, bTop, cRad, bTop)
                    lineTo(lx, bTop)
                    cubicTo(cx - nHalf * 0.66f, bTop, cx - nHalf * 0.40f, nBot, cx, nBot)
                    cubicTo(cx + nHalf * 0.40f, nBot, cx + nHalf * 0.66f, bTop, rx, bTop)
                    lineTo(W - cRad, bTop)
                    quadraticBezierTo(W, bTop, W, bTop + cRad)
                    lineTo(W, HH)               // ← bottom goes to full height
                    close()
                }

                drawIntoCanvas { canvas ->
                    canvas.drawPath(
                        barWithNotch,
                        androidx.compose.ui.graphics.Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                color       = android.graphics.Color.TRANSPARENT
                                setShadowLayer(20f, 0f, -8f,
                                    android.graphics.Color.argb(100, 0, 0, 0))
                            }
                        }
                    )
                }
                drawPath(barWithNotch, backgroundColor)

                drawCircle(color = circleColor, radius = cR, center = Offset(cx, cY))
            }
        }

        // ── Icons in bar — positions unchanged, still based on H (108dp) ─────
        models.forEachIndexed { index, model ->
            val isSelected = model.id == selectedId
            val iconCXDp   = with(density) { (W * (index + 0.5f) / models.size).toDp() }
            val iconBarYDp = with(density) { (H * iconBarYFrac).toDp() }

            val scale by animateFloatAsState(
                targetValue   = if (isNone) 1f else if (isSelected) 0f else 1f,
                animationSpec = if (hasAnimation) spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessLow
                ) else snap(),
                label = "scale$index"
            )

            Box(
                modifier         = Modifier
                    .offset(x = iconCXDp - 24.dp, y = iconBarYDp - 24.dp)
                    .size(48.dp)
                    .clickable { onTabSelected(model) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = model.icon,
                    contentDescription = null,
                    tint               = defaultIconColor,
                    modifier           = Modifier
                        .size(28.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                )
            }
        }

        if (!isNone) {
            val selectedModel = models.find { it.id == selectedId }
            if (selectedModel != null) {
                val bubbleCXDp = with(density) { animX.toDp() }
                val bubbleYDp  = with(density) { (H * iconPopYFrac).toDp() }

                Box(
                    modifier         = Modifier
                        .offset(x = bubbleCXDp - 24.dp, y = bubbleYDp - 24.dp)
                        .size(48.dp)
                        .clickable { onTabSelected(selectedModel) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = selectedModel.icon,
                        contentDescription = null,
                        tint               = selectedIconColor,
                        modifier           = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}