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
import com.example.myapplication.ui.components.DiscussionVoiceSnippet
import com.example.myapplication.ui.components.FieldLabel
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.LocationPickerField
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowTextField
import com.example.myapplication.ui.theme.BaumansFont
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont

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
    var nameText by remember { mutableStateOf("") }
    var relationText by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                            .background(MaterialTheme.colorScheme.surface),
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
                                tint = MaterialTheme.colorScheme.primary,
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

                    Spacer(modifier = Modifier.height(35.dp))
                    Text(
                        text = "Last Meeting Information",
                        fontSize = 32.sp,
                        fontFamily = BaumansFont,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                                date = "26 Jun 2026"
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

                    DiscussionVoiceSnippet()

                    Spacer(modifier = Modifier.height(30.dp))

                    ShadowButton(
                        height = 56.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        cornerRadius = 28.dp,
                        onClick = { }
                    ) {
                        Text(
                            text = "View More",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = OutfitFont,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text = "Add Voice or Conversation",
                            fontSize = 30.sp,
                            fontFamily = BaumansFont,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.height(28.dp))

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

                            Spacer(modifier = Modifier.height(8.dp))

                            ShadowButton(
                                width = 72.dp,
                                height = 51.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                cornerRadius = 15.dp,
                                onClick = { }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.location_icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Box(modifier = Modifier.weight(1f)) {

                            ShadowButton(
                                height = 56.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                cornerRadius = 30.dp,
                                onClick = { }
                            ) {

                                Text(
                                    "Save",
                                    fontSize = 22.sp,
                                    fontFamily = OutfitFont,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1.3f)) {

                            ShadowButton(
                                height = 56.dp,
                                color = MaterialTheme.colorScheme.primary,
                                cornerRadius = 30.dp,
                                onClick = { }
                            ) {

                                Text(
                                    "Record Conversation",
                                    fontSize = 18.sp,
                                    fontFamily = OutfitFont,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
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
                37.dp,
                (13).dp,
                onBack = { })

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecognisedPreview() {
    GreenTheme {
        val context = LocalContext.current
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.loading_4)
        RecognisedContent(imageBitmap = bitmap.asImageBitmap())
    }
}