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
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.text.style.TextDirection
@Composable
fun PopupCard(
messageText: String,
buttonText: String = "Record",
height: Float = 0.3f,
upperPadding: Dp = 20.dp,
showButton: Boolean = false,

textOffset: DpOffset = DpOffset(
x = (-34).dp,
y = 293.dp
),

shapeOffset: DpOffset = DpOffset(
x = 8.dp,
y = (-24).dp
),

navController: NavController,

    onDismiss: () -> Unit = {},
    onButtonClick: () -> Unit = {},
) {

    val appColors = AppTheme.colors

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
        /*
 * ============================================================
 * RESPONSIVE POPUP HEIGHT
 * ============================================================
 *
 * Keep the existing height fraction when there is enough
 * vertical space.
 *
 * On short screens, increase the popup only enough to keep
 * the button and message visible.
 *
 * Width responsiveness and ellipse behavior are unchanged.
 */

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

        /*
 * ============================================================
 * RESPONSIVE BUTTON
 * ============================================================
 *
 * 390dp = original button design
 * Below 390dp = shrink only as much as needed
 * 390dp and above = keep original size
 *
 * Button width is also constrained by the actual
 * available popup content width.
 */


        /*
 * ============================================================
 * RESPONSIVE BUTTON ONLY
 * ============================================================
 *
 * Width behavior stays the same as before.
 *
 * Height behavior additionally adapts to short screens so
 * the button does not disappear when the popup becomes short.
 *
 * The rest of PopupCard is unchanged.
 */

// Keep the existing width responsiveness
        val buttonWidth =
            (200f * widthScale)
                .coerceIn(
                    160f,
                    200f
                )
                .dp
                .coerceAtMost(
                    messageWidth
                )

        /*
         * Short-screen scale for the BUTTON ONLY.
         *
         * 851dp -> 1.0
         * 651dp -> about 0.76
         *
         * This does NOT change the popup or ellipse.
         */
        val buttonHeightScale =
            (maxHeight.value / 851f)
                .coerceIn(
                    0.72f,
                    1f
                )

        val buttonHeight =
            (52f * widthScale * buttonHeightScale)
                .coerceIn(
                    40f,
                    52f
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
            (50f * widthScale)
                .coerceIn(
                    44f,
                    50f
                )
                .dp

        val buttonTextSize =
            (27f * widthScale * buttonHeightScale)
                .coerceIn(
                    20f,
                    27f
                )
                .sp

        val closeShapeWidth =
            (78f * widthScale)
                .coerceIn(
                    64f,
                    78f
                )
                .dp

        val closeShapeHeight =
            (70f * widthScale)
                .coerceIn(
                    60f,
                    70f
                )
                .dp

        val closeXSize =
            (38f * widthScale)
                .coerceIn(
                    32f,
                    38f
                )
                .sp

        val responsiveShapeOffset =
            DpOffset(
                x = shapeOffset.x * widthScale,
                y = shapeOffset.y * widthScale
            )

        Box(
            modifier = Modifier
                .width(popupWidth)
                .height(popupHeight)
                .align(Alignment.Center)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(
                        RoundedCornerShape(20.dp)
                    )
                    .background(
                        appColors.pagesText
                    )
                    .padding(
                        popupPadding
                    )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
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
                                    TextDirection.Content,   // [FIX] resolves bidi direction from the actual string content (handles Urdu + trailing "!" correctly), instead of only the ambient layout direction
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
                                    OutfitFont
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
                    0.25f,

                upperPadding =
                    10.dp,

                shapeOffset =
                    DpOffset(
                        x = 8.dp,
                        y = (-24).dp
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