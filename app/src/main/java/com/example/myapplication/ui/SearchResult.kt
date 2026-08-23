package com.example.myapplication.ui
import android.graphics.BitmapFactory
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.DateDisplayField
import com.example.myapplication.ui.components.DiscussionSummaryBox
import com.example.myapplication.ui.components.FieldLabel
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.LocationPickerField
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowTextField
import com.example.myapplication.ui.components.VoicePlayerBar
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.BaumansFont
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.TextUnit

class SearchResultsScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GreenTheme {
                SearchResultsContent()
            }
        }
    }
}

@Composable
fun SearchResultsContent(
    imageBitmap: ImageBitmap? = null,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onBack: () -> Unit = {},
    onViewmore: () -> Unit = {},
) {

    var nameText by remember { mutableStateOf("") }
    var relationText by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

    var isPlaying by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableStateOf(1f) }

    val appColors = AppTheme.colors

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

            // ============================================================
            // RESPONSIVE DIMENSIONS
            // ============================================================

            /*
             * Horizontal screen padding.
             *
             * 360dp → approximately 24dp
             * 390dp → approximately 25dp
             * 412dp → approximately 27dp
             */
            val horizontalPadding =
                (maxWidth.value * 0.065f)
                    .coerceIn(20f, 28f)
                    .dp


            /*
             * Profile image
             *
             * Original: 206 x 182
             */
            val imageWidth =
                (maxWidth.value * 0.54f)
                    .coerceIn(190f, 220f)
                    .dp

            val imageHeight =
                imageWidth * 0.883f


            /*
             * Responsive text sizes.
             *
             * IMPORTANT:
             * maxWidth.value gives us a Float.
             * We then convert the result to sp.
             */
            val sectionTitleSize: TextUnit =
                (maxWidth.value * 0.082f)
                    .coerceIn(29f, 34f)
                    .sp

            val fieldLabelSize: TextUnit =
                (maxWidth.value * 0.047f)
                    .coerceIn(17f, 19f)
                    .sp


            /*
             * Date field width
             */
            val dateFieldWidth =
                (maxWidth.value * 0.29f)
                    .coerceIn(105f, 125f)
                    .dp


            /*
             * Responsive vertical spacing
             */
            val imageTopSpacing =
                (maxHeight.value * 0.025f)
                    .coerceIn(20f, 30f)
                    .dp

            val imageBottomSpacing =
                (maxHeight.value * 0.025f)
                    .coerceIn(22f, 30f)
                    .dp

            val fieldSpacing =
                (maxHeight.value * 0.026f)
                    .coerceIn(24f, 32f)
                    .dp

            val sectionSpacing =
                (maxHeight.value * 0.035f)
                    .coerceIn(32f, 42f)
                    .dp


            // ============================================================
            // SCROLLABLE CONTENT
            // ============================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                /*
                 * Header space.
                 *
                 * Header is intentionally untouched.
                 */
                Spacer(
                    modifier = Modifier.height(218.dp)
                )


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = horizontalPadding
                        )
                        .offset(y = (-40).dp)
                ) {

                    Spacer(
                        modifier = Modifier.height(
                            imageTopSpacing
                        )
                    )


                    // ====================================================
                    // PROFILE IMAGE
                    // ====================================================

                    Box(
                        modifier = Modifier
                            .width(imageWidth)
                            .height(imageHeight)
                            .align(Alignment.CenterHorizontally)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(15.dp),
                                clip = false
                            )
                            .clip(
                                RoundedCornerShape(15.dp)
                            )
                            .background(
                                appColors.textfield
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        if (imageBitmap != null) {

                            Image(
                                bitmap = imageBitmap,
                                contentDescription =
                                    stringResource(
                                        R.string.profile_photo
                                    ),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
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
                                tint = appColors.backButton,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(
                            imageBottomSpacing
                        )
                    )


                    // ====================================================
                    // NAME
                    // ====================================================

                    FieldLabel(
                        stringResource(R.string.name),
                        fieldLabelSize,
                        OutfitFont,
                        FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    ShadowTextField(
                        value = nameText,
                        onValueChange = {
                            nameText = it
                        },
                        placeholder =
                            stringResource(
                                R.string.name_placeholder
                            ),
                        leadingIconRes =
                            R.drawable.profile_icon,
                        height = 52.dp,
                        cornerRadius = 15.dp
                    )


                    Spacer(
                        modifier = Modifier.height(
                            fieldSpacing
                        )
                    )


                    // ====================================================
                    // RELATION
                    // ====================================================

                    FieldLabel(
                        stringResource(R.string.relation),
                        fieldLabelSize,
                        OutfitFont,
                        FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    ShadowTextField(
                        value = relationText,
                        onValueChange = {
                            relationText = it
                        },
                        placeholder =
                            stringResource(
                                R.string.relation_placeholder
                            ),
                        leadingIconRes =
                            R.drawable.ic_call,
                        height = 52.dp,
                        cornerRadius = 15.dp
                    )


                    Spacer(
                        modifier = Modifier.height(
                            sectionSpacing
                        )
                    )


                    // ====================================================
                    // LAST MEETING INFORMATION
                    // ====================================================

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                stringResource(
                                    R.string.last_meeting_information
                                ),

                            fontSize = sectionTitleSize,

                            fontFamily = BaumansFont,

                            fontWeight = FontWeight.Normal,

                            color = appColors.pagesText,

                            textAlign = textAlign,

                            modifier = Modifier.fillMaxWidth(),

                            onTextLayout = { result: TextLayoutResult ->

                                textAlign =
                                    if (result.lineCount > 1) {
                                        TextAlign.Center
                                    } else {
                                        TextAlign.Left
                                    }
                            }
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 2.dp,
                            color = appColors.pagesText
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )


                    // ====================================================
                    // LOCATION + DATE
                    // ====================================================

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        // LOCATION

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 16.dp)
                        ) {

                            FieldLabel(
                                stringResource(R.string.location),
                                fieldLabelSize,
                                OutfitFont,
                                FontWeight.Medium
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            LocationPickerField(
                                value = selectedAddress,
                                placeholder =
                                    stringResource(
                                        R.string.get_current_location
                                    ),
                                onClick = {
                                    // open map
                                }
                            )
                        }


                        // DATE

                        Column(
                            modifier = Modifier
                                .padding(top = 16.dp)
                        ) {

                            FieldLabel(
                                stringResource(R.string.date),
                                fieldLabelSize,
                                OutfitFont,
                                FontWeight.Medium
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            DateDisplayField(
                                date = "26 Jun 2026",
                                backgroundColor =
                                    appColors.textfield,
                                textColor =
                                    appColors.pagesText,
                                fontSize = 18.sp,
                                modifier =
                                    Modifier.width(
                                        dateFieldWidth
                                    )
                            )
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(36.dp)
                    )


                    // ====================================================
                    // DISCUSSION SUMMARY
                    // ====================================================

                    FieldLabel(
                        stringResource(
                            R.string.discussion_summary
                        ),
                        fieldLabelSize,
                        OutfitFont,
                        FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    DiscussionSummaryBox()


                    Spacer(
                        modifier = Modifier.height(28.dp)
                    )


                    // ====================================================
                    // VOICE SUMMARY
                    // ====================================================

                    FieldLabel(
                        stringResource(
                            R.string.discussion_summary_voice
                        ),
                        fieldLabelSize,
                        OutfitFont,
                        FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    VoicePlayerBar(
                        isPlaying = isPlaying,
                        speedMultiplier = speedMultiplier,
                        backgroundColor = appColors.textfield,

                        onPlayPauseClick = {
                            isPlaying = !isPlaying
                        },

                        onSpeedClick = {

                            speedMultiplier =
                                when (speedMultiplier) {
                                    1f -> 1.5f
                                    1.5f -> 2f
                                    else -> 1f
                                }
                        }
                    )


                    Spacer(
                        modifier = Modifier.height(30.dp)
                    )


                    // ====================================================
                    // VIEW MORE
                    // ====================================================

                    ShadowButton(
                        height = 56.dp,
                        color = appColors.popupText,
                        cornerRadius = 28.dp,
                        onClick = {
                            onViewmore()
                        }
                    ) {

                        Text(
                            text =
                                stringResource(
                                    R.string.view_more
                                ),

                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = OutfitFont,
                            color = appColors.pagesText
                        )
                    }


                    Spacer(
                        modifier = Modifier.height(40.dp)
                    )


                    // ====================================================
                    // EDIT + DELETE
                    // ====================================================

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(15.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Box(
                            modifier = Modifier.weight(1f)
                        ) {

                            ShadowButton(
                                height = 56.dp,
                                color = appColors.pagesText,
                                cornerRadius = 30.dp,
                                onClick = {
                                    onEdit()
                                }
                            ) {

                                Text(
                                    text =
                                        stringResource(
                                            R.string.edit
                                        ),

                                    fontSize = 22.sp,
                                    fontFamily = OutfitFont,
                                    fontWeight = FontWeight.Medium,
                                    color = appColors.popupText
                                )
                            }
                        }


                        Box(
                            modifier = Modifier.weight(1f)
                        ) {

                            ShadowButton(
                                height = 56.dp,
                                color = appColors.pagesText,
                                cornerRadius = 30.dp,
                                onClick = {
                                    onDelete()
                                }
                            ) {

                                Text(
                                    text =
                                        stringResource(
                                            R.string.delete_entry
                                        ),

                                    fontSize = 20.sp,
                                    fontFamily = OutfitFont,
                                    fontWeight = FontWeight.Medium,
                                    color = appColors.popupText
                                )
                            }
                        }
                    }


                    Spacer(
                        modifier = Modifier.height(64.dp)
                    )
                }
            }


            // ============================================================
            // HEADER
            // ============================================================

            HeaderSection(
                title = stringResource(
                    R.string.search_result
                ),

                headerHeight = 218.dp,

                textSize = 37.sp,

                spacing = 58.dp,

                bottomspace = 34.dp,

                leaves =
                    appColors.headerDecorOffset2,

                onBack = {
                    onBack()
                }
            )
        }
    }
}
    @Preview(
        showBackground = true,
        showSystemUi = true
    )
    @Composable
    fun SearchResultsPreview() {

        GreenTheme {

            val context = LocalContext.current

            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.loading_4
            )

            SearchResultsContent(
                imageBitmap = bitmap.asImageBitmap()
            )
        }
    }
