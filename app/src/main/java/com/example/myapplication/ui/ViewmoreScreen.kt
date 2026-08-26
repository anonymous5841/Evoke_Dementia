package com.example.myapplication.ui

// ViewMoreScreen.kt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource

import com.example.myapplication.R
import com.example.myapplication.ui.components.AddShapeButton
import com.example.myapplication.ui.components.BackIconButton
import com.example.myapplication.ui.components.IconWithShadow
import com.example.myapplication.ui.components.LocationPickerField
import com.example.myapplication.ui.components.SearchFieldWithIcon
import com.example.myapplication.ui.components.VoicePlayerBar
import com.example.myapplication.ui.components.rememberBottomNavBarHeight
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.ui.theme.OutfitFont


// ================================================================
// VIEW MORE SCREEN
// ================================================================

@Composable
fun ViewMoreScreen(
    onBackClick: () -> Unit = {},
    onEllipseClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
) {
    var searchQuery by remember {
        mutableStateOf("")
    }

    val recordings = remember {
        mutableStateListOf(1, 2, 3)
    }

    val playingStates =
        remember { mutableStateMapOf<Int, Boolean>() }

    val speedStates =
        remember { mutableStateMapOf<Int, Float>() }

    val bottomPadding =
        rememberBottomNavBarHeight()

    val appColors =
        AppTheme.colors


    Scaffold(
        containerColor = appColors.background,

        contentWindowInsets =
            WindowInsets(0, 0, 0, 0)

    ) { innerPadding ->


        // ============================================================
        // BOX WITH CONSTRAINTS
        // ============================================================

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {


            // ========================================================
            // RESPONSIVE WIDTH VALUES
            // ========================================================

            /*
             * Original screen padding:
             *
             * 16.dp on each side
             *
             * We keep this as the reference.
             */

            val screenPadding = 16.dp


            /*
             * Actual width available to the recording cards.
             */

            val contentWidth =
                (maxWidth - (screenPadding * 2))
                    .coerceAtLeast(0.dp)


            /*
             * Search/header spacing
             *
             * Original spacing remains unchanged.
             */

            val topSpacing = 18.dp
            val backToSearchSpacing = 24.dp
            val searchToListSpacing = 35.dp


            /*
             * Original gap between recording items.
             */

            val recordingSpacing = 70.dp


            /*
             * ========================================================
             * RESPONSIVE RECORDING CARD HEIGHT
             * ========================================================
             *
             * 390dp phone:
             * approximately original 190dp
             *
             * Narrower phone:
             * never becomes smaller than 190dp
             *
             * Larger phone:
             * can grow slightly.
             */

            val recordingHeight =
                (contentWidth * 0.53f)
                    .coerceIn(
                        190.dp,
                        240.dp
                    )


            /*
             * DATE TEXT
             *
             * Original = 22.sp
             */

            val dateTextSize =
                (contentWidth.value * 0.061f)
                    .coerceIn(
                        22f,
                        25f
                    )
                    .sp


            /*
             * Location title
             *
             * Original = 18.sp
             */

            val locationTextSize =
                (contentWidth.value * 0.050f)
                    .coerceIn(
                        18f,
                        21f
                    )
                    .sp


            /*
             * Discussion summary text
             *
             * Original = 17.sp
             */

            val discussionTextSize =
                (contentWidth.value * 0.047f)
                    .coerceIn(
                        17f,
                        20f
                    )
                    .sp


            /*
             * ========================================================
             * ADD SHAPE / PLUS RESPONSIVE PLACEMENT
             * ========================================================
             *
             * Original reference:
             *
             * boxOffset.x = 298.dp
             * plusOffset.x = 289.dp
             *
             * These were effectively based on a ~358dp card width.
             *
             * We calculate the offsets from the current content width
             * instead of using fixed 298/289dp values.
             */

            val shapeWidth =
                (contentWidth * 0.246f)
                    .coerceIn(
                        72.dp,
                        88.dp
                    )


            val shapeHeight =
                (contentWidth * 0.196f)
                    .coerceIn(
                        60.dp,
                        70.dp
                    )


            /*
             * Original 298dp offset is approximately:
             *
             * contentWidth - shapeWidth + 28dp
             */

            val shapeOffsetX =
                (
                        contentWidth -
                                shapeWidth +
                                28.dp
                        ).coerceAtLeast(0.dp)


            /*
             * Original plus x = 289dp.
             */

            val plusOffsetX =
                (
                        contentWidth -
                                shapeWidth +
                                19.dp
                        ).coerceAtLeast(0.dp)


            /*
             * Original vertical positions:
             *
             * -42dp
             * -29dp
             *
             * Keep these because vertical placement is part
             * of the original design.
             */

            val shapeOffsetY = (-42).dp
            val plusOffsetY = (-29).dp


            /*
             * Plus size
             *
             * Original = 67.sp
             */

            val plusFontSize =
                (contentWidth.value * 0.187f)
                    .coerceIn(
                        60f,
                        67f
                    )
                    .sp


            /*
             * ========================================================
             * MAIN COLUMN
             * ========================================================
             */

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = screenPadding
                    )
            ) {


                /*
                 * ORIGINAL TOP SPACING
                 */

                Spacer(
                    modifier = Modifier.height(
                        topSpacing
                    )
                )


                /*
                 * BACK BUTTON
                 */

                BackIconButton(
                    onBackClick
                )


                /*
                 * ORIGINAL GAP
                 */

                Spacer(
                    modifier = Modifier.height(
                        backToSearchSpacing
                    )
                )


                /*
                 * SEARCH FIELD
                 */

                SearchFieldWithIcon(
                    placeholder =
                        stringResource(
                            R.string.search_by_date
                        ),

                    value =
                        searchQuery,

                    onValueChange = {
                        searchQuery = it
                    },

                    greenIconOffset =
                        DpOffset(
                            x = (-20).dp,
                            y = (-1).dp
                        ),

                    searchIconOffset =
                        DpOffset(
                            x = 4.4.dp,
                            y = (-9).dp
                        ),

                    boxWidth = null
                )


                /*
                 * ORIGINAL GAP
                 */

                Spacer(
                    modifier = Modifier.height(
                        searchToListSpacing
                    )
                )


                /*
                 * RECORDING LIST
                 */

                LazyColumn(
                    modifier = Modifier
                        .weight(1f),

                    contentPadding =
                        PaddingValues(
                            bottom =
                                (bottomPadding - 15.dp)
                        ),

                    verticalArrangement =
                        Arrangement.Top
                ) {

                    itemsIndexed(
                        recordings
                    ) { index, _ ->

                        RecordingItem(
                            index = index,

                            onDeleteClick = {
                                onDeleteClick()
                            },

                            onEllipseClick =
                                onEllipseClick,

                            isPlaying =
                                playingStates[index]
                                    ?: false,

                            speedMultiplier =
                                speedStates[index]
                                    ?: 1f,

                            onPlayPauseClick = {
                                playingStates[index] =
                                    !(playingStates[index]
                                        ?: false)
                            },

                            onSpeedClick = {

                                val current =
                                    speedStates[index]
                                        ?: 1f

                                speedStates[index] =
                                    when (current) {
                                        1f -> 1.5f
                                        1.5f -> 2f
                                        else -> 1f
                                    }
                            },

                            cardHeight =
                                recordingHeight,

                            dateTextSize =
                                dateTextSize,

                            locationTextSize =
                                locationTextSize,

                            discussionTextSize =
                                discussionTextSize,

                            shapeWidth =
                                shapeWidth,

                            shapeHeight =
                                shapeHeight,

                            shapeOffsetX =
                                shapeOffsetX,

                            shapeOffsetY =
                                shapeOffsetY,

                            plusOffsetX =
                                plusOffsetX,

                            plusOffsetY =
                                plusOffsetY,

                            plusFontSize =
                                plusFontSize
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    recordingSpacing
                                )
                        )
                    }
                }
            }
        }
    }
}


// ================================================================
// RECORDING ITEM
// ================================================================

@Composable
fun RecordingItem(
    index: Int,
    onDeleteClick: () -> Unit,
    onEllipseClick: () -> Unit,
    isPlaying: Boolean,
    speedMultiplier: Float,
    onPlayPauseClick: () -> Unit,
    onSpeedClick: () -> Unit,

    /*
     * Responsive values supplied by ViewMoreScreen
     */

    cardHeight: androidx.compose.ui.unit.Dp =
        190.dp,

    dateTextSize: androidx.compose.ui.unit.TextUnit =
        22.sp,

    locationTextSize: androidx.compose.ui.unit.TextUnit =
        18.sp,

    discussionTextSize: androidx.compose.ui.unit.TextUnit =
        17.sp,

    shapeWidth: androidx.compose.ui.unit.Dp =
        88.dp,

    shapeHeight: androidx.compose.ui.unit.Dp =
        70.dp,

    shapeOffsetX: androidx.compose.ui.unit.Dp =
        298.dp,

    shapeOffsetY: androidx.compose.ui.unit.Dp =
        (-42).dp,

    plusOffsetX: androidx.compose.ui.unit.Dp =
        289.dp,

    plusOffsetY: androidx.compose.ui.unit.Dp =
        (-29).dp,

    plusFontSize: androidx.compose.ui.unit.TextUnit =
        67.sp
) {

    var selectedLocation by remember {
        mutableStateOf("")
    }

    val appColors =
        AppTheme.colors


    Column {

        // ============================================================
        // DELETE + DATE
        // ============================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 4.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            IconWithShadow(
                painter =
                    painterResource(
                        R.drawable.ic_delete
                    ),

                contentDescription =
                    null,

                tint =
                    Color.Red,

                modifier =
                    Modifier.size(27.dp),

                onClick = {
                    onDeleteClick()
                }
            )


            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )


            Box(
                modifier =
                    Modifier.wrapContentSize()
            ) {

                Text(
                    text =
                        stringResource(
                            R.string.recording_date
                        ),

                    fontSize =
                        dateTextSize,

                    fontWeight =
                        FontWeight.Medium,

                    color =
                        Color.Black,

                    fontFamily =
                        MartelFont,

                    style =
                        TextStyle(
                            shadow =
                                Shadow(
                                    color =
                                        Color.Black.copy(
                                            alpha = 0.4f
                                        ),

                                    offset =
                                        Offset(
                                            x = 5f,
                                            y = 4f
                                        ),

                                    blurRadius = 5f
                                )
                        ),

                    modifier =
                        Modifier.offset(
                            y = 2.dp
                        )
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(4.dp)
        )


        // ============================================================
        // MAIN RECORDING CARD
        // ============================================================

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight)
        ) {


            // ========================================================
            // MAIN LOCATION BOX
            // ========================================================

            Box(
                modifier = Modifier
                    .fillMaxSize()

                    .shadow(
                        elevation = 8.dp,

                        shape =
                            RoundedCornerShape(
                                topStart = 0.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp
                            )
                    )

                    .clip(
                        RoundedCornerShape(
                            topStart = 0.dp,
                            topEnd = 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )

                    .background(
                        appColors.boxOuter
                    )
            ) {


                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {


                    // ==================================================
                    // LOCATION
                    // ==================================================

                    Text(
                        text =
                            stringResource(
                                R.string.location
                            ),

                        fontSize =
                            locationTextSize,

                        color =
                            Color.Black,

                        fontFamily =
                            OutfitFont
                    )


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                    // ==================================================
                    // LOCATION PICKER
                    // ==================================================

                    LocationPickerField(
                        value =
                            selectedLocation,

                        placeholder =
                            stringResource(
                                R.string.open_location_map
                            ),

                        onClick = {
                            // open map or location picker
                        },

                        modifier =
                            Modifier.fillMaxWidth(
                                0.70f
                            ),

                        backgroundColor =
                            appColors.boxInner,

                        showShadow =
                            false
                    )


                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )


                    // ==================================================
                    // DISCUSSION SUMMARY
                    // ==================================================

                    Text(
                        text =
                            stringResource(
                                R.string.discussion_summary_voice
                            ),

                        fontSize =
                            discussionTextSize,

                        color =
                            Color.Black,

                        fontWeight =
                            FontWeight.Medium,

                        modifier =
                            Modifier.padding(
                                start = 15.dp,
                                top = 8.dp
                            )
                    )
                }
            }


            // ========================================================
            // ADD SHAPE BUTTON
            // ========================================================

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-42).dp)
                ) {

                    AddShapeButton(
                        onClick = onEllipseClick,
                        shapeColor = appColors.pagesText,
                        plusColor = appColors.popupText,

                        shapeShadowHeight = shapeHeight,
                        shapeShadowWidth = shapeWidth,

                        boxOffset = DpOffset(
                            x = 0.dp,
                            y = 0.dp
                        ),

                        plusFontSize = plusFontSize,

                        plusOffset = DpOffset(
                            x = (-9).dp,
                            y = 13.dp
                        )
                    )
                }
            }

            // ========================================================
            // VOICE PLAYER
            // ========================================================

            VoicePlayerBar(
                isPlaying =
                    isPlaying,

                speedMultiplier =
                    speedMultiplier,

                onPlayPauseClick =
                    onPlayPauseClick,

                onSpeedClick =
                    onSpeedClick,

                modifier = Modifier
                    .align(
                        Alignment.BottomCenter
                    )

                    .fillMaxWidth(
                        0.92f
                    )

                    .offset(
                        y = 20.dp
                    )
            )
        }
    }
}


// ================================================================
// PREVIEW
// ================================================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun ViewMorePreview() {

    GreenTheme {
        ViewMoreScreen()
    }
}