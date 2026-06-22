package com.example.myapplication.ui.components


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShadowButton(
    width        : Dp?       = null,
    height       : Dp,
    color        : Color,
    cornerRadius : Dp     = 30.dp,
    onClick      : () -> Unit = {},
    content      : @Composable () -> Unit
) {

    val sizeModifier = if (width != null)
        Modifier.width(width).height(height)
    else
        Modifier.fillMaxWidth().height(height)

    Box(
        modifier = sizeModifier
            .drawBehind {
                drawIntoCanvas { canvas ->
                    val paint          = Paint()
                    val frameworkPaint = paint.asFrameworkPaint()
                    frameworkPaint.isAntiAlias = true
                    frameworkPaint.color       = android.graphics.Color.TRANSPARENT
                    frameworkPaint.setShadowLayer(
                        12f,
                        -7f,
                        18f,
                        android.graphics.Color.argb(90, 0, 0, 0)
                    )
                    canvas.drawRoundRect(
                        left    = 0f,
                        top     = 0f,
                        right   = size.width,
                        bottom  = size.height,
                        radiusX = cornerRadius.toPx(),
                        radiusY = cornerRadius.toPx(),
                        paint   = paint
                    )
                }
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(color)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = ripple(bounded = true)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
