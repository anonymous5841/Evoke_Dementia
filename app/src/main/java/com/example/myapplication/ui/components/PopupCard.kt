package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.ui.offsetShadow
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.theme.MargarineFont
import com.example.myapplication.ui.components.ShadowButton
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity

@Composable
fun PopupCard(
    messageText: String,
    buttonText: String = "Record",
    height: Float = 0.3f,
    upperPadding: Dp = 20.dp,
    showButton: Boolean = false,
    shapeOffset: DpOffset = DpOffset(
        x = 8.dp,
        y = (-19).dp
    ),
    navController: NavController,
    onDismiss: () -> Unit = {},
    onButtonClick: () -> Unit = {},
) {

    val appColors = AppTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.6f)
            ),
        contentAlignment = Alignment.Center
    ) {

        val widthScale = (
                maxWidth.value / 390f
                ).coerceIn(
                320f / 390f,
                600f / 390f
            )

        val popupWidth =
            maxWidth * 0.93f

        val requestedPopupHeight =
            maxHeight * height

        val minimumButtonPopupHeight =
            220.dp

        val minimumNoButtonPopupHeight =
            150.dp

        val minimumPopupHeight =
            if (showButton) {
                minimumButtonPopupHeight
            } else {
                minimumNoButtonPopupHeight
            }

        val popupHeight =
            requestedPopupHeight.coerceAtLeast(
                minimumPopupHeight
            )

        val popupPadding =
            (24f * widthScale)
                .coerceIn(
                    20f,
                    30f
                )
                .dp

        val popupTopPadding =
            (upperPadding.value * widthScale)
                .coerceAtLeast(
                    upperPadding.value
                )
                .dp

        val messageTextSize =
            (33f * widthScale)
                .coerceIn(
                    27f,
                    33f
                )
                .sp

        val messageWidth =
            (
                    popupWidth -
                            (popupPadding * 2f) -
                            (35.dp * widthScale)
                    ).coerceAtLeast(
                    0.dp
                )

        val buttonHeightScale =
            (maxHeight.value / 851f)
                .coerceIn(
                    0.72f,
                    1f
                )

        val buttonTextSize =
            (27f * widthScale * buttonHeightScale)
                .coerceIn(
                    20f,
                    42f
                )
                .sp

        val buttonTextWidthPx =
            textMeasurer.measure(
                text = buttonText,
                style = TextStyle(
                    fontSize = buttonTextSize,
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Medium
                )
            ).size.width

        val buttonTextWidthDp =
            with(density) {
                buttonTextWidthPx.toDp()
            }

        val buttonHorizontalPadding =
            (48f * widthScale)
                .dp

        val buttonWidth =
            (buttonTextWidthDp + buttonHorizontalPadding)
                .coerceAtLeast(
                    (160f * widthScale).dp
                )
                .coerceAtMost(
                    messageWidth
                )

        /*
         * ============================================================
         * BUTTON HEIGHT — [FIX] now accounts for actual wrapped text
         * ============================================================
         *
         * buttonWidth can be squeezed below buttonTextWidthDp by the
         * coerceAtMost(messageWidth) cap above — on narrow cards with
         * longer strings (seen with the Urdu "Erase Data" button) the
         * text then wraps onto two lines inside ShadowButton. The old
         * buttonHeight formula had no idea this could happen — it was
         * purely screen-size driven — so it stayed sized for one line
         * and the second line got clipped.
         *
         * This re-measures buttonText constrained to the width it will
         * ACTUALLY render at (buttonWidth minus horizontal padding),
         * which tells us the real number of lines/height needed. If
         * it wraps, buttonHeight grows to fit; if it doesn't, nothing
         * changes from before.
         */

        val buttonTextConstrainedWidthPx =
            with(density) {
                (buttonWidth - buttonHorizontalPadding)
                    .toPx()
                    .toInt()
                    .coerceAtLeast(0)
            }

        val buttonTextHeightPx =
            textMeasurer.measure(
                text = buttonText,
                style = TextStyle(
                    fontSize = buttonTextSize,
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Medium
                ),
                constraints = Constraints(
                    maxWidth = buttonTextConstrainedWidthPx
                )
            ).size.height

        val buttonTextHeightDp =
            with(density) {
                buttonTextHeightPx.toDp()
            }

        val buttonVerticalPadding =
            (16f * widthScale)
                .dp

        val buttonHeight =
            (60f * widthScale * buttonHeightScale)
                .coerceIn(
                    46f,
                    60f
                )
                .dp
                .coerceAtLeast(
                    buttonTextHeightDp + buttonVerticalPadding   // [FIX] grows to fit wrapped text
                )

        val buttonSpacing =
            (34f * widthScale * buttonHeightScale)
                .coerceIn(
                    16f,
                    34f
                )
                .dp

        val buttonCorner =
            (56f * widthScale)
                .coerceIn(
                    48f,
                    56f
                )
                .dp
        val closeShapeWidth =
            (78f * widthScale)
                .coerceIn(
                    64f,
                    120f
                )
                .dp

        val closeShapeHeight =
            (70f * widthScale)
                .coerceIn(
                    60f,
                    108f
                )
                .dp

        val closeXSize =
            (38f * widthScale)
                .coerceIn(
                    32f,
                    58f
                )
                .sp

        val responsiveShapeOffset =
            DpOffset(
                x = shapeOffset.x * widthScale,
                y = shapeOffset.y * widthScale
            )

        /*
         * popupHeight is now the MINIMUM height. The card grows when its
         * content needs more room (e.g. the 2-line Urdu "erase data"
         * message + button). With a fixed height the Column gave the
         * button only the leftover space, so the button got squashed.
         * Short popups look exactly as before (content centred).
         */
        Box(
            modifier = Modifier
                .width(popupWidth)
                .align(Alignment.Center)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = popupHeight)
                    .clip(
                        RoundedCornerShape(20.dp)
                    )
                    .background(
                        appColors.pagesText
                    )
                    .padding(
                        popupPadding
                    ),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = popupTopPadding
                        ),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(
                        text = messageText,

                        color =
                            appColors.popupText,

                        fontSize =
                            messageTextSize,

                        fontWeight =
                            FontWeight.Normal,

                        fontFamily =
                            OutfitFont,

                        textAlign =
                            TextAlign.Center,

                        modifier =
                            Modifier.width(
                                messageWidth
                            ),

                        style =
                            TextStyle(
                                textDirection =
                                    TextDirection.Content,

                                shadow =
                                    Shadow(
                                        color =
                                            Color.Gray,

                                        offset =
                                            Offset(
                                                x = -2f,
                                                y = 8f
                                            ),

                                        blurRadius = 18f
                                    )
                            )
                    )

                    if (showButton) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    buttonSpacing
                                )
                        )

                        ShadowButton(
                            width =
                                buttonWidth,

                            height =
                                buttonHeight,

                            color =
                                appColors.popupText,

                            cornerRadius =
                                buttonCorner,

                            onClick = {
                                onButtonClick()
                            }
                        ) {

                            Text(
                                text =
                                    buttonText,

                                color =
                                    appColors.pagesText,

                                fontSize =
                                    buttonTextSize,

                                fontWeight =
                                    FontWeight.Medium,

                                fontFamily =
                                    OutfitFont,

                                textAlign =
                                    TextAlign.Center,

                                modifier =
                                    Modifier.padding(
                                        horizontal = buttonHorizontalPadding / 2
                                    )
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = responsiveShapeOffset.x,
                        y = responsiveShapeOffset.y
                    )
                    .size(
                        width = closeShapeWidth,
                        height = closeShapeHeight
                    )
                    .offsetShadow(
                        offsetX = (-14).dp,
                        offsetY = 14.dp
                    ),
                contentAlignment = Alignment.Center
            ) {

                Image(
                    painter =
                        painterResource(
                            id = R.drawable.add_shape
                        ),

                    contentDescription =
                        "Close",

                    contentScale =
                        ContentScale.Crop,

                    colorFilter =
                        ColorFilter.tint(
                            color =
                                appColors.popupText,

                            blendMode =
                                BlendMode.SrcIn
                        ),

                    modifier = Modifier
                        .fillMaxSize()
                        .clip(
                            RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 0.dp,
                                bottomStart = 30.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .clickable {
                            onDismiss()
                        }
                )

                Text(
                    text = "X",

                    color =
                        appColors.pagesText,

                    fontSize =
                        closeXSize,

                    fontFamily =
                        MargarineFont,

                    fontWeight =
                        FontWeight.Bold,

                    modifier = Modifier
                        .clickable {
                            onDismiss()
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
fun PopupCardNoButtonPreview() {

    val navController =
        rememberNavController()

    GreenTheme {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(alpha = 0.5f)
                )
        ) {

            PopupCard(
                messageText =
                    "Record Deleted Successfully!",

                height =
                    0.30f,

                upperPadding =
                    10.dp,

                shapeOffset =
                    DpOffset(
                        x = 8.dp,
                        y = (-19).dp
                    ),

                showButton =
                    true,

                navController =
                    navController,

                onDismiss = {}
            )
        }
    }
}