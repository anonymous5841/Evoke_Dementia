package com.example.myapplication.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    /*
     * ============================================================
     * [ONE-SHOT ACTION GUARD] — fix for "unresponsive" popups
     * ============================================================
     *
     * Every popup action (X or button) calls popBackStack(), often
     * two of them in a row. A second tap (double-click on the
     * emulator, or a re-tap because the Urdu text made the popup
     * slow to appear) used to run those pops AGAIN — closing extra
     * screens or leaving a duplicate popup behind that looked like
     * the button "did nothing".
     *
     * Now only the FIRST action of this popup runs; any later tap
     * is ignored while the popup is closing.
     */
    var actionTaken by remember { mutableStateOf(false) }

    val runOnce: (() -> Unit) -> Unit = { action ->
        if (!actionTaken) {
            actionTaken = true
            action()
        }
    }

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

        val buttonTextStyle =
            TextStyle(
                fontSize = buttonTextSize,
                fontFamily = OutfitFont,
                fontWeight = FontWeight.Medium
            )

        /*
         * Button WIDTH still follows the label (cached measurement):
         * text width + padding, at least 160dp, at most the message
         * width.
         */
        val buttonTextWidthPx =
            remember(buttonText, buttonTextSize) {
                textMeasurer.measure(
                    text = buttonText,
                    style = buttonTextStyle
                ).size.width
            }

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
         * [BUTTON HEIGHT] — fixed, never measured from the text
         * ============================================================
         *
         * The height used to grow to "measured text height + 16dp".
         * Urdu is drawn with the device's own Urdu font, so that
         * measurement differed per device: on the emulator (Noto
         * Nastaliq Urdu) the label measured as 2 tall lines and the
         * button became an oval, while phones looked fine.
         *
         * Now the height is only screen-based (46–60dp, the original
         * design) and the label is a single line that shrinks to fit
         * (see [BUTTON LABEL] below). Same pill on every device and
         * in both languages.
         */
        val buttonHeight =
            (60f * widthScale * buttonHeightScale)
                .coerceIn(
                    46f,
                    60f
                )
                .dp

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
         * popupHeight is the MINIMUM height. The card grows when its
         * content needs more room (e.g. the 2-line Urdu "erase data"
         * message + button). Short popups look exactly as before.
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

                            // [EXACT HEIGHT] stop ShadowButton's 51/56dp
                            // auto-resize from ever shrinking this button
                            useExactHeight = true,

                            onClick = {
                                runOnce(onButtonClick)
                            }
                        ) {

                            /*
                             * [BUTTON LABEL] one line; keeps buttonTextSize when
                             * it fits, shrinks (down to 14sp) when it does not —
                             * e.g. long Urdu "Erase Data" on narrow phones.
                             */
                            FitText(
                                text =
                                    buttonText,

                                maxFontSize =
                                    buttonTextSize,

                                minFontSize =
                                    14.sp,

                                color =
                                    appColors.pagesText,

                                fontWeight =
                                    FontWeight.Medium,

                                fontFamily =
                                    OutfitFont,

                                textAlign =
                                    TextAlign.Center,

                                maxLines = 1,

                                modifier =
                                    Modifier.padding(
                                        horizontal = buttonHorizontalPadding / 2
                                    )
                            )
                        }
                    }
                }
            }

            /*
             * [CLOSE TAB] — one tap area for the whole yellow tab.
             * Before, only the clipped image and the small "X" text
             * were clickable (two separate targets, with dead corners).
             */
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
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        runOnce(onDismiss)
                    },
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
                        FontWeight.Bold
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