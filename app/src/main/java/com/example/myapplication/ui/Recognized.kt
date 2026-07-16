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
import androidx.compose.ui.unit.TextUnit
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
import com.example.myapplication.ui.components.InfoNotePill
import com.example.myapplication.ui.components.RecordConversationField
import com.example.myapplication.ui.components.VoicePlayerBar

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
    imageBitmap: ImageBitmap? = null,  // ← passed from previous screen or DB
    onSubmit: (name: String, relation: String, address: String) -> Unit = { _, _, _ -> }
) {
    val appColors = AppTheme.colors
    var nameText by remember { mutableStateOf("") }
    var relationText by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var speedMultiplier by remember { mutableStateOf(1f) }
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
                        leadingIconRes = R.drawable.relation_icon,
                        height = 52.dp,
                        cornerRadius = 15.dp,
                    )

                    Spacer(modifier = Modifier.height(37.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = "Last Meeting Information",
                            fontSize = 30.sp,
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
                        Column(modifier = Modifier
                            .weight(1f)
                            .padding(top = 16.dp)) {
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

                    Spacer(modifier = Modifier.height(34.dp))

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
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = "Add New Information",
                            fontSize = 30.sp,
                            fontFamily = BaumansFont,
                            fontWeight = FontWeight.Normal,
                            color = appColors.pagesText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 2.dp,
                            color = appColors.pagesText
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        InfoNotePill(text = "* Select location/record conversation or both")
                    }
                    Spacer(modifier = Modifier.height(25.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        // Location
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(top = 16.dp)
                        ) {

                            FieldLabel(
                                "Location *",
                                18.sp,
                                OutfitFont,
                                FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LocationPickerField(
                                value = selectedAddress,
                                placeholder = "open Location in map",
                                onClick = { }
                            )
                        }

                        // Get Location
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {

                            FieldLabel(
                                "Get Location *",
                                17.sp,
                                OutfitFont,
                                FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            ShadowButton(
                                width = 72.dp,
                                height = 51.dp,
                                color = appColors.popupText,
                                cornerRadius = 15.dp,
                                onClick = { }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.location_icon),
                                    contentDescription = null,
                                    tint = appColors.pagesText,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

// ── "OR" divider — thin fading lines either side of centered text ──────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        appColors.pagesText.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(27.dp))


                        // Recording
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.70f)
                        ) {

                            FieldLabel(
                                "Record Conversation *",
                                18.sp,
                                OutfitFont,
                                FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            RecordConversationField(
                                value = selectedAddress,
                                placeholder = "Click to record",
                                onClick = { }
                            )
                        }

                    Spacer(modifier = Modifier.height(28.dp))

                        ShadowButton(
                            height = 56.dp,
                            color = appColors.popupText,
                            cornerRadius = 28.dp,
                            onClick = {
                                onSubmit(nameText, relationText, selectedAddress)
                            }
                        ) {
                            Text(
                                text = "Save",
                                color = appColors.pagesText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = OutfitFont
                            )
                        }

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }

            // ── Header ────────────────────────────────────────────────────────
            HeaderSection(
                title = "Result: ",
                "Recognised",
                218.dp,
                33.sp,
                60.dp,
                (28).dp,
                leaves = 4.dp,
                onBack = { })

        }
    }
}

@Composable
fun DateDisplayField(date: String, backgroundColor: Color, textColor: Color, fontSize: TextUnit) {
    TODO("Not yet implemented")
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecognisedPreview() {
    GreenTheme {
//        val context = LocalContext.current
//        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.loading_4)
        RecognisedContent()
    }
}