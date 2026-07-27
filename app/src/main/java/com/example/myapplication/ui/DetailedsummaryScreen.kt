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
        frameworkPaint.color = android.graphics.Color.TRANSPARENT
        frameworkPaint.setShadowLayer(
            blurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            color.toArgb()
        )
        val outline = shape.createOutline(size, layoutDirection, this)
        canvas.drawOutline(outline, paint)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(15.dp),
            contentAlignment = Alignment.Center
        ) {
            // ── POPUP CARD ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.895f)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                        ambientColor = Color.DarkGray,
                        spotColor = Color.DarkGray
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(appColors.pagesText)
                    .align(Alignment.Center)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.discussion_summary),
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = MartelFont,
                            color = appColors.popupText
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    summaryPoints.forEach { point ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.bullet),
                                fontSize = 16.sp,
                                fontFamily = MartelFont,
                                color = Color.White,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = point,
                                fontSize = 21.sp,
                                fontFamily = MartelFont,
                                color = appColors.textfield
                            )
                        }
                    }
                }
            }

            // ── CLOSE BUTTON — yellow shape + shadow, no extra wrapping Box ──
            Image(
                painter = painterResource(id = R.drawable.add_shape),
                contentDescription = stringResource(R.string.close),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(
                    color = appColors.popupText,
                    blendMode = BlendMode.SrcIn
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 9.dp, y = 29.dp)
                    .size(height = 70.dp, width = 78.dp)
                    .offsetShadow(
                        offsetX = (-14).dp,
                        offsetY = 14.dp
                    )
                    .size(72.dp)                     // if not already applied here
                    .clip(RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 0.dp,
                        bottomStart = 30.dp,
                        bottomEnd = 0.dp
                    ))
                    .clickable { onCloseClick() }
            )

            // ── X TEXT ──
            Text(
                text = stringResource(R.string.close_text),
                color = appColors.pagesText,
                fontSize = 38.sp,
                fontFamily = MargarineFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-17).dp, y = 38.dp)
                    .clickable { onCloseClick() }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetailedSummaryScreenPreview() {
    GreenTheme {
        DetailedSummaryScreen()
    }
}