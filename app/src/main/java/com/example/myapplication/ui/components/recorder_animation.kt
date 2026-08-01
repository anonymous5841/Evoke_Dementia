package com.example.animation

import android.os.Build
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.AppTheme
import kotlin.math.min
import kotlin.math.sin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.theme.BlueTheme
import com.example.myapplication.ui.theme.GreenTheme
import kotlin.math.PI
import kotlin.math.cos
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color


/**
 * Animated voice recorder component.
 *
 * ROTATION: driven by exact keyframes from Figma (see previous revision's
 * comments for the full unwrap math) - unchanged in this update.
 *
 * COLOR: also keyframed now, matching Figma's per-state stroke gradients.
 * Two color "looks" alternate across the 6-state loop:
 *
 *   State-A colors (states 1, 3, 4, 6 - the "resting" look):
 *     Ring A gradient:      #A6FF94 (stop 0%)   -> #F9FFA6 (stop 100%)
 *     Rings B/C/D gradient: #56F794 (stop 0%)   -> #B0E420 (stop 100%)
 *
 *   State-B colors (states 2, 5 - the "energized" look):
 *     Ring A gradient:      #34FF22 (stop 34%)  -> #F2F20B (stop 100%)
 *     Rings B/C/D gradient: #3CFF0B (stop 46%)  -> #F7FF01 (stop 100%)
 *
 * The "stop %" is the gradient's start-color position along the stroke
 * (not a timing value) - State-B's gradient starts its top color further
 * along the ellipse before transitioning, which reads as a punchier/more
 * saturated look. That start-stop position is animated as its own Float
 * (stopA / stopBCD below) alongside the two endpoint colors, all sharing
 * the same 11000ms keyframe timeline as rotation:
 *
 *   t=0     (state1) -> State-A
 *   t=1000  (state2) -> State-B
 *   t=3000  (state3) -> State-A
 *   t=5000  (state4) -> State-A
 *   t=7000  (state5) -> State-B
 *   t=9000  (state6) -> State-A
 *   t=11000 (loop)   -> State-A (matches state1 exactly, seamless restart)
 *
 * Position wobble, opacity pulsing, ring sizes, and the Ring A drop-shadow
 * color (#FF9838) are unchanged - only the ellipse stroke gradients
 * (colors + start-stop position) are newly keyframed.
 */
