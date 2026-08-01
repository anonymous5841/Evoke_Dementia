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
import com.example.myapplication.ui.theme.AppTheme

data class MeowBottomNavigationModel(
    val id   : Int,
    val icon : ImageVector,
    val count: String = ""
)

// Shared with BottomNavBar.kt — the reserved headroom above the solid bar,
// where the selected tab's bubble pops up into.
internal val NAV_BAR_DESIGN_HEIGHT = 108f
internal val NAV_BAR_TOP_FRACTION  = 38f / NAV_BAR_DESIGN_HEIGHT

@Composable
fun MeowBottomNavigation(
    models            : List<MeowBottomNavigationModel>,
    selectedId        : Int,
    onTabSelected     : (MeowBottomNavigationModel) -> Unit,
    modifier          : Modifier = Modifier,
    backgroundColor   : Color    = AppTheme.colors.headerBg,
    selectedIconColor : Color    = AppTheme.colors.iconSelected,
    defaultIconColor  : Color    = AppTheme.colors.headerText,
    circleColor       : Color    = AppTheme.colors.headerBg,
    hasAnimation      : Boolean  = true
) {
    val density      = LocalDensity.current
    val totalHeight  = rememberBottomNavBarHeight()
    val designHeight = NAV_BAR_DESIGN_HEIGHT.dp

    val barTopFrac   = NAV_BAR_TOP_FRACTION
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
            .height(totalHeight)
            .graphicsLayer { renderEffect = null }
    ) {
        val w  = with(density) { maxWidth.toPx() }
        val h  = with(density) { designHeight.toPx() }
        val hh = with(density) { totalHeight.toPx() }

        val selectedIndex = models.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        val targetX       = w * (selectedIndex + 0.5f) / models.size

        val animX by animateFloatAsState(
            targetValue   = targetX,
            animationSpec = if (hasAnimation) spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessLow
            ) else snap(),
            label = "meowX"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val bTop  = h * barTopFrac
            val cY    = h * circleCYFrac
            val cR    = h * circleRFrac
            val nBot  = h * notchBotFrac
            val nHalf = w * notchHWFrac
            val cRad  = w * cornerRFrac
            val cx    = animX

            if (isNone) {
                val barPath = Path().apply {
                    moveTo(0f, hh)
                    lineTo(0f, bTop + cRad)
                    quadraticBezierTo(0f, bTop, cRad, bTop)
                    lineTo(w - cRad, bTop)
                    quadraticBezierTo(w, bTop, w, bTop + cRad)
                    lineTo(w, hh)
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
                    moveTo(0f, hh)
                    lineTo(0f, bTop + cRad)
                    quadraticBezierTo(0f, bTop, cRad, bTop)
                    lineTo(lx, bTop)
                    cubicTo(cx - nHalf * 0.66f, bTop, cx - nHalf * 0.40f, nBot, cx, nBot)
                    cubicTo(cx + nHalf * 0.40f, nBot, cx + nHalf * 0.66f, bTop, rx, bTop)
                    lineTo(w - cRad, bTop)
                    quadraticBezierTo(w, bTop, w, bTop + cRad)
                    lineTo(w, hh)
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

        // ── Icons in bar — positions unchanged, still based on h (108dp) ─────
        models.forEachIndexed { index, model ->
            val isSelected = model.id == selectedId
            val iconCXDp   = with(density) { (w * (index + 0.5f) / models.size).toDp() }
            val iconBarYDp = with(density) { (h * iconBarYFrac).toDp() }

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
                val bubbleYDp  = with(density) { (h * iconPopYFrac).toDp() }

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