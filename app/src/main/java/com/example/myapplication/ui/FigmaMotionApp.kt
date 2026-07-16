package com.example.myapplication.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.withSave
import com.example.myapplication.ui.theme.GreenTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import android.os.Build
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.example.myapplication.R
import com.example.myapplication.ui.theme.BaumansFont
import com.example.myapplication.ui.theme.PompiereFont
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues

private enum class Screen { Loading3, Loading4 }

// ─── bottom shape paths ────────────────────────────────────────────────────────
private fun buildMainGreenPath(w: Float, h: Float): Path {
    val sx = w / 412f; val sy = h / 433f
    return Path().apply {
        moveTo(0f, 159f * sy)
        cubicTo(2.065f*sx, 116.659f*sy, 37.048f*sx, 82.964f*sy, 79.901f*sx, 82.964f*sy)
        lineTo(332.902f*sx, 82.964f*sy)
        cubicTo(376.068f*sx, 82.964f*sy, 411.250f*sx, 48.776f*sy, 412.902f*sx, 6.000f*sy)
        lineTo(412.902f*sx, h); lineTo(0f, h); close()
    }
}
private fun buildRightBorderPath(w: Float, h: Float): Path {
    val sx = w/412f; val sy = h/433f
    return Path().apply {
        moveTo(412.978f*sx, 0f)
        cubicTo(411.988f*sx,43.323f*sy, 376.561f*sx,78.139f*sy, 333f*sx,78.139f*sy)
        lineTo(146.685f*sx,78.139f*sy); lineTo(146.685f*sx,84.466f*sy); lineTo(333f*sx,84.466f*sy)
        cubicTo(377.183f*sx,84.466f*sy, 413f*sx,48.649f*sy, 413f*sx,4.466f*sy)
        lineTo(413f*sx,0f); close()
    }
}
private fun buildLeftBorderPath(w: Float, h: Float): Path {
    val sx = w/412f; val sy = h/433f
    return Path().apply {
        moveTo(0.022f*sx,162.466f*sy)
        cubicTo(1.012f*sx,119.143f*sy, 36.439f*sx,84.327f*sy, 80f*sx,84.327f*sy)
        lineTo(147.954f*sx,84.327f*sy); lineTo(147.954f*sx,78f*sy); lineTo(80f*sx,78f*sy)
        cubicTo(35.817f*sx,78f*sy, 0f,113.817f*sy, 0f,158f*sy)
        lineTo(0f,162.466f*sy); close()
    }
}
private fun buildRightShadowPath(w: Float, h: Float): Path {
    val sx = w/412f; val sy = h/433f
    return Path().apply {
        moveTo(412.978f*sx,7f*sy)
        cubicTo(411.988f*sx,50.323f*sy, 376.561f*sx,85.139f*sy, 333f*sx,85.139f*sy)
        lineTo(146.685f*sx,85.139f*sy); lineTo(146.685f*sx,91.466f*sy); lineTo(333f*sx,91.466f*sy)
        cubicTo(377.183f*sx,91.466f*sy, 413f*sx,55.649f*sy, 413f*sx,11.466f*sy)
        lineTo(413f*sx,7f*sy); close()
    }
}
private fun buildLeftShadowPath(w: Float, h: Float): Path {
    val sx = w/412f; val sy = h/433f
    return Path().apply {
        moveTo(0.022f*sx,169.466f*sy)
        cubicTo(1.012f*sx,126.143f*sy, 36.439f*sx,91.327f*sy, 80f*sx,91.327f*sy)
        lineTo(147.954f*sx,91.327f*sy); lineTo(147.954f*sx,85f*sy); lineTo(80f*sx,85f*sy)
        cubicTo(35.817f*sx,85f*sy, 0f,120.817f*sy, 0f,165f*sy)
        lineTo(0f,169.466f*sy); close()
    }
}
private fun DrawScope.drawBottomXml() {
    val w = size.width; val h = size.height
    drawPath(buildMainGreenPath(w,h),   Color(0xFF3E634F))
    drawPath(buildRightBorderPath(w,h), Color(0xFF95A79D))
    drawPath(buildLeftBorderPath(w,h),  Color(0xFF95A79D))
    drawPath(buildRightShadowPath(w,h), Color(0x10000000))
    drawPath(buildLeftShadowPath(w,h),  Color(0x10000000))
}

private const val TEXT_SHADOW_BLUR_X   = 18f
private const val TEXT_SHADOW_BLUR_Y   = 18f
private const val TEXT_SHADOW_ALPHA    = 0.55f
private const val TEXT_SHADOW_OFFSET_Y = 2

// ─── Main composable ──────────────────────────────────────────────────────────
@Composable
fun FigmaMotionApp(
    onAddComplete: () -> Unit = {}   // ← add this
) {
    var current by remember { mutableStateOf(Screen.Loading3) }

    val bubbleReveal  = remember { Animatable(0f) }
    val imageReveal   = remember { Animatable(0f) }
    val contentReveal = remember { Animatable(0f) }
    val riseContent   = remember { Animatable(0f) }

    // slideUp: 0f = language block at rest, 1f = fully slid off screen upward
    val slideUp = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(2200)
        current = Screen.Loading4
        bubbleReveal.snapTo(0f); imageReveal.snapTo(0f)
        contentReveal.snapTo(0f); riseContent.snapTo(0f)
        slideUp.snapTo(0f)

        kotlinx.coroutines.coroutineScope {
            // Bubbles: same pace as before but no pauses — smooth continuous flow
            launch {
                bubbleReveal.animateTo(0.60f, tween(3500, easing = LinearEasing))
                bubbleReveal.animateTo(0.75f, tween(1500, easing = LinearEasing))
                bubbleReveal.animateTo(1f,    tween(1200, easing = FastOutSlowInEasing))
            }
            // Content: starts 2 seconds earlier than before (was after all bubble phases + delays)
            launch {
                delay(5000)  // ← was ~5800ms before (3500 + 800 + 1500 + 400 delays removed)
                imageReveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            }
            launch {
                delay(5000)
                contentReveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            }
        }

        riseContent.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenHeight = maxHeight

        when (current) {
            Screen.Loading3 -> Box(Modifier.fillMaxSize().background(Color(0xFF3E634F)))

            Screen.Loading4 -> {
                val p          = bubbleReveal.value.coerceIn(0f, 1f)
                val centerRise = (-120f * riseContent.value).roundToInt()

                Box(Modifier.fillMaxSize()) {

                    // ── Layer 1 (bottom): Profile screen — always rendered ─────
                    // Revealed as language block slides up
                    // In FigmaMotionApp, add a reverse slideUp state
                    GreenTheme {
                        PersonFormContent(
                            mode   = PersonFormMode.ADD,
                            onAdd  = { _, _, _, _ ->
                                onAddComplete()           // ← fires when Add button clicked
                            },
                            onBack = {
                                // reverse the slide — animate slideUp back to 0
                                scope.launch {
                                    slideUp.animateTo(
                                        0f,
                                        tween(600, easing = FastOutSlowInEasing)
                                    )
                                    // go back to Loading4 language screen
                                    current = Screen.Loading4
                                }
                            }
                        )
                    }

                    // ── Layer 2: Language block slides upward on Continue ──────
                    // Offset goes from 0 → -screenHeight as slideUp 0 → 1
                    // When slideUp = 0 it sits at bottom covering profile
                    // When slideUp = 1 it has moved fully off screen upward
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = (-screenHeight * slideUp.value))
                            .pointerInput(slideUp.value) {
                                if (slideUp.value < 0.95f) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitPointerEvent(PointerEventPass.Initial)
                                        }
                                    }
                                }
                            }
                    ) {
                        // White background + bubbles (fades out as slide progresses)
                        val contentAlpha = (1f - slideUp.value * 2f).coerceIn(0f, 1f)

                        Box(Modifier.fillMaxSize().background(Color(0xFF3E634F)))

                        Box(Modifier.fillMaxSize().alpha(contentAlpha)) {
                            WhiteBubbleFill(progress = p)

                            val whiteFillP = ((p - 0.72f) / 0.18f).coerceIn(0f, 1f)
                            if (whiteFillP > 0f) {
                                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = whiteFillP)))
                            }

                            val contentTop    = (screenHeight * 0.25f) + centerRise.dp
                            val headingTop    = (screenHeight * 0.54f) + centerRise.dp
                            val subheadingTop = (screenHeight * 0.63f) + centerRise.dp

                            Icon(
                                painter            = painterResource(id = R.drawable.main_image),
                                contentDescription = null,
                                tint               = Color.Unspecified,
                                modifier           = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = contentTop)
                                    .size(280.dp, 240.dp)
                                    .alpha(imageReveal.value)
                                    .scale(0.92f + 0.08f * imageReveal.value),
                            )

                            // "Evoke" shadow
                            Text(
                                "Evoke",
                                color = Color.White,
                                style = TextStyle(fontSize = 60.sp),
                                fontFamily = BaumansFont,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(
                                        x = ((1f - contentReveal.value) * -220f).dp,
                                        y = headingTop + TEXT_SHADOW_OFFSET_Y.dp
                                    )
                                    .alpha(contentReveal.value * TEXT_SHADOW_ALPHA)
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                        renderEffect = BlurEffect(
                                            radiusX       = TEXT_SHADOW_BLUR_X,
                                            radiusY       = TEXT_SHADOW_BLUR_Y,
                                            edgeTreatment = TileMode.Decal
                                        )
                                    },
                            )
                            // "Evoke" real text
                            Text(
                                "Evoke",
                                color = Color(0xFF3E634F),
                                style = TextStyle(fontSize = 60.sp),
                                fontFamily = BaumansFont,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(x = ((1f - contentReveal.value) * -220f).dp, y = headingTop)
                                    .alpha(contentReveal.value),
                            )

                            Text(
                                "helps you remember",
                                color = Color(0xFFFFCD38),
                                style = TextStyle(fontSize = 37.sp),
                                fontFamily =  PompiereFont,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(x = ((1f - contentReveal.value) * -260f).dp, y = subheadingTop)
                                    .alpha(contentReveal.value),
                            )
                        }

                        // Language block — always slides with the parent Box
                        BottomLanguageBlockWithShadow(
                            titleReveal = riseContent.value,
                            modifier    = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (140f * (1f - riseContent.value)).dp)
                                .alpha(riseContent.value),
                            onContinue  = {
                                // Slide the whole layer upward — profile revealed underneath
                                slideUp.animateTo(
                                    1f,
                                    tween(600, easing = FastOutSlowInEasing)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ─── Bubble wave ──────────────────────────────────────────────────────────────
@Composable
private fun WhiteBubbleFill(progress: Float) {
    Box(Modifier.fillMaxSize()) {
// First wave — corners and edges, appear earliest
        TransitionBubble(x=-160f, y=-160f, size=360f, progress=progress, stagger=0.00f, blur=0f)
        TransitionBubble(x= 420f, y=-140f, size=340f, progress=progress, stagger=0.02f, blur=0f)
        TransitionBubble(x=  80f, y=-240f, size=420f, progress=progress, stagger=0.02f, blur=0f) // ← moved here
        TransitionBubble(x= 480f, y=760f,  size=400f, progress=progress, stagger=0.04f, blur=0f)
        TransitionBubble(x=-220f, y=750f,  size=320f, progress=progress, stagger=0.04f, blur=0f)


        // Second wave — spread inward
        TransitionBubble(x= 300f, y=  0f,  size=400f, progress=progress, stagger=0.08f, blur=0f)
        TransitionBubble(x= 550f, y=100f,  size=250f, progress=progress, stagger=0.10f, blur=0f)
        TransitionBubble(x=-160f, y=340f,  size=360f, progress=progress, stagger=0.12f, blur=6f)
        TransitionBubble(x= 530f, y=880f,  size=260f, progress=progress, stagger=0.12f, blur=0f)

        // Third wave — middle fill
        TransitionBubble(x= 100f, y=400f,  size=350f, progress=progress, stagger=0.18f, blur=0f)
        TransitionBubble(x=-100f, y=500f,  size=350f, progress=progress, stagger=0.20f, blur=0f)
        TransitionBubble(x= 420f, y=300f,  size=360f, progress=progress, stagger=0.20f, blur=6f)
        TransitionBubble(x= 480f, y=400f,  size=320f, progress=progress, stagger=0.22f, blur=0f)
        TransitionBubble(x= 500f, y=200f,  size=300f, progress=progress, stagger=0.24f, blur=0f)

        // Fourth wave — center closers
        TransitionBubble(x= 520f, y=500f,  size=280f, progress=progress, stagger=0.30f, blur=0f)
        TransitionBubble(x= 450f, y=350f,  size=240f, progress=progress, stagger=0.32f, blur=0f)
        TransitionBubble(x= 550f, y=600f,  size=300f, progress=progress, stagger=0.34f, blur=0f)
        TransitionBubble(x= 560f, y=720f,  size=240f, progress=progress, stagger=0.36f, blur=0f)
        TransitionBubble(x= 510f, y=820f,  size=220f, progress=progress, stagger=0.38f, blur=0f)
        TransitionBubble(x= 600f, y=900f,  size=280f, progress=progress, stagger=0.40f, blur=0f)

        // Final fill — last gaps
        TransitionBubble(x= 500f, y=740f,  size=180f, progress=progress, stagger=0.46f, blur=0f)
        TransitionBubble(x= 460f, y=660f,  size=260f, progress=progress, stagger=0.48f, blur=0f)
        TransitionBubble(x= 120f, y=760f,  size=420f, progress=progress, stagger=0.56f, blur=0f)
    }
}

@Composable
private fun BoxScope.TransitionBubble(
    x: Float, y: Float, size: Float,
    progress: Float, stagger: Float, blur: Float,
) {
    val rawP = ((progress - stagger) / (1f - stagger)).coerceIn(0f, 1f)
    if (rawP <= 0f) return
    val p           = FastOutSlowInEasing.transform(rawP)
    val growth      = 0.1f + p * 1.6f
    val currentSize = (size * growth).dp
    val currentBlur = (blur * (1f - p)).dp
    Box(
        Modifier
            .offset(x.dp, y.dp)
            .size(currentSize)
            .background(Color.White.copy(alpha = 0.94f), CircleShape)
            .then(if (blur > 0f) Modifier.blur(currentBlur) else Modifier),
    )
}

// ─── Bottom language block ────────────────────────────────────────────────────
@Composable
private fun BottomLanguageBlockWithShadow(
    titleReveal : Float,
    modifier    : Modifier = Modifier,
    onContinue  : suspend () -> Unit = {}
) {
    val fullTitle    = "Language"
    val visibleChars = (fullTitle.length * titleReveal).roundToInt().coerceIn(0, fullTitle.length)
    val titleText    = fullTitle.take(visibleChars)
    val scope        = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.50f)
    ) {
        // Black blurred shadow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ — BlurEffect version
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .offset(y = (-16).dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        renderEffect = BlurEffect(
                            radiusX       = 10f,
                            radiusY       = 50f,
                            edgeTreatment = TileMode.Decal
                        )
                        alpha = 0.65f
                    }
                    .drawBehind {
                        val w = size.width; val h = size.height
                        drawPath(buildMainGreenPath(w, h), Color.Black)
                    }
            )
        } else {
            // API < 31 — spread layers version
            val cachedBottomPath = remember { mutableListOf<Path>() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .offset(y = (-16).dp)
                    .drawBehind {
                        val w = size.width
                        val h = size.height

                        val bottomPath: Path
                        if (cachedBottomPath.isEmpty()) {
                            val newPath = buildMainGreenPath(w, h)
                            cachedBottomPath.add(newPath)
                            bottomPath = newPath
                        } else {
                            bottomPath = cachedBottomPath[0]
                        }

                        val spreadLayers = listOf(
                            Pair(-8f,  0.04f),  // closest to shape — highest alpha
                            Pair(-14f, 0.020f),
                            Pair(-20f, 0.015f),
                            Pair(-28f, 0.010f),
                            Pair(-36f, 0.007f),
                            Pair(-44f, 0.004f),
                            Pair(-52f, 0.002f),  // furthest from shape — lowest alpha
                        )

                        spreadLayers.forEach { (yOff, alpha) ->
                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply {
                                    color = Color.Black.copy(alpha = alpha)
                                    isAntiAlias = false  // ← add this
                                }
                                canvas.withSave {
                                    canvas.translate(0f, yOff)
                                    canvas.drawPath(bottomPath, paint)
                                }
                            }
                        }
                    }
            )
        }

        // Real shape
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .drawBehind { drawBottomXml() }
        ) {
            val curveHeightDp = (159f / 433f * maxHeight.value).dp
            val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            Box(

            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top    = curveHeightDp - 10.dp,
                    start  = 44.dp,
                    end    = 24.dp,
                    bottom = (navBarPadding - 10.dp).coerceAtLeast(0.dp),
                ),
            ) {
                // "Language" with shadow
                Box {
                    Text(
                        titleText,
                        color = Color.White,
                        style = TextStyle(fontSize = 48.sp),
                        fontFamily = BaumansFont,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(y = 6.dp)
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                                renderEffect = BlurEffect(
                                    radiusX       = 15f,
                                    radiusY       = 15f,
                                    edgeTreatment = TileMode.Decal
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { alpha = 0.4f}
                                else{alpha = 0.3f}
                            }
                    )
                    Text(
                        titleText,
                        color = Color.White,
                        style = TextStyle(fontSize = 48.sp),
                        fontFamily = BaumansFont,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }

                val selectedLanguage = remember { mutableStateOf("English") }
                val selectorOffset   = animateDpAsState(
                    if (selectedLanguage.value == "English") 0.dp else 160.dp,
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 100.dp)
                        .width(320.dp)
                        .height(58.dp)
                        .background(Color.White, RoundedCornerShape(50))
                        .padding(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = selectorOffset.value)
                            .size(width = 150.dp, height = 50.dp)
                            .background(Color(0xFF3E634F), RoundedCornerShape(50)),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 50.dp)
                                .clickable { selectedLanguage.value = "English" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "English",
                                color = if (selectedLanguage.value == "English") Color(0xFFFFCD38) else Color(0xFF3E634F),
                                style = TextStyle(fontSize = 28.sp),
                                fontFamily = BaumansFont
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(width = 120.dp, height = 50.dp)
                                .clickable { selectedLanguage.value = "Urdu" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Urdu",
                                color = if (selectedLanguage.value == "Urdu") Color(0xFFFFCD38) else Color(0xFF3E634F),
                                style = TextStyle(fontSize = 28.sp),
                                fontFamily = BaumansFont
                            )
                        }
                    }
                }

                // Continue row
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 8.dp)
                        .clickable { scope.launch { onContinue() } },
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Continue...",
                        color = Color(0xFFFFCD38),
                        style = TextStyle(fontSize = 30.sp),
                        fontFamily = PompiereFont
                    )
                    Icon(
                        painter            = painterResource(id = R.drawable.forward_icon),
                        contentDescription = "forward icon",
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}