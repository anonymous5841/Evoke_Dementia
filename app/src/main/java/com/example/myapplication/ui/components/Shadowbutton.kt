package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    color: Color,
    cornerRadius: Dp = 30.dp,
    shadowColor: Color = Color.Black,
    shadowAlpha: Int = 90,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit
) {

    BoxWithConstraints(
        modifier = modifier
    ) {

        /*
         * ============================================================
         * RESPONSIVE DIMENSIONS
         * ============================================================
         */

        /*
         * Height
         *
         * The supplied height is treated as the design/reference
         * value and adjusted slightly according to available width.
         */
        val responsiveHeight =
            if (height == 51.dp) {
                (maxWidth * 0.13f)
                    .coerceIn(48.dp, 55.dp)
            } else if (height == 56.dp) {
                (maxWidth * 0.14f)
                    .coerceIn(52.dp, 60.dp)
            } else {
                height
            }


        /*
         * Width
         *
         * If a width was supplied, preserve its intended purpose
         * but make common button sizes responsive.
         *
         * If width == null, the button remains full width.
         */
        val responsiveWidth =
            when (width) {

                72.dp -> {
                    (maxWidth * 0.19f)
                        .coerceIn(64.dp, 78.dp)
                }

                66.dp -> {
                    (maxWidth * 0.175f)
                        .coerceIn(60.dp, 72.dp)
                }

                else -> width
            }


        /*
         * Corner radius
         */

        val responsiveCornerRadius =
            if (cornerRadius == 15.dp) {

                (maxWidth * 0.040f)
                    .coerceIn(13.dp, 17.dp)

            } else if (cornerRadius == 28.dp) {

                (responsiveHeight * 0.50f)
                    .coerceIn(26.dp, 30.dp)

            } else {

                cornerRadius
            }


        /*
         * ============================================================
         * SIZE MODIFIER
         * ============================================================
         */

        val sizeModifier =
            if (responsiveWidth != null) {

                Modifier
                    .width(responsiveWidth)
                    .height(responsiveHeight)

            } else {

                Modifier
                    .fillMaxWidth()
                    .height(responsiveHeight)
            }


        /*
         * ============================================================
         * BUTTON
         * ============================================================
         */

        Box(
            modifier = sizeModifier
                .drawBehind {

                    drawIntoCanvas { canvas ->

                        val paint = Paint()

                        val frameworkPaint =
                            paint.asFrameworkPaint()

                        frameworkPaint.isAntiAlias = true

                        frameworkPaint.color =
                            android.graphics.Color.TRANSPARENT

                        frameworkPaint.setShadowLayer(
                            12f,
                            -7f,
                            18f,

                            android.graphics.Color.argb(
                                shadowAlpha,

                                (shadowColor.red * 255)
                                    .toInt(),

                                (shadowColor.green * 255)
                                    .toInt(),

                                (shadowColor.blue * 255)
                                    .toInt()
                            )
                        )

                        canvas.drawRoundRect(

                            left = 0f,

                            top = 0f,

                            right = size.width,

                            bottom = size.height,

                            radiusX =
                                responsiveCornerRadius.toPx(),

                            radiusY =
                                responsiveCornerRadius.toPx(),

                            paint = paint
                        )
                    }
                }

                .clip(
                    RoundedCornerShape(
                        responsiveCornerRadius
                    )
                )

                .background(color)

                .clickable(
                    interactionSource =
                        remember {
                            MutableInteractionSource()
                        },

                    indication =
                        ripple(
                            bounded = true
                        )
                ) {
                    onClick()
                },

            contentAlignment =
                Alignment.Center
        ) {

            content()
        }
    }
}