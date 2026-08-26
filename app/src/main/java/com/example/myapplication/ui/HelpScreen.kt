package com.example.myapplication.ui

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont


@Composable
fun HelpScreen(
    onBack: () -> Unit = {},
    onSendMessage: () -> Unit = {},
    onMapToHome: () -> Unit = {}
) {

    val appColors = AppTheme.colors

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * ORIGINAL SPACING
             * ----------------
             * These values are kept the same
             * as your original HelpScreen.
             */

            val headerHeight = 260.dp

            val horizontalPadding = 22.dp

            val cardSpacing = 20.dp

            val verticalSpacing = 24.dp


            /*
             * RESPONSIVE CARD WIDTH
             * --------------------
             *
             * Available width:
             *
             * screen width
             * - left padding
             * - right padding
             * - space between cards
             *
             * The remaining width is divided
             * equally between the two cards.
             */

            val availableCardWidth =
                (
                        maxWidth
                                - (horizontalPadding * 2)
                                - cardSpacing
                        ) / 2


            /*
             * Keep the original 175.dp width
             * whenever the screen is large enough.
             *
             * On smaller screens the cards
             * automatically shrink.
             */

            val cardWidth =
                availableCardWidth.coerceAtMost(175.dp)


            /*
             * ORIGINAL CARD HEIGHT
             *
             * Height does NOT depend on screen
             * height.
             */

            val cardHeight = 125.dp


            /*
             * SCROLLABLE CONTENT
             */

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                /*
                 * ORIGINAL HEADER SPACE
                 */

                Spacer(
                    modifier = Modifier.height(
                        headerHeight
                    )
                )


                /*
                 * CONTENT CONTAINER
                 */

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = horizontalPadding
                        )
                        .offset(
                            y = (-15).dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            verticalSpacing
                        )
                ) {


                    /*
                     * CARD ROW
                     */

                    Row(
                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.Center,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {


                        /*
                         * SEND MESSAGE CARD
                         */

                        HelpCard(
                            iconRes =
                                R.drawable.message_icon,

                            title =
                                stringResource(
                                    R.string.send_message
                                ),

                            onClick =
                                onSendMessage,

                            cardWidth =
                                cardWidth,

                            cardHeight =
                                cardHeight
                        )


                        /*
                         * ORIGINAL 20dp GAP
                         */

                        Spacer(
                            modifier =
                                Modifier.width(
                                    cardSpacing
                                )
                        )


                        /*
                         * MAP TO HOME CARD
                         */

                        HelpCard(
                            iconRes =
                                R.drawable.map_to_home_icon,

                            title =
                                stringResource(
                                    R.string.map_to_home
                                ),

                            onClick =
                                onMapToHome,

                            iconWidth = 66.dp,

                            iconHeight = 56.dp,

                            cardWidth =
                                cardWidth,

                            cardHeight =
                                cardHeight
                        )
                    }
                }
            }


            /*
             * HEADER
             *
             * Your header is already responsive,
             * so nothing is changed here.
             */

            HeaderSection(
                stringResource(R.string.help),

                spacing = 74.dp,

                bottomspace = 44.dp,

                onBack = onBack
            )
        }
    }
}


/*
 * HELP CARD
 */

@Composable
fun HelpCard(
    iconRes: Int,
    title: String,
    onClick: () -> Unit,

    iconWidth: Dp = 55.dp,

    iconHeight: Dp = 47.dp,

    cardWidth: Dp = 175.dp,

    cardHeight: Dp = 125.dp
) {

    val appColors = AppTheme.colors

    Column(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)

            /*
             * ORIGINAL SHADOW
             */

            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp)
            )

            /*
             * ORIGINAL SHAPE
             */

            .clip(
                RoundedCornerShape(16.dp)
            )

            /*
             * ORIGINAL BACKGROUND
             */

            .background(
                appColors.textfield
            )

            /*
             * ORIGINAL CLICK BEHAVIOR
             */

            .clickable {
                onClick()
            }

            /*
             * Small horizontal padding prevents
             * text from touching the card edges
             * when the card becomes narrow.
             */

            .padding(
                horizontal = 8.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {


        /*
         * ICON
         */

        Icon(
            painter =
                painterResource(
                    id = iconRes
                ),

            contentDescription =
                title,

            tint =
                AppTheme.colors.backButton,

            modifier =
                Modifier.size(
                    iconWidth,
                    iconHeight
                )
        )


        /*
         * ORIGINAL 12dp SPACING
         */

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        /*
         * TEXT
         *
         * maxLines = 2 allows the text to
         * wrap instead of overlapping when
         * the card becomes narrow.
         */

        Text(
            text = title,

            fontSize = 19.sp,

            fontWeight =
                FontWeight.Medium,

            fontFamily =
                MartelFont,

            color =
                Color.Black,

            maxLines = 2,

            textAlign =
                TextAlign.Center
        )
    }
}


/*
 * PREVIEW
 */

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun HelpScreenPreview() {

    GreenTheme {

        HelpScreen()
    }
}