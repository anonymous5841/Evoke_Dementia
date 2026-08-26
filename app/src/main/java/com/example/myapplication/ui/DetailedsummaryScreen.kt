package com.example.myapplication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MargarineFont
import com.example.myapplication.ui.theme.MartelFont
import androidx.compose.ui.res.stringResource


fun Modifier.offsetShadow(
    color: Color = Color.Black.copy(alpha = 0.19f),
    blurRadius: Dp = 17.dp,
    offsetX: Dp = (-14).dp,
    offsetY: Dp = 14.dp,
    shape: Shape = RoundedCornerShape(
        topStart = 14.dp,
        topEnd = 0.dp,
        bottomStart = 30.dp,
        bottomEnd = 0.dp
    )
): Modifier = this.drawBehind {

    drawIntoCanvas { canvas ->

        val paint = Paint()

        val frameworkPaint = paint.asFrameworkPaint()

        frameworkPaint.color =
            android.graphics.Color.TRANSPARENT

        frameworkPaint.setShadowLayer(
            blurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            color.toArgb()
        )

        val outline =
            shape.createOutline(
                size,
                layoutDirection,
                this
            )

        canvas.drawOutline(
            outline,
            paint
        )
    }
}


@Composable
fun DetailedSummaryScreen(
    onCloseClick: () -> Unit = {},
) {
    val appColors = AppTheme.colors

    val summaryPoints = listOf(
        stringResource(R.string.discussion_summary_text)
    )

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(15.dp),
            contentAlignment = Alignment.Center
        ) {

            /*
             * ====================================================
             * RESPONSIVE WIDTH
             * ====================================================
             */

            val screenWidth = maxWidth

            /*
             * Keep the popup width proportional to the screen.
             */
            val cardWidth = (screenWidth * 0.92f)
                .coerceIn(290.dp, 552.dp)

            /*
             * Keep original card padding.
             */
            val cardPadding = (screenWidth * 0.05f)
                .coerceIn(20.dp, 28.dp)

            /*
             * Content width inside popup.
             */
            val contentWidth = cardWidth * 0.90f


            /*
             * ====================================================
             * CLOSE BUTTON
             * ====================================================
             *
             * IMPORTANT:
             *
             * These values control ONLY the close button.
             * The heading does not affect them.
             */

            val closeShapeWidth = (screenWidth * 0.20f)
                .coerceIn(70.dp, 90.dp)

            val closeShapeHeight = (screenWidth * 0.18f)
                .coerceIn(64.dp, 82.dp)

            val closeShapeSize = (screenWidth * 0.185f)
                .coerceIn(68.dp, 78.dp)

            val closeShapeOffsetX = (screenWidth * 0.023f)
                .coerceIn(9.dp, 14.dp)

            val closeShapeOffsetY = (screenWidth * 0.074f)
                .coerceIn(29.dp, 42.dp)

            val closeTextSize = (screenWidth.value * 0.097f)
                .coerceIn(38f, 46f)
                .sp

            val closeTextOffsetX = -(screenWidth * 0.044f)
                .coerceIn(17.dp, 26.dp)

            val closeTextOffsetY = (screenWidth * 0.098f)
                .coerceIn(38.dp, 55.dp)


            /*
             * ====================================================
             * HEADING SIZE
             * ====================================================
             */

            val titleSize = (screenWidth.value * 0.059f)
                .coerceIn(23f, 28f)
                .sp


            /*
             * ====================================================
             * HEADING AVAILABLE WIDTH
             * ====================================================
             *
             * At small widths, reserve space on the right for
             * the close button.
             *
             * At 356dp and above there is enough room for the
             * complete heading on one line.
             */

            val headingAvailableWidth =
                if (screenWidth < 356.dp) {
                    contentWidth - 75.dp
                } else {
                    contentWidth
                }


            /*
             * ====================================================
             * OTHER RESPONSIVE VALUES
             * ====================================================
             */

            val bulletSize = (screenWidth.value * 0.041f)
                .coerceIn(16f, 20f)
                .sp

            val summaryTextSize = (screenWidth.value * 0.054f)
                .coerceIn(21f, 26f)
                .sp

            val bulletTextSpacing = (screenWidth * 0.015f)
                .coerceIn(6.dp, 10.dp)

            val pointBottomSpacing = (screenWidth * 0.02f)
                .coerceIn(8.dp, 12.dp)


            /*
             * ====================================================
             * MAIN CONTAINER
             * ====================================================
             */

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp),
                contentAlignment = Alignment.Center
            ) {


                /*
                 * =================================================
                 * POPUP CARD
                 * =================================================
                 */

                Box(
                    modifier = Modifier
                        .width(cardWidth)
                        .fillMaxHeight(0.895f)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = false,
                            ambientColor = Color.DarkGray,
                            spotColor = Color.DarkGray
                        )
                        .clip(
                            RoundedCornerShape(20.dp)
                        )
                        .background(
                            appColors.pagesText
                        )
                        .padding(cardPadding)
                        .align(Alignment.Center)
                ) {

                    Column(
                        modifier = Modifier
                            .width(contentWidth)
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {

                        /*
                         * =========================================
                         * TITLE
                         * =========================================
                         *
                         * The title gets only the horizontal space
                         * that is actually available.
                         *
                         * At 320–355dp:
                         *     Detailed
                         *     Summary
                         *
                         * At 356–600dp:
                         *     Detailed Summary
                         */

                        Text(
                            text = stringResource(
                                R.string.discussion_summary
                            ),
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MartelFont,
                            color = appColors.popupText,
                            modifier = Modifier
                                .width(headingAvailableWidth)
                        )


                        /*
                         * TITLE → SUMMARY GAP
                         *
                         * Same as original.
                         */

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )


                        /*
                         * =========================================
                         * SUMMARY
                         * =========================================
                         */

                        summaryPoints.forEach { point ->

                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(
                                    bottom = pointBottomSpacing
                                )
                            ) {

                                Text(
                                    text = stringResource(
                                        R.string.bullet
                                    ),
                                    fontSize = bulletSize,
                                    fontFamily = MartelFont,
                                    color = Color.White,
                                    modifier = Modifier.padding(
                                        end = bulletTextSpacing
                                    )
                                )

                                Text(
                                    text = point,
                                    fontSize = summaryTextSize,
                                    fontFamily = MartelFont,
                                    color = appColors.textfield,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }


                /*
                 * =================================================
                 * YELLOW CLOSE SHAPE
                 * =================================================
                 *
                 * This is completely independent from the title.
                 *
                 * Therefore changing the title width/line count
                 * will NOT move the button.
                 */

                Image(
                    painter = painterResource(
                        id = R.drawable.add_shape
                    ),
                    contentDescription = stringResource(
                        R.string.close
                    ),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(
                        color = appColors.popupText,
                        blendMode = BlendMode.SrcIn
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = closeShapeOffsetX,
                            y = closeShapeOffsetY
                        )
                        .size(
                            height = closeShapeHeight,
                            width = closeShapeWidth
                        )
                        .offsetShadow(
                            offsetX = (-14).dp,
                            offsetY = 14.dp
                        )
                        .size(closeShapeSize)
                        .clip(
                            RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 0.dp,
                                bottomStart = 30.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .clickable {
                            onCloseClick()
                        }
                )


                /*
                 * =================================================
                 * X TEXT
                 * =================================================
                 */

                Text(
                    text = stringResource(
                        R.string.close_text
                    ),
                    color = appColors.pagesText,
                    fontSize = closeTextSize,
                    fontFamily = MargarineFont,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(
                            x = closeTextOffsetX,
                            y = closeTextOffsetY
                        )
                        .clickable {
                            onCloseClick()
                        }
                )
            }
        }
    }

}


@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun DetailedSummaryScreenPreview() {

    GreenTheme {

        DetailedSummaryScreen()
    }
}