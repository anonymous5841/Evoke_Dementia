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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.FieldLabel
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.InfoNotePill
import com.example.myapplication.ui.components.LocationPickerField
import com.example.myapplication.ui.components.RecordConversationField
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowTextField
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.BaumansFont
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont

class NotRecognisedScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GreenTheme {
                NotRecognisedContent()
            }
        }
    }
}

@Composable
fun NotRecognisedContent(
    imageBitmap: ImageBitmap? = null,  // ← passed from previous screen or DB
    onSubmit: (name: String, relation: String, address: String) -> Unit = { _, _, _ -> },
    onVoiceSampleClick: () -> Unit = {},   // ← add this
    onBack: (() -> Unit)? = null,
) {
    val appColors = AppTheme.colors
    var nameText by remember { mutableStateOf("") }
    var relationText by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

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

                    FieldLabel("Register", 32.sp, BaumansFont, FontWeight.Normal)
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

                    Spacer(modifier = Modifier.height(36.dp))

                    InfoNotePill(text = "* Select location/record conversation or both")

                    Spacer(modifier = Modifier.height(21.dp))

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

                        // Right: label + microphone button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FieldLabel("Add Person's\nvoice sample *", 17.sp, OutfitFont, FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            ShadowButton(
                                width = 72.dp,
                                height = 51.dp,
                                color = appColors.popupText,
                                cornerRadius = 15.dp,
                                onClick = { onVoiceSampleClick() }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.microphone_icon),
                                    contentDescription = "Record voice",
                                    tint = appColors.pagesText,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

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

                    Spacer(modifier = Modifier.height(32.dp))

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
                title = "Result:",
                "Not-Recognised",
                218.dp,
                33.sp,
                41.dp,
                (28).dp,
                leaves = 4.dp,
                onBack = { })

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotRecognisedPreview() {
    GreenTheme {
        val context = LocalContext.current
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.loading_4)
        NotRecognisedContent(imageBitmap = bitmap.asImageBitmap())
    }
}