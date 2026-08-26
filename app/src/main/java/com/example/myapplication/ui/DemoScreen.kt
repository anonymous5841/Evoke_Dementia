package com.example.myapplication.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont
import androidx.compose.ui.res.stringResource


// ══════════════════════════════════════════════════════
// CLICKABLE SELECTION CARD
// ══════════════════════════════════════════════════════

@Composable
fun ClickableSelectionCard(
    text: String,
    isSelected: Boolean,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color,
    defaultColor: Color,
    iconTint: Color = Color.Unspecified,
    textColor: Color,

    // Responsive dimensions
    cardHeight: androidx.compose.ui.unit.Dp = 60.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 20.dp,
    iconPadding: androidx.compose.ui.unit.Dp = 20.dp
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(cardHeight)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(15.dp),
                clip = false,
                ambientColor = Color.LightGray.copy(alpha = 0.3f),
                spotColor = Color.LightGray.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick),

        shape = RoundedCornerShape(15.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                if (isSelected) selectedColor
                else defaultColor
        ),

        elevation = CardDefaults.cardElevation(8.dp)
    ) {

        Box(
            modifier = Modifier.fillMaxSize()
        ) {

            // ─────────────────────────────────────────────
            // TEXT
            // ─────────────────────────────────────────────

            Text(
                text = text,

                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(
                        start = horizontalPadding,
                        end = 50.dp
                    ),

                fontFamily = MartelFont,
                fontSize = textSize,
                fontWeight = FontWeight.Normal,
                color = textColor,
                maxLines = 1
            )


            // ─────────────────────────────────────────────
            // FORWARD ICON
            // ─────────────────────────────────────────────

            Icon(
                painter = painterResource(id = icon),

                contentDescription = null,

                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = iconPadding),

                tint = iconTint
            )
        }
    }
}


// ══════════════════════════════════════════════════════
// DEMO SCREEN
// ══════════════════════════════════════════════════════

@Composable
fun DemoScreen(
    onBack: () -> Unit = {},
    onPersonRegistration: () -> Unit = {},
    onPersonRecognition: () -> Unit = {},
    onAddLocation: () -> Unit = {},
    onSearchLocation: () -> Unit = {},
    onSearchPersonByName: () -> Unit = {},
    onNavigationToHome: () -> Unit = {},
    onSendHelpMessage: () -> Unit = {},
    onEmergencyCall: () -> Unit = {},
    onChangeAppLanguage: () -> Unit = {},
    onChangeAppTheme: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRecoveryData: () -> Unit = {},
    onCompleteDelete: () -> Unit = {},
) {

    val appColors = AppTheme.colors


    Scaffold(
        containerColor = appColors.background,

        contentWindowInsets = WindowInsets(
            0, 0, 0, 0
        )

    ) { innerPadding ->


        // ════════════════════════════════════════════════
        // BOX WITH CONSTRAINTS
        // ════════════════════════════════════════════════

        BoxWithConstraints(

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

        ) {


            // ════════════════════════════════════════════
            // RESPONSIVE WIDTH VALUES
            // ════════════════════════════════════════════

            /*
             * Original design:
             *
             * fillMaxWidth(0.96f)
             * padding(horizontal = 16.dp)
             *
             * We keep the same visual proportion but
             * calculate it from the available width.
             */


            // Main content width
            val contentWidth =
                (maxWidth * 0.96f)
                    .coerceAtMost(maxWidth)


            // Horizontal padding
            val horizontalPadding =
                (maxWidth * 0.05f)
                    .coerceIn(
                        16.dp,
                        24.dp
                    )


            // Card height
            //
            // Original = 60.dp
            //
            // We don't let it become too small because
            // the text/icon must remain readable.

            val cardHeight =
                (maxWidth * 0.1875f)
                    .coerceIn(
                        60.dp,
                        72.dp
                    )


            // Space between cards
            //
            // Original = 28.dp

            val cardSpacing =
                (maxWidth * 0.0875f)
                    .coerceIn(
                        28.dp,
                        42.dp
                    )


            // Text size
            //
            // Original = 18.sp
            //
            // Keep it readable on narrow screens.

            val cardTextSize =
                (maxWidth.value * 0.05625f)
                    .coerceIn(
                        18f,
                        21f
                    ).sp


            // Text left padding
            val textPadding =
                (maxWidth * 0.0625f)
                    .coerceIn(
                        20.dp,
                        28.dp
                    )


            // Icon right padding
            val iconPadding =
                (maxWidth * 0.0625f)
                    .coerceIn(
                        20.dp,
                        28.dp
                    )


            // Space reserved on the right of the text
            //
            // This prevents long titles from touching
            // the forward arrow.

            val textEndPadding =
                (maxWidth * 0.15f)
                    .coerceIn(
                        48.dp,
                        72.dp
                    )


            // ════════════════════════════════════════════
            // SCROLLABLE CONTENT
            // ════════════════════════════════════════════

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {


                // Same header space as original design
                Spacer(
                    modifier = Modifier.height(
                        247.dp
                    )
                )


                // ════════════════════════════════════════
                // DEMO ITEMS CONTAINER
                // ════════════════════════════════════════

                Column(

                    modifier = Modifier
                        .width(contentWidth)
                        .align(
                            Alignment.CenterHorizontally
                        )
                        .padding(
                            horizontal = horizontalPadding
                        )
                        .padding(
                            bottom = 70.dp
                        )
                        .offset(
                            y = (-30).dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            cardSpacing
                        )

                ) {


                    val demoItems = listOf(

                        stringResource(
                            R.string.person_registration
                        ) to onPersonRegistration,

                        stringResource(
                            R.string.person_recognition
                        ) to onPersonRecognition,

                        stringResource(
                            R.string.add_location
                        ) to onAddLocation,

                        stringResource(
                            R.string.search_location
                        ) to onSearchLocation,

                        stringResource(
                            R.string.search_person_by_name
                        ) to onSearchPersonByName,

                        stringResource(
                            R.string.navigation_to_home
                        ) to onNavigationToHome,

                        stringResource(
                            R.string.send_help_message
                        ) to onSendHelpMessage,

                        stringResource(
                            R.string.emergency_call_for_help
                        ) to onEmergencyCall,

                        stringResource(
                            R.string.change_app_language
                        ) to onChangeAppLanguage,

                        stringResource(
                            R.string.change_app_theme
                        ) to onChangeAppTheme,

                        stringResource(
                            R.string.backup_data_on_cloud
                        ) to onBackup,

                        stringResource(
                            R.string.recover_data_from_cloud
                        ) to onRecoveryData,

                        stringResource(
                            R.string.delete_app_data_completely
                        ) to onCompleteDelete
                    )


                    // ════════════════════════════════════
                    // CREATE CARDS
                    // ════════════════════════════════════

                    demoItems.forEach { (title, action) ->

                        if (title.isNotEmpty()) {

                            DemoItem(
                                title = title,
                                onClick = action,

                                cardHeight = cardHeight,
                                textSize = cardTextSize,
                                textPadding = textPadding,
                                iconPadding = iconPadding,
                                textEndPadding = textEndPadding
                            )
                        }
                    }
                }
            }


            // ════════════════════════════════════════════
            // HEADER
            // ════════════════════════════════════════════

            HeaderSection(
                stringResource(R.string.demo),

                spacing = 73.dp,

                bottomspace = 44.dp,

                onBack = onBack
            )
        }
    }
}


// ══════════════════════════════════════════════════════
// DEMO ITEM
// ══════════════════════════════════════════════════════

@Composable
fun DemoItem(
    title: String,
    onClick: () -> Unit,

    // Responsive values supplied by DemoScreen
    cardHeight: androidx.compose.ui.unit.Dp = 60.dp,
    textSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    textPadding: androidx.compose.ui.unit.Dp = 20.dp,
    iconPadding: androidx.compose.ui.unit.Dp = 20.dp,
    textEndPadding: androidx.compose.ui.unit.Dp = 50.dp
) {

    val appColors = AppTheme.colors


    ClickableSelectionCard(

        text = title,

        isSelected = false,

        icon = R.drawable.forward_icon,

        onClick = onClick,

        selectedColor =
            appColors.textfield,

        defaultColor =
            appColors.textfield,

        iconTint =
            appColors.backButton,

        textColor =
            appColors.pagesText,

        cardHeight =
            cardHeight,

        textSize =
            textSize,

        horizontalPadding =
            textPadding,

        iconPadding =
            iconPadding,

        modifier = Modifier
    )
}


// ══════════════════════════════════════════════════════
// PREVIEW
// ══════════════════════════════════════════════════════

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun DemoPreview() {

    GreenTheme {

        DemoScreen()
    }
}