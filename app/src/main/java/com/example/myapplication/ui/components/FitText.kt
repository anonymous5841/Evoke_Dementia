package com.example.myapplication.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Text that uses [maxFontSize] when it fits, and shrinks (down to [minFontSize])
 * when it doesn't — e.g. long Urdu labels inside buttons on small screens.
 *
 * The space it may use comes from its parent (button width, card height, …).
 */
@Composable
fun FitText(
    text: String,
    maxFontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minFontSize: TextUnit = 10.sp,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    lineHeight: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        autoSize = fitAutoSize(minFontSize, maxFontSize),
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        lineHeight = lineHeight,
        maxLines = maxLines,
        style = style,
    )
}

/**
 * TextAutoSize that never crashes: StepBased requires min < max, so if the
 * given max is tiny the min is lowered automatically.
 */
fun fitAutoSize(minFontSize: TextUnit, maxFontSize: TextUnit): TextAutoSize {
    val max = maxFontSize.value.coerceAtLeast(2f)
    val min = minFontSize.value.coerceAtMost(max - 1f).coerceAtLeast(1f)
    return TextAutoSize.StepBased(
        minFontSize = min.sp,
        maxFontSize = max.sp,
        stepSize = 0.5.sp
    )
}