@Composable
fun VoiceRecorderAnimation(
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voiceAnimation")
    val appColors = AppTheme.colors
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    val theta = time * (2f * Math.PI.toFloat())

    // ---- Rotation A (Ellipse1 / Ring A) - keyframed, net +720 deg per loop ----
    val rotationA by infiniteTransition.animateFloat(
        initialValue = 8.4f,
        targetValue = 728.4f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                8.4f at 0
                86.04f at 1000 using LinearEasing
                218.4f at 3000 using LinearEasing
                368.4f at 5000 using LinearEasing
                446.04f at 7000 using LinearEasing
                578.4f at 9000 using LinearEasing
                728.4f at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationA"
    )

    // ---- Rotation B (Ellipse2 / Ring B) - keyframed, net +720 deg per loop ----
    val rotationB by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                0f at 0
                82.55f at 1000 using LinearEasing
                210.0f at 3000 using LinearEasing
                360.0f at 5000 using LinearEasing
                442.55f at 7000 using LinearEasing
                570.0f at 9000 using LinearEasing
                720.0f at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationB"
    )

    // ---- Rotation C (Ellipse3 / Ring C) - keyframed, net +360 deg per loop ----
    val rotationC by infiniteTransition.animateFloat(
        initialValue = 14.9f,
        targetValue = 374.9f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                14.9f at 0
                117.67f at 1000 using LinearEasing
                252.62f at 3000 using LinearEasing
                312.02f at 5000 using LinearEasing
                481.22f at 7000 using LinearEasing
                534.95f at 9000 using LinearEasing
                374.9f at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationC"
    )

    // ---- Rotation D (Ellipse4 / Ring D) - keyframed, net 0 deg (returns to start) ----
    val rotationD by infiniteTransition.animateFloat(
        initialValue = 77.22f,
        targetValue = 77.22f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                77.22f at 0
                159.86f at 1000 using LinearEasing
                292.14f at 3000 using LinearEasing
                266.1f at 5000 using LinearEasing
                157.24f at 7000 using LinearEasing
                140.86f at 9000 using LinearEasing
                77.22f at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationD"
    )

    // ==== COLOR KEYFRAMES ====
    // State-A = #A6FF94 -> #F9FFA6, stop 0%. State-B = #34FF22 -> #F2F20B, stop 34%.
    val colorA1 by infiniteTransition.animateColor(
        initialValue = appColors.voiceS1E1T,
        targetValue = appColors.voiceS1E1T,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                appColors.voiceS1E1T at 0
                appColors.voiceS1E2T at 1000 using LinearEasing
                appColors.voiceS1E1T at 3000 using LinearEasing
                appColors.voiceS1E1T at 5000 using LinearEasing
                appColors.voiceS1E2T at 7000 using LinearEasing
                appColors.voiceS1E1T at 9000 using LinearEasing
                appColors.voiceS1E1T at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "colorA1"
    )
    val colorA2 by infiniteTransition.animateColor(
        initialValue = appColors.voiceS1E1B,
        targetValue = appColors.voiceS1E1B,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                appColors.voiceS1E1B at 0
                appColors.voiceS2E1B at 1000 using LinearEasing
                appColors.voiceS1E1B at 3000 using LinearEasing
                appColors.voiceS1E1B at 5000 using LinearEasing
                appColors.voiceS2E1B at 7000 using LinearEasing
                appColors.voiceS1E1B at 9000 using LinearEasing
                appColors.voiceS1E1B at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "colorA2"
    )
    val stopA by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                0f at 0
                0.34f at 1000 using LinearEasing
                0f at 3000 using LinearEasing
                0f at 5000 using LinearEasing
                0.34f at 7000 using LinearEasing
                0f at 9000 using LinearEasing
                0f at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "stopA"
    )

    // Rings B/C/D share the same color channel. State-A = #56F794 -> #B0E420, stop 0%.
    // State-B = #3CFF0B -> #F7FF01, stop 46%.
    val colorBCD1 by infiniteTransition.animateColor(
        initialValue = appColors.voiceS1E2T,
        targetValue = appColors.voiceS1E2T,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                appColors.voiceS1E2T at 0
                appColors.voiceS2E2T at 1000 using LinearEasing
                appColors.voiceS1E2T at 3000 using LinearEasing
                appColors.voiceS1E2T at 5000 using LinearEasing
                appColors.voiceS2E2T at 7000 using LinearEasing
                appColors.voiceS1E2T at 9000 using LinearEasing
                appColors.voiceS1E2T at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "colorBCD1"
    )
    val colorBCD2 by infiniteTransition.animateColor(
        initialValue = appColors.voiceS1E2B,
        targetValue = appColors.voiceS1E2B,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                appColors.voiceS1E2B at 0
                appColors.voiceS2E2B at 1000 using LinearEasing
                appColors.voiceS1E2B at 3000 using LinearEasing
                appColors.voiceS1E2B at 5000 using LinearEasing
                appColors.voiceS2E2B at 7000 using LinearEasing
                appColors.voiceS1E2B at 9000 using LinearEasing
                appColors.voiceS1E2B at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "colorBCD2"
    )
    val stopBCD by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 11000
                0f at 0
                0.46f at 1000 using LinearEasing
                0f at 3000 using LinearEasing
                0f at 5000 using LinearEasing
                0.46f at 7000 using LinearEasing
                0f at 9000 using LinearEasing
                0f at 11000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "stopBCD"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isAnimating) {

            // ---- Ring A (Ellipse1): rx=113.44 ry=92.74 - shadow + ring together ----
            RingAWithShadow(
                theta = theta,
                rotation = rotationA,
                colorTop = colorA1,
                colorBottom = colorA2,
                colorStop = stopA
            )

            // ---- Ring D (Ellipse4): rx=111.56 ry=93.98 (+7dp) ----
            Canvas(modifier = Modifier.fillMaxSize().blurIfSupported(6.dp)) {
                val scale = fitScale(size)
                val sizeBump = 7.dp.toPx()
                val cx = baseCenterX(size, scale, 206.07f) + sin(theta + 3.6f) * 6f * scale
                val cy = baseCenterY(size, scale, 152.29f)
                drawRing(
                    centerX = cx, centerY = cy,
                    radiusX = 111.56f * scale + sizeBump, radiusY = 93.98f * scale + sizeBump,
                    rotation = rotationD,
                    opacity = (0.55f + 0.25f * sin(theta + 3.0f)).coerceIn(0.15f, 0.9f),
                    baseStart = colorBCD1, baseEnd = colorBCD2, baseStartStop = stopBCD,
                    overlayStart = Color(0xFF05F80D).copy(alpha = 0.33f),
                    overlayEnd = Color.White.copy(alpha = 0.15f),
                    strokeWidth = 10f * scale
                )
            }

            // ---- Ring C (Ellipse3): rx=111.56 ry=93.98 (+7dp) ----
            Canvas(modifier = Modifier.fillMaxSize().blurIfSupported(6.dp)) {
                val scale = fitScale(size)
                val sizeBump = 7.dp.toPx()
                val cx = baseCenterX(size, scale, 209.20f) + sin(theta + 2.4f) * 5f * scale
                val cy = baseCenterY(size, scale, 152.46f)
                drawRing(
                    centerX = cx, centerY = cy,
                    radiusX = 111.56f * scale + sizeBump, radiusY = 93.98f * scale + sizeBump,
                    rotation = rotationC,
                    opacity = (0.6f + 0.25f * sin(theta + 2.0f)).coerceIn(0.15f, 0.95f),
                    baseStart = colorBCD1, baseEnd = colorBCD2, baseStartStop = stopBCD,
                    overlayStart = Color(0xFF05F80D).copy(alpha = 0.33f),
                    overlayEnd = Color.White.copy(alpha = 0.15f),
                    strokeWidth = 10f * scale
                )
            }

            // ---- Ring B (Ellipse2): rx=111.56 ry=93.98 (+7dp) ----
            Canvas(modifier = Modifier.fillMaxSize().blurIfSupported(6.dp)) {
                val scale = fitScale(size)
                val sizeBump = 7.dp.toPx()
                val cx = baseCenterX(size, scale, 197.61f) + sin(theta + 1.2f) * 5f * scale
                val cy = baseCenterY(size, scale, 152.04f)
                drawRing(
                    centerX = cx, centerY = cy,
                    radiusX = 111.56f * scale + sizeBump, radiusY = 93.98f * scale + sizeBump,
                    rotation = rotationB,
                    opacity = (0.75f + 0.2f * sin(theta + 1.0f)).coerceIn(0.2f, 1f),
                    baseStart = colorBCD1, baseEnd = colorBCD2, baseStartStop = stopBCD,
                    overlayStart = Color(0xFF05F80D).copy(alpha = 0.33f),
                    overlayEnd = Color.White.copy(alpha = 0.15f),
                    strokeWidth = 10f * scale
                )
            }

        } else {
            // Static resting frame - matches Ring B's fitted geometry, State-A colors
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scale = fitScale(size)
                drawAnimatedEllipse(
                    centerX = baseCenterX(size, scale, 197.61f),
                    centerY = baseCenterY(size, scale, 152.04f),
                    radiusX = 111.56f * scale, radiusY = 93.98f * scale,
                    rotation = 0f,
                    opacity = 1f,
                    primaryColor = appColors.voiceS1E2T,
                    secondaryColor = appColors.voiceS1E2B,
                    startStop = 0f,
                    strokeWidth = 10f * scale
                )
            }
        }
    }
}

/**
 * Ring A (Ellipse1) drawn as ONE unit: shadow + sharp ring share the exact
 * same center/rotation, computed once, so they can never drift apart.
 * `rotation`, `colorTop`, `colorBottom`, and `colorStop` all come in as
 * parameters (keyframed values from the caller) instead of being computed
 * here from a formula.
 */
@Composable
private fun RingAWithShadow(
    theta: Float,
    rotation: Float,
    colorTop: Color,
    colorBottom: Color,
    colorStop: Float
) {
    val appColors = AppTheme.colors
    val designCenterX = 198.33f
    val designCenterY = 153.46f
    val designRadiusX = 113.44f
    val designRadiusY = 92.74f
    val ringOpacity = (0.9f + 0.1f * sin(theta)).coerceIn(0.5f, 1f)
    val shadowOpacity = (0.5f + 0.15f * sin(theta)).coerceIn(0.2f, 0.7f)

    // Drop shadow - orange (#FF9838), unaffected by the ring's color changes,
    // blurred, drawn first so it sits behind the ring, rotates WITH it.
    Canvas(modifier = Modifier.fillMaxSize().blurIfSupported(8.dp)) {
        val scale = fitScale(size)
        val sizeBump = 7.dp.toPx()
        val shadowExtra = 7.dp.toPx()
        val cx = baseCenterX(size, scale, designCenterX) + sin(theta) * 2.5f * scale
        val cy = baseCenterY(size, scale, designCenterY)
        val shadowRadiusX = designRadiusX * scale + sizeBump + shadowExtra
        val shadowRadiusY = designRadiusY * scale + sizeBump + shadowExtra
        rotate(degrees = rotation, pivot = Offset(cx, cy)) {
            drawOval(
                color = appColors.Voiceshadow.copy(alpha = shadowOpacity),
                topLeft = Offset(cx - shadowRadiusX, cy - shadowRadiusY),
                size = Size(shadowRadiusX * 2, shadowRadiusY * 2),
                style = Stroke(width = 4f * scale)
            )
        }
    }

    // The ring itself - unblurred, drawn last so it stays crisp on top of its own shadow.
    Canvas(modifier = Modifier.fillMaxSize()) {
        val scale = fitScale(size)
        val sizeBump = 7.dp.toPx()
        val cx = baseCenterX(size, scale, designCenterX) + sin(theta) * 2.5f * scale
        val cy = baseCenterY(size, scale, designCenterY)
        drawAnimatedEllipse(
            centerX = cx, centerY = cy,
            radiusX = designRadiusX * scale + sizeBump,
            radiusY = designRadiusY * scale + sizeBump,
            rotation = rotation,
            opacity = ringOpacity,
            primaryColor = colorTop,
            secondaryColor = colorBottom,
            startStop = colorStop,
            strokeWidth = 4f * scale
        )
    }
}

/** Reference design dimensions from the SVG viewBox (400x300), center (200,150). */
private const val BASELINE_WIDTH = 400f
private const val BASELINE_HEIGHT = 300f
private const val BASELINE_CENTER_X = 200f
private const val BASELINE_CENTER_Y = 150f

/**
 * Uniform (non-distorting) scale factor: fit the 400x300 design inside
 * whatever container size is given, preserving the ellipses' true aspect
 * ratio instead of stretching them to fill a non-4:3 box.
 */
private const val ANIMATION_SCALE = 1.15f

private fun fitScale(size: Size): Float =
    min(size.width / BASELINE_WIDTH, size.height / BASELINE_HEIGHT) * ANIMATION_SCALE

/** Maps a design-space X coordinate (0-400) to actual canvas pixels, centered. */
private fun baseCenterX(size: Size, scale: Float, designX: Float): Float =
    size.width / 2f + (designX - BASELINE_CENTER_X) * scale

/** Maps a design-space Y coordinate (0-300) to actual canvas pixels, centered. */
private fun baseCenterY(size: Size, scale: Float, designY: Float): Float =
    size.height / 2f + (designY - BASELINE_CENTER_Y) * scale

/**
 * Applies Modifier.blur() only on API 31+ (RenderEffect requirement).
 * On older devices this is a no-op, so the ellipse renders sharp instead of crashing.
 */
private fun Modifier.blurIfSupported(radius: Dp): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this.blur(radius = radius)
    } else {
        this
    }
}

/**
 * Draws a single-gradient stroked ellipse (used for Ring A, the thin sharp ring).
 * `startStop` places the top color's gradient stop (0f = start of stroke,
 * matching Figma's "stop %" value) - the bottom color is always pinned at 1f (100%).
 */
private fun DrawScope.drawAnimatedEllipse(
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
    rotation: Float,
    opacity: Float,
    primaryColor: Color,
    secondaryColor: Color,
    startStop: Float,
    strokeWidth: Float
) {
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            startStop to primaryColor.copy(alpha = opacity),
            1f to secondaryColor.copy(alpha = opacity * 0.7f)
        ),
        start = Offset(centerX - radiusX, centerY - radiusY),
        end = Offset(centerX + radiusX, centerY + radiusY)
    )

    rotate(degrees = rotation, pivot = Offset(centerX, centerY)) {
        drawOval(
            brush = brush,
            topLeft = Offset(centerX - radiusX, centerY - radiusY),
            size = Size(radiusX * 2, radiusY * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

/**
 * Draws a "glassy ring" the way the SVG does it for Rings B/C/D: a base
 * color-gradient stroke (now with an animatable `baseStartStop` gradient
 * position, matching Figma's per-state stop %), then a second translucent
 * white/green highlight stroke drawn directly on top at the same rotation.
 * The overlay's gradient position is unaffected by the state color change
 * (not specified in the Figma color data), so it keeps its original
 * fixed-offset positioning.
 */
private fun DrawScope.drawRing(
    centerX: Float,
    centerY: Float,
    radiusX: Float,
    radiusY: Float,
    rotation: Float,
    opacity: Float,
    baseStart: Color,
    baseEnd: Color,
    baseStartStop: Float,
    overlayStart: Color,
    overlayEnd: Color,
    strokeWidth: Float
) {
    val topLeft = Offset(centerX - radiusX, centerY - radiusY)
    val ovalSize = Size(radiusX * 2, radiusY * 2)

    val baseBrush = Brush.linearGradient(
        colorStops = arrayOf(
            baseStartStop to baseStart.copy(alpha = opacity),
            1f to baseEnd.copy(alpha = opacity)
        ),
        start = topLeft,
        end = Offset(centerX + radiusX, centerY + radiusY)
    )
    val overlayBrush = Brush.linearGradient(
        colors = listOf(
            overlayStart.copy(alpha = overlayStart.alpha * opacity),
            overlayEnd.copy(alpha = overlayEnd.alpha * opacity)
        ),
        start = Offset(centerX - radiusX * 0.6f, centerY + radiusY),
        end = Offset(centerX + radiusX, centerY - radiusY * 0.4f)
    )

    rotate(degrees = rotation, pivot = Offset(centerX, centerY)) {
        drawOval(brush = baseBrush, topLeft = topLeft, size = ovalSize, style = Stroke(width = strokeWidth))
        drawOval(brush = overlayBrush, topLeft = topLeft, size = ovalSize, style = Stroke(width = strokeWidth))
    }
}

/**
 * Preview function to test the voice recorder animation
 */
@Composable
fun VoiceRecorderAnimationPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        VoiceRecorderAnimation(
            modifier = Modifier
                .size(400.dp, 300.dp)
        )
    }
}


/**
 * A soft, organic "liquid blob" animation built entirely with Compose Canvas
 * primitives (Path + gradients). No RenderEffect/blur, no dynamic color —
 * both require API 31+ — so this renders identically on API 21-30 and 31+.
 *
 * WHY THIS VERSION IS SMOOTH (laminar, not glitchy):
 * The previous version drove the wobble from four separate
 * `infiniteRepeatable` animations, each restarting on its own independent
 * schedule (6s / 9s / 3.2s / 24s). Individually each one loops cleanly, but
 * four independently-restarting clocks never restart in lockstep with each
 * other, so their combination stutters. On top of that, only 10 sample
 * points were used to draw noise with a frequency up to 5 — right at the
 * Nyquist edge — so as the phases drifted apart the sampled outline would
 * occasionally under-resolve and pinch/cross itself for a frame.
 *
 * The fix: everything here derives from ONE continuously-increasing time
 * value read straight off the frame clock (`withFrameNanos`). It never
 * restarts, so there's nothing to desync. Point count is also raised so the
 * curve always has enough resolution to stay smooth at every phase
 * combination.
 *
 * Colors are pulled from [AppTheme.colors], so wrapping this in [GreenTheme]
 * or [BlueTheme] automatically re-skins it.
 */
@Composable
fun LiquidBlobAnimation(
    modifier: Modifier = Modifier.size(220.dp), // only used if the caller passes no modifier
    points: Int = 24 // more samples = no pinching/aliasing between the noise waves
) {
    val colors = AppTheme.colors

    // Single free-running clock, in seconds, driven by actual frame time.
    var timeSeconds by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                timeSeconds = (now - start) / 1_000_000_000f
            }
        }
    }

    val twoPi = (2f * PI).toFloat()
    val phase1 = timeSeconds * (twoPi / 6f)          // ~6s wobble cycle
    val phase2 = timeSeconds * (twoPi / 9f)          // ~9s wobble cycle
    val rotation = (timeSeconds * (360f / 24f)) % 360f // one revolution / 24s
    val scalePulse = 1f + 0.06f * sin(timeSeconds * (twoPi / 3.2f)) // ~3.2s breathing

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val baseRadius = (min(w, h) / 2f) * 0.7f

            // Builds a smooth closed blob path: raw sampled points become
            // control points, and the curve passes through the midpoints
            // between consecutive points, so there are never sharp corners.
            fun blobPath(noise: (Float) -> Float): Path {
                val angleStep = twoPi / points
                val raw = (0 until points).map { i ->
                    val angle = angleStep * i
                    val r = baseRadius + noise(angle)
                    Offset(cx + r * cos(angle), cy + r * sin(angle))
                }
                val path = Path()
                val firstMid = Offset(
                    (raw[0].x + raw[points - 1].x) / 2f,
                    (raw[0].y + raw[points - 1].y) / 2f
                )
                path.moveTo(firstMid.x, firstMid.y)
                for (i in 0 until points) {
                    val current = raw[i]
                    val next = raw[(i + 1) % points]
                    val mid = Offset((current.x + next.x) / 2f, (current.y + next.y) / 2f)
                    path.quadraticTo(current.x, current.y, mid.x, mid.y)
                }
                path.close()
                return path
            }

            // Layered sine/cosine waves at low, well-resolved frequencies —
            // cheap smooth pseudo-noise, safe against the 24-point sampling.
            fun noiseFor(seed: Float): (Float) -> Float = { angle ->
                sin(angle * 3f + phase1 + seed) * (baseRadius * 0.10f) +
                        cos(angle * 4f - phase2 + seed) * (baseRadius * 0.06f) +
                        sin(angle * 2f + phase1 * 0.5f + seed) * (baseRadius * 0.08f)
            }

            withTransform({
                rotate(rotation, pivot = Offset(cx, cy))
                scale(scalePulse, scalePulse, pivot = Offset(cx, cy))
            }) {
                // 1) Soft shadow blob behind everything, for depth (no blur used)
                drawPath(
                    path = blobPath(noiseFor(1.7f)),
                    color = Color(0xFFFF9838).copy(alpha = 0.16f)
                )

                // 2) Main liquid body with a radial gradient fill
                drawPath(
                    path = blobPath(noiseFor(0f)),
                    brush = Brush.radialGradient(
                        colors = listOf(colors.cameraInner, colors.cameraOuter),
                        center = Offset(cx * 0.85f, cy * 0.8f),
                        radius = baseRadius * 1.6f
                    )
                )

                // 3) Inner highlight blob to sell the "liquid sheen"
                drawPath(
                    path = blobPath(noiseFor(3.4f)),
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.boxInner.copy(alpha = 0.35f),
                            colors.boxInner.copy(alpha = 0f)
                        ),
                        center = Offset(cx * 0.7f, cy * 0.65f),
                        radius = baseRadius * 1.1f
                    )
                )

                // 4) Small floating accent blob for extra life
                drawPath(
                    path = blobPath(noiseFor(5.2f)),
                    color = colors.staticwave.copy(alpha = 0.20f)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Liquid Blob - Green")
@Composable
private fun LiquidBlobGreenPreview() {
    GreenTheme {
        LiquidBlobAnimation()
    }
}

@Preview(showBackground = true, name = "Liquid Blob - Blue")
@Composable
private fun LiquidBlobBluePreview() {
    BlueTheme {
        LiquidBlobAnimation()
    }
}