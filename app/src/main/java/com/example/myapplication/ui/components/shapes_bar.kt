package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.offsetShadow
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import com.example.myapplication.ui.theme.MartelFont
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.shadow
import com.example.myapplication.ui.theme.OutfitFont
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.shape.CircleShape
import com.example.myapplication.ui.theme.BlueAppColors
import com.example.myapplication.ui.theme.GreenAppColors

@Composable
fun SearchFieldWithIcon(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,

    // Box sizing — pass null for boxWidth to fillMaxWidth instead of a fixed width
    boxWidth: Dp? = 280.dp,
    boxHeight: Dp = 54.dp,

    // Text field
    placeholder: String = "Search by name",
    placeholderColor: Color = Color(0xFF555555),
    fontSize: TextUnit = 20.sp,
    cornerRadius: Dp = 15.dp,
    textFieldYOffset: Dp = 3.dp,

    // Green blob icon + its shadow
    greenIconSize: DpSize = DpSize(width = 80.dp, height = 119.dp),
    greenIconOffset: DpOffset = DpOffset(x = (-12).dp, y = (-1).dp),
    shadowColor: Color = Color.Black.copy(alpha = 0.25f),
    shadowBlurRadius: Dp = 12.dp,
    shadowOffset: DpOffset = DpOffset(x = (-6).dp, y = 8.dp),
    shadowScaleX: Float = 1f,
    shadowScaleY: Float = 0.65f,

    // Yellow search icon (drawn on top)
    searchIconSize: Dp = 28.dp,
    searchIconOffset: DpOffset = DpOffset(x = 13.dp, y = (-9).dp),
    searchIconEndPadding: Dp = 50.dp,
) {
    val boxSizeModifier = if (boxWidth != null) {
        Modifier.width(boxWidth).height(boxHeight)
    } else {
        Modifier.fillMaxWidth().height(boxHeight)
    }

    val appColors = AppTheme.colors

    fun icGreenShadowShape(
        scaleX: Float = 1f,
        scaleY: Float = 0.8f
    ): Shape = GenericShape { size, _ ->
        val sx = (size.width * scaleX) / 72f
        val sy = (size.height * scaleY) / 57f
        val offsetX = (size.width - 72f * sx) / 2f
        val offsetY = (size.height - 57f * sy) / 2f

        fun tx(x: Float) = x * sx + offsetX
        fun ty(y: Float) = y * sy + offsetY

        moveTo(tx(68f), ty(15.488f))
        cubicTo(
            tx(68f), ty(32.34f),
            tx(54.569f), ty(46f),
            tx(38f), ty(46f)
        )
        cubicTo(
            tx(21.431f), ty(46f),
            tx(8f), ty(32.34f),
            tx(8f), ty(15.488f)
        )
        cubicTo(
            tx(8f), ty(-1.363f),
            tx(8f), ty(1.13f),
            tx(38f), ty(1.13f)
        )
        cubicTo(
            tx(68f), ty(1.13f),
            tx(68f), ty(-1.363f),
            tx(68f), ty(15.488f)
        )
        close()
    }

    Box(modifier = modifier.then(boxSizeModifier)) {

        ShadowTextField(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = textFieldYOffset),
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            height = boxHeight,
            cornerRadius = cornerRadius,
            fontSize = fontSize,
            placeholderColor = placeholderColor
        )

        // Bottom drawable — green blob + its traced shadow
        Icon(
            painter = painterResource(R.drawable.ic_green),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = greenIconOffset.x, y = greenIconOffset.y)
                .requiredSize(height = greenIconSize.height, width = greenIconSize.width)
                .offsetShadow(
                    color = shadowColor,
                    blurRadius = shadowBlurRadius,
                    offsetX = shadowOffset.x,
                    offsetY = shadowOffset.y,
                    shape = icGreenShadowShape(scaleX = shadowScaleX, scaleY = shadowScaleY)
                ),
            tint = appColors.pagesText
        )

        // Top drawable — yellow search icon, placed over the green blob
        Icon(
            painter = painterResource(R.drawable.ic_yellowsearch),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = searchIconEndPadding)
                .offset(x = searchIconOffset.x, y = searchIconOffset.y)
                .size(searchIconSize),
            tint = appColors.popupText
        )
    }
}


@Composable
fun AddShapeButton(
    onClick: () -> Unit,
    shapeColor: Color,
    plusColor: Color,
    modifier: Modifier = Modifier,

    // Outer positioning box
    boxSize: DpSize = DpSize(80.dp, 80.dp),
    boxOffset: DpOffset = DpOffset(x = (279).dp, y = (-39).dp),

    // First size in the chain — this is what the shadow's outline is computed against
    shapeShadowWidth: Dp = 58.dp,
    shapeShadowHeight: Dp = 50.dp,
    shapeOffset: DpOffset = DpOffset(x = 9.dp, y = 29.dp),

    // Shadow beneath the shape
    shadowColor: Color = Color.Black.copy(alpha = 0.19f),
    shadowBlurRadius: Dp = 17.dp,
    shadowOffsetX: Dp = (-8).dp,
    shadowOffsetY: Dp = 8.dp,

    // Second size in the chain — this is the actual final rendered/clipped size
    shapeVisualSize: Dp = 72.dp,

    // "+" text overlay
    plusText: String = "+",
    plusFontSize: TextUnit = 53.sp,
    plusOffset: DpOffset = DpOffset(x = (278).dp, y = (-26).dp),
) {
    Box(modifier = modifier) {

        Box(
            modifier = Modifier
                .size(width = boxSize.width, height = boxSize.height)
                .align(Alignment.TopEnd)
                .offset(x = boxOffset.x, y = boxOffset.y)
        ) {
            Image(
                painter = painterResource(id = R.drawable.add_shape),
                contentDescription = "Close",
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(
                    color = shapeColor,
                    blendMode = BlendMode.SrcIn
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = shapeOffset.x, y = shapeOffset.y)
                    .size(height = shapeShadowHeight, width = shapeShadowWidth)
                    .offsetShadow(
                        color = shadowColor,
                        blurRadius = shadowBlurRadius,
                        offsetX = shadowOffsetX,
                        offsetY = shadowOffsetY
                    )
                    .size(shapeVisualSize)   // ← restored — this is the real final size, distinct from the shadow-computation size above
                    .clip(
                        RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 0.dp,
                            bottomStart = 30.dp,
                            bottomEnd = 0.dp
                        )
                    )
                    .clickable { onClick() }
            )
        }

        Text(
            text = plusText,
            color = plusColor,
            fontSize = plusFontSize,
            fontFamily = MartelFont,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = plusOffset.x, y = plusOffset.y)
                .clickable { onClick() }
        )
    }
}



// ══════════════════════════════════════════════════════
// WAVEFORM HEIGHTS — top level of file
// FIX: was inside Row causing 'private' modifier error and scope errors
// ══════════════════════════════════════════════════════
private val waveformHeights = listOf(
    6f, 12f, 18f, 8f, 22f, 14f, 26f, 10f, 20f, 16f,
    28f, 6f, 18f, 12f, 24f, 8f, 20f, 14f, 10f, 22f,
    6f, 16f, 26f, 12f, 18f, 8f, 24f, 10f, 20f, 12f, 18f
)

// ══════════════════════════════════════════════════════
// WAVEFORM ANIMATED — top level of file
// FIX: was defined inside Row composable causing all Unresolved reference errors
// [ANIMATION SPEED] change 380f
// [BAR WIDTH] change 3.5.dp
// [BAR COLOR] change Color(0xFF3E634F)
// ══════════════════════════════════════════════════════
@Composable
fun WaveformAnimated(
    modifier: Modifier = Modifier,
    speedMultiplier: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val baseDuration = (380f / speedMultiplier).toInt()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp), //changed space
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveformHeights.forEachIndexed { index, baseHeight ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = baseHeight,
                targetValue = baseHeight * 1.75f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = baseDuration,
                        delayMillis = index * 22
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.colors.pagesText)
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// WAVEFORM STATIC — top level of file
// FIX: was defined inside Row causing Unresolved reference errors
// [BAR COLOR] change Color(0xFF8FB89A)
// ══════════════════════════════════════════════════════
@Composable
fun WaveformStatic(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveformHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AppTheme.colors.staticwave)
            )
        }
    }
}

@Composable
fun VoicePlayerBar(
    isPlaying: Boolean,
    speedMultiplier: Float,
    onPlayPauseClick: () -> Unit,
    onSpeedClick: () -> Unit,
    width: Dp = 320.dp,
    height: Dp = 57.dp,
    backgroundColor: Color = AppTheme.colors.boxInner,
    playColor: Color = AppTheme.colors.afterPlayColor,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp)
            .padding(start = 7.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Play / Pause Button
            IconButton(
                onClick = { onPlayPauseClick() },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.play_icon
                        else R.drawable.pause_icon
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) playColor else Color.Black,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))

            // Waveform — fills remaining space between button and speed text
            if (isPlaying) {
                WaveformAnimated(
                    modifier = Modifier.weight(1f),
                    speedMultiplier = speedMultiplier
                )
            } else {
                WaveformStatic(modifier = Modifier.weight(1f).padding(start=2.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Speed text — right corner of recording box
            Text(
                text = when (speedMultiplier) {
                    1.5f -> "x1.5"
                    2f -> "x2"
                    else -> "x1"
                },
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFont,
                modifier = Modifier
                    .width(34.dp)
                    .clickable { onSpeedClick() }
            )
        }
    }
}


@Composable
fun ThemeToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    trackWidth: Dp = 70.dp,
    trackHeight: Dp = 35.dp,
    thumbSize: Dp = 28.dp,          // ← fixed, never animates — this is your "stays large" guarantee
    thumbPadding: Dp = 4.dp,
    checkedThumbColor: Color =  BlueAppColors.toggleColor,
    uncheckedThumbColor: Color = GreenAppColors.toggleColor,
    trackColor: Color = AppTheme.colors.textfield
) {
    val thumbColor by animateColorAsState(
        targetValue = if (checked) checkedThumbColor else uncheckedThumbColor,
        label = "thumbColor"
    )

    val thumbOffsetX by animateDpAsState(
        targetValue = if (checked) (trackWidth - thumbSize - thumbPadding) else thumbPadding,
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetX)
                .size(thumbSize)                 // ← constant — no scale animation ever touches this
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}