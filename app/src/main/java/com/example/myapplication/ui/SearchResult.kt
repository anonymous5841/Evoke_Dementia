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
    imageBitmap: ImageBitmap? = null,  // ← passed from previous screen or DB
    onSubmit: (name: String, relation: String, address: String) -> Unit = { _, _, _ -> }
) {
    var nameText by remember { mutableStateOf("") }
    var relationText by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableStateOf(1f) }
    val appColors = AppTheme.colors
    var textAlign by remember { mutableStateOf(TextAlign.Left) }

    Scaffold(
        containerColor = appColors.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(modifier = Modifier.height(218.dp))  // ← match your headerHeight

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-40).dp)

                ) {


                    Spacer(modifier = Modifier.height(28.dp))

                    // ── Image holder ──────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .width(206.dp)
                            .height(182.dp)
                            .align(Alignment.CenterHorizontally)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(15.dp),
                                clip = false
                            )
                            .clip(RoundedCornerShape(15.dp))
                            .background(appColors.textfield),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.profile_icon),
                                contentDescription = "Profile photo",
                                tint = appColors.backButton,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── Name * ────────────────────────────────────────────────────
                    FieldLabel("Name *", 18.sp, OutfitFont, FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        placeholder = "Enter name",
                        leadingIconRes = R.drawable.profile_icon,
                        height = 52.dp,
                        cornerRadius = 15.dp,
                    )

                    Spacer(modifier = Modifier.height(29.dp))

                    // ── Relation * ────────────────────────────────────────────────
                    FieldLabel("Relation *", 18.sp, OutfitFont, FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = relationText,
                        onValueChange = { relationText = it },
                        placeholder = "Enter relation e.g. Friend",
                        leadingIconRes = R.drawable.ic_call,
                        height = 52.dp,
                        cornerRadius = 15.dp,
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Last Meeting Information",
                            fontSize = 32.sp,
                            fontFamily = BaumansFont,
                            fontWeight = FontWeight.Normal,
                            color = appColors.pagesText,
                            textAlign = textAlign,
                            modifier = Modifier.fillMaxWidth(),   // ← required — textAlign has no effect unless the Text is wider than its own content
                            onTextLayout = { result ->
                                textAlign = if (result.lineCount > 1) TextAlign.Center else TextAlign.Left
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 2.dp,
                            color = appColors.pagesText
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    // ── Address * + Add Voice * side by side ──────────────────────
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Left: label + address field
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 16.dp)
                        ) {
                            FieldLabel("Location *", 18.sp, OutfitFont, FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))
                            LocationPickerField(
                                value = selectedAddress,
                                placeholder = "Get current location",
                                onClick = { /* open map */ }
                            )
                        }

                        Column(
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            FieldLabel(
                                "Date",
                                18.sp,
                                OutfitFont,
                                FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            DateDisplayField(
                                date = "26 Jun 2026",
                                backgroundColor = appColors.textfield,   // or whichever field fits your design
                                textColor = appColors.pagesText,
                                fontSize = 18.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(36.dp))

                    FieldLabel(
                        "Discussion Summary",
                        18.sp,
                        OutfitFont,
                        FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DiscussionSummaryBox()

                    Spacer(modifier = Modifier.height(28.dp))

                    FieldLabel(
                        "Discussion Summary in Voice",
                        18.sp,
                        OutfitFont,
                        FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    VoicePlayerBar(
                        isPlaying = isPlaying,
                        speedMultiplier = speedMultiplier,
                        backgroundColor = appColors.textfield,
                        onPlayPauseClick = {
                            isPlaying = !isPlaying
                        },
                        onSpeedClick = {
                            speedMultiplier = when (speedMultiplier) {
                                1f -> 1.5f
                                1.5f -> 2f
                                else -> 1f
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(30.dp))

                    ShadowButton(
                        height = 56.dp,
                        color = appColors.popupText,
                        cornerRadius = 28.dp,
                        onClick = { }
                    ) {
                        Text(
                            text = "View More",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = OutfitFont,
                            color = appColors.pagesText
                        )
                    }


                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        // Edit Button
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {

                            ShadowButton(
                                height = 56.dp,
                                color = appColors.pagesText,
                                cornerRadius = 30.dp,
                                onClick = { }
                            ) {

                                Text(
                                    text = "Edit",
                                    fontSize = 22.sp,
                                    fontFamily = OutfitFont,
                                    fontWeight = FontWeight.Medium,
                                    color = appColors.popupText
                                )
                            }
                        }

                        // Delete Button
                        Box(
                            modifier = Modifier.weight(1f)
                        ) {

                            ShadowButton(
                                height = 56.dp,
                                color = appColors.pagesText,
                                cornerRadius = 30.dp,
                                onClick = { }
                            ) {

                                Text(
                                    text = "Delete Entry",
                                    fontSize = 20.sp,
                                    fontFamily = OutfitFont,
                                    fontWeight = FontWeight.Medium,
                                    color = appColors.popupText,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
                    // ── Header ────────────────────────────────────────────────────────
                    HeaderSection(
                        title = "Search Result",
                        headerHeight = 218.dp,
                        textSize = 37.sp,
                        spacing = 58.dp,
                        bottomspace = 34.dp,
                        leaves = 4.dp,
                        onBack = { })
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
