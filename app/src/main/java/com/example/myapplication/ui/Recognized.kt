package com.example.myapplication.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.DiscussionSummaryBox
import com.example.myapplication.ui.components.FieldLabel
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.LocationPickerField
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowTextField
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.BaumansFont
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont
import androidx.compose.ui.graphics.Brush
import com.example.myapplication.ui.components.DateDisplayField
import com.example.myapplication.ui.components.InfoNotePill
import com.example.myapplication.ui.components.RecordConversationField
import com.example.myapplication.ui.components.VoicePlayerBar
import androidx.compose.ui.res.stringResource

class RecognisedScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GreenTheme {
                RecognisedContent()
            }
        }
    }
}

@Composable
fun RecognisedContent(
    imageBitmap: ImageBitmap? = null,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
    onVoiceSampleClick: () -> Unit = {},
    onViewmore: () -> Unit = {}
) {

    val appColors = AppTheme.colors

    var nameText by remember { mutableStateOf("") }
    var relationText by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

    var isPlaying by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableStateOf(1f) }

    var textAlign by remember {
        mutableStateOf(TextAlign.Left)
    }

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
             * ============================================================
             * RESPONSIVE SCREEN DIMENSIONS
             * ============================================================
             */

            val screenWidth = maxWidth
            val screenHeight = maxHeight


            /*
             * ============================================================
             * RESPONSIVE DIMENSIONS
             * ============================================================
             */

            // Main horizontal padding
            val horizontalPadding =
                (screenWidth * 0.06f)
                    .coerceIn(20.dp, 32.dp)

            // Header → content spacing
            val headerSpacing =
                (screenHeight * 0.28f)
                    .coerceIn(190.dp, 230.dp)

            // Content upward offset
            val contentOffset =
                (screenHeight * 0.052f)
                    .coerceIn(32.dp, 44.dp)

            // First spacing before image
            val imageTopSpacing =
                (screenHeight * 0.035f)
                    .coerceIn(22.dp, 32.dp)


            /*
             * FIELD DIMENSIONS
             */

            val fieldLabelSize =
                (screenWidth.value * 0.045f)
                    .coerceIn(16f, 19f)
                    .sp

            val fieldHeight =
                (screenHeight * 0.060f)
                    .coerceIn(50.dp, 55.dp)

            val fieldCornerRadius =
                (screenWidth * 0.040f)
                    .coerceIn(13.dp, 16.dp)

            val fieldLabelSpacing =
                (screenHeight * 0.009f)
                    .coerceIn(6.dp, 10.dp)

            val fieldSectionSpacing =
                (screenHeight * 0.033f)
                    .coerceIn(25.dp, 34.dp)


            /*
             * ============================================================
             * CONTENT
             * ============================================================
             */

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                /*
                 * HEADER → CONTENT
                 */

                Spacer(
                    modifier = Modifier.height(
                        headerSpacing
                    )
                )


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = horizontalPadding
                        )
                        .offset(
                            y = -contentOffset
                        )
                ) {

                    Spacer(
                        modifier = Modifier.height(
                            imageTopSpacing
                        )
                    )


                    /*
                     * ====================================================
                     * PROFILE IMAGE
                     * ====================================================
                     */

                    val profileImageWidth =
                        (screenWidth * 0.57f)
                            .coerceIn(180.dp, 220.dp)

                    val profileImageHeight =
                        (screenHeight * 0.215f)
                            .coerceIn(165.dp, 190.dp)

                    val profileIconSize =
                        (screenWidth * 0.17f)
                            .coerceIn(56.dp, 68.dp)

                    val profileCornerRadius =
                        (screenWidth * 0.040f)
                            .coerceIn(13.dp, 17.dp)


                    Box(
                        modifier = Modifier
                            .width(profileImageWidth)
                            .height(profileImageHeight)
                            .align(
                                Alignment.CenterHorizontally
                            )
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(
                                    profileCornerRadius
                                ),
                                clip = false
                            )
                            .clip(
                                RoundedCornerShape(
                                    profileCornerRadius
                                )
                            )
                            .background(
                                appColors.textfield
                            ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        if (imageBitmap != null) {

                            Image(
                                bitmap = imageBitmap,
                                contentDescription =
                                    "Profile photo",

                                contentScale =
                                    ContentScale.Crop,

                                modifier =
                                    Modifier.fillMaxSize()
                            )

                        } else {

                            Icon(
                                painter = painterResource(
                                    id = R.drawable.profile_icon
                                ),

                                contentDescription =
                                    stringResource(
                                        R.string.profile_photo
                                    ),

                                tint =
                                    appColors.backButton,

                                modifier =
                                    Modifier.size(
                                        profileIconSize
                                    )
                            )
                        }
                    }


                    /*
                     * IMAGE → NAME
                     */

                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.032f)
                                .coerceIn(22.dp, 32.dp)
                        )
                    )


                    /*
                     * ====================================================
                     * NAME
                     * ==================================================== */

                    FieldLabel(
                        stringResource(
                            R.string.name_label
                        ),

                        fieldLabelSize,

                        OutfitFont,

                        FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(
                            fieldLabelSpacing
                        )
                    )

                    ShadowTextField(
                        value = nameText,

                        onValueChange = {
                            nameText = it
                        },

                        placeholder = stringResource(
                            R.string.enter_full_name
                        ),

                        leadingIconRes =
                            R.drawable.profile_icon,

                        height = fieldHeight,

                        cornerRadius =
                            fieldCornerRadius
                    )


                    /*
                     * NAME → RELATION
                     */

                    Spacer(
                        modifier = Modifier.height(
                            fieldSectionSpacing
                        )
                    )


                    /*
                     * ====================================================
                     * RELATION
                     * ==================================================== */

                    FieldLabel(
                        stringResource(
                            R.string.relation_label
                        ),

                        fieldLabelSize,

                        OutfitFont,

                        FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(
                            fieldLabelSpacing
                        )
                    )

                    ShadowTextField(
                        value = relationText,

                        onValueChange = {
                            relationText = it
                        },

                        placeholder = stringResource(
                            R.string.enter_relation
                        ),

                        leadingIconRes =
                            R.drawable.relation_icon,

                        height = fieldHeight,

                        cornerRadius =
                            fieldCornerRadius
                    )


                    /*
                     * RELATION → LAST MEETING
                     */

                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.040f)
                                .coerceIn(30.dp, 40.dp)
                        )
                    )


                    // =====================================================
                    // PART 2 CONTINUES HERE
                    // =====================================================
                    /*
 * ====================================================
 * LAST MEETING INFORMATION
 * ====================================================
 */

                    val sectionTitleSize =
                        (screenWidth.value * 0.073f)
                            .coerceIn(27f, 32f)
                            .sp

                    val dividerThickness =
                        (screenWidth * 0.005f)
                            .coerceIn(1.5.dp, 2.5.dp)

                    val dividerSpacing =
                        (screenHeight * 0.005f)
                            .coerceIn(3.dp, 6.dp)


                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = stringResource(
                                R.string.last_meeting_information
                            ),

                            fontSize =
                                sectionTitleSize,

                            fontFamily =
                                BaumansFont,

                            fontWeight =
                                FontWeight.Normal,

                            color =
                                appColors.pagesText,

                            textAlign =
                                textAlign,

                            modifier =
                                Modifier.fillMaxWidth(),

                            onTextLayout = { result ->

                                textAlign =
                                    if (
                                        result.lineCount > 1
                                    ) {
                                        TextAlign.Center
                                    } else {
                                        TextAlign.Left
                                    }
                            }
                        )


                        Spacer(
                            modifier = Modifier.height(
                                dividerSpacing
                            )
                        )


                        HorizontalDivider(
                            modifier =
                                Modifier.fillMaxWidth(),

                            thickness =
                                dividerThickness,

                            color =
                                appColors.pagesText
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.030f)
                                .coerceIn(22.dp, 30.dp)
                        )
                    )


                    /*
                     * ====================================================
                     * LOCATION + DATE
                     * ====================================================
                     */

                    val rowSpacing =
                        (screenWidth * 0.030f)
                            .coerceIn(10.dp, 16.dp)

                    val rowTopPadding =
                        (screenHeight * 0.017f)
                            .coerceIn(12.dp, 18.dp)

                    val smallFieldTextSize =
                        (screenWidth.value * 0.045f)
                            .coerceIn(16f, 19f)
                            .sp


                    Row(
                        verticalAlignment =
                            Alignment.Top,

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                rowSpacing
                            ),

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        /*
                         * LOCATION
                         */

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(
                                    top = rowTopPadding
                                )
                        ) {

                            FieldLabel(
                                stringResource(
                                    R.string.location_label
                                ),

                                smallFieldTextSize,

                                OutfitFont,

                                FontWeight.Medium
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    fieldLabelSpacing
                                )
                            )

                            LocationPickerField(
                                value =
                                    selectedAddress,

                                placeholder =
                                    stringResource(
                                        R.string.get_current_location
                                    ),

                                onClick = {
                                    // open map
                                }
                            )
                        }


                        /*
                         * DATE
                         */

                        Column(
                            modifier = Modifier.padding(
                                top = rowTopPadding
                            )
                        ) {

                            FieldLabel(
                                stringResource(
                                    R.string.date
                                ),

                                smallFieldTextSize,

                                OutfitFont,

                                FontWeight.Medium
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    fieldLabelSpacing
                                )
                            )

                            DateDisplayField(
                                date =
                                    "26 Jun 2026",

                                backgroundColor =
                                    appColors.textfield,

                                textColor =
                                    appColors.pagesText,

                                fontSize =
                                    smallFieldTextSize
                            )
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.040f)
                                .coerceIn(30.dp, 40.dp)
                        )
                    )


                    /*
                     * ====================================================
                     * DISCUSSION SUMMARY
                     * ==================================================== */

                    FieldLabel(
                        stringResource(
                            R.string.discussion_summary
                        ),

                        fieldLabelSize,

                        OutfitFont,

                        FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.012f)
                                .coerceIn(9.dp, 13.dp)
                        )
                    )

                    DiscussionSummaryBox()


                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.030f)
                                .coerceIn(24.dp, 30.dp)
                        )
                    )


                    /*
                     * ====================================================
                     * VOICE SUMMARY
                     * ==================================================== */

                    FieldLabel(
                        stringResource(
                            R.string.discussion_summary_in_voice
                        ),

                        fieldLabelSize,

                        OutfitFont,

                        FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.012f)
                                .coerceIn(9.dp, 13.dp)
                        )
                    )


                    VoicePlayerBar(
                        isPlaying =
                            isPlaying,

                        speedMultiplier =
                            speedMultiplier,

                        backgroundColor =
                            appColors.textfield,

                        onPlayPauseClick = {
                            isPlaying =
                                !isPlaying
                        },

                        onSpeedClick = {

                            speedMultiplier =
                                when (
                                    speedMultiplier
                                ) {

                                    1f -> 1.5f

                                    1.5f -> 2f

                                    else -> 1f
                                }
                        }
                    )


                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.035f)
                                .coerceIn(28.dp, 36.dp)
                        )
                    )


                    /*
                     * ====================================================
                     * VIEW MORE BUTTON
                     * ==================================================== */

                    val actionButtonHeight =
                        (screenHeight * 0.065f)
                            .coerceIn(52.dp, 58.dp)

                    val actionButtonTextSize =
                        (screenWidth.value * 0.055f)
                            .coerceIn(20f, 23f)
                            .sp


                    ShadowButton(
                        height =
                            actionButtonHeight,

                        color =
                            appColors.popupText,

                        cornerRadius =
                            (actionButtonHeight * 0.50f)
                                .coerceIn(
                                    26.dp,
                                    30.dp
                                ),

                        onClick = {
                            onViewmore()
                        }
                    ) {

                        Text(
                            text = stringResource(
                                R.string.view_more
                            ),

                            fontSize =
                                actionButtonTextSize,

                            fontWeight =
                                FontWeight.Medium,

                            fontFamily =
                                OutfitFont,

                            color =
                                appColors.pagesText
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.042f)
                                .coerceIn(34.dp, 44.dp)
                        )
                    )


                    /*
                     * ====================================================
                     * ADD NEW INFORMATION
                     * ==================================================== */

                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = stringResource(
                                R.string.add_new_information
                            ),

                            fontSize =
                                sectionTitleSize,

                            fontFamily =
                                BaumansFont,

                            fontWeight =
                                FontWeight.Normal,

                            color =
                                appColors.pagesText,

                            textAlign =
                                TextAlign.Center,

                            modifier =
                                Modifier.fillMaxWidth()
                        )


                        Spacer(
                            modifier = Modifier.height(
                                dividerSpacing
                            )
                        )


                        HorizontalDivider(
                            modifier =
                                Modifier.fillMaxWidth(),

                            thickness =
                                dividerThickness,

                            color =
                                appColors.pagesText
                        )


                        Spacer(
                            modifier = Modifier.height(
                                (screenHeight * 0.007f)
                                    .coerceIn(5.dp, 8.dp)
                            )
                        )


                        InfoNotePill(
                            text = stringResource(
                                R.string.select_location_record_both
                            )
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.027f)
                                .coerceIn(22.dp, 28.dp)
                        )
                    )


                    /*
                     * ====================================================
                     * NEW LOCATION + GET LOCATION
                     * ==================================================== */

                    Row(
                        verticalAlignment =
                            Alignment.Top,

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                rowSpacing
                            ),

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        /*
                         * LOCATION
                         */

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(
                                    top = rowTopPadding
                                )
                        ) {

                            FieldLabel(
                                stringResource(
                                    R.string.location_label
                                ),

                                fieldLabelSize,

                                OutfitFont,

                                FontWeight.Medium
                            )

                            Spacer(
                                modifier = Modifier.height(
                                    fieldLabelSpacing
                                )
                            )


                            LocationPickerField(
                                value =
                                    selectedAddress,

                                placeholder =
                                    stringResource(
                                        R.string.open_location_in_map
                                    ),

                                onClick = { }
                            )
                        }


                        /*
                         * GET LOCATION
                         */

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            modifier = Modifier.padding(
                                top = rowTopPadding
                            )
                        ) {

                            FieldLabel(
                                stringResource(
                                    R.string.get_location
                                ),

                                (screenWidth.value * 0.042f)
                                    .coerceIn(
                                        15f,
                                        18f
                                    )
                                    .sp,

                                OutfitFont,

                                FontWeight.Medium
                            )


                            Spacer(
                                modifier = Modifier.height(
                                    (screenHeight * 0.012f)
                                        .coerceIn(
                                            10.dp,
                                            15.dp
                                        )
                                )
                            )


                            ShadowButton(
                                width =
                                    (screenWidth * 0.19f)
                                        .coerceIn(
                                            64.dp,
                                            76.dp
                                        ),

                                height =
                                    (screenHeight * 0.060f)
                                        .coerceIn(
                                            48.dp,
                                            53.dp
                                        ),

                                color =
                                    appColors.popupText,

                                cornerRadius =
                                    (screenWidth * 0.040f)
                                        .coerceIn(
                                            13.dp,
                                            16.dp
                                        ),

                                onClick = { }
                            ) {

                                Icon(
                                    painter =
                                        painterResource(
                                            R.drawable.location_icon
                                        ),

                                    contentDescription =
                                        null,

                                    tint =
                                        appColors.pagesText,

                                    modifier =
                                        Modifier.size(
                                            (screenWidth * 0.085f)
                                                .coerceIn(
                                                    28.dp,
                                                    34.dp
                                                )
                                        )
                                )
                            }
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.050f)
                                .coerceIn(40.dp, 52.dp)
                        )
                    )


                    // =====================================================
                    // PART 3 CONTINUES HERE
                    // =====================================================
                    /*
 * ====================================================
 * OR DIVIDER
 * ==================================================== */

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                (screenHeight * 0.0015f)
                                    .coerceIn(1.dp, 2.dp)
                            )
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,

                                        appColors.pagesText
                                            .copy(alpha = 0.5f),

                                        Color.Transparent
                                    )
                                )
                            )
                    )


                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.028f)
                                .coerceIn(22.dp, 30.dp)
                        )
                    )


                    /*
                     * ====================================================
                     * RECORD CONVERSATION
                     * ==================================================== */

                    Column(
                        modifier =
                            Modifier.fillMaxWidth(0.70f)
                    ) {

                        FieldLabel(
                            stringResource(
                                R.string.record_conversation
                            ),

                            fieldLabelSize,

                            OutfitFont,

                            FontWeight.Medium
                        )


                        Spacer(
                            modifier = Modifier.height(
                                fieldLabelSpacing
                            )
                        )


                        RecordConversationField(
                            value =
                                selectedAddress,

                            placeholder =
                                stringResource(
                                    R.string.click_to_record
                                ),

                            onClick = {
                                onVoiceSampleClick()
                            }
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.030f)
                                .coerceIn(24.dp, 32.dp)
                        )
                    )


                    /*
                     * ====================================================
                     * SAVE BUTTON
                     * ==================================================== */

                    ShadowButton(
                        height =
                            actionButtonHeight,

                        color =
                            appColors.popupText,

                        cornerRadius =
                            (actionButtonHeight * 0.50f)
                                .coerceIn(
                                    26.dp,
                                    30.dp
                                ),

                        onClick = {
                            onSave()
                        }
                    ) {

                        Text(
                            text = stringResource(
                                R.string.save
                            ),

                            color =
                                appColors.pagesText,

                            fontSize =
                                actionButtonTextSize,

                            fontWeight =
                                FontWeight.Medium,

                            fontFamily =
                                OutfitFont
                        )
                    }


                    /*
                     * BOTTOM SPACING
                     */

                    Spacer(
                        modifier = Modifier.height(
                            (screenHeight * 0.075f)
                                .coerceIn(
                                    55.dp,
                                    75.dp
                                )
                        )
                    )
                }
            }


            /*
             * ============================================================
             * HEADER
             *
             * LEAVE THIS AS YOUR EXISTING HEADER.
             * ============================================================
             */

            HeaderSection(
                title =
                    stringResource(
                        R.string.result
                    ),

                secondaryTitle =
                    stringResource(
                        R.string.recognised
                    ),

                218.dp,

                33.sp,

                60.dp,

                28.dp,

                leaves =
                    appColors.headerDecorOffset2,

                onBack = {
                    onBack()
                }
            )
        }
    }
}


/*
 * ================================================================
 * PREVIEW
 * ================================================================
 */

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecognisedPreview() {
    GreenTheme {
//        val context = LocalContext.current
//        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.loading_4)
        RecognisedContent()
    }
}