package com.example.myapplication.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.R
import com.example.myapplication.ui.components.BottomNavBar
import com.example.myapplication.ui.components.FieldLabel
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowButtonFull

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
    imageBitmap : ImageBitmap? = null,  // ← passed from previous screen or DB
    onSubmit    : (name: String, relation: String, address: String) -> Unit = { _, _, _ -> }
) {
    var nameText        by remember { mutableStateOf("") }
    var relationText    by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Header ────────────────────────────────────────────────────────
            HeaderSection(
                title = "Result: ",
                "Not-Recognised",
                218.dp,
                30.sp,
                35.dp,
                (9).dp,
                onBack = { })

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-30).dp)
            ) {

                FieldLabel("Register", 30.sp)
                Spacer(modifier = Modifier.height(28.dp))


                // ── Image holder ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .width(206.dp)
                        .height(182.dp)
                        .align(Alignment.CenterHorizontally)
                        .shadow(
                            elevation = 6.dp,
                            shape     = RoundedCornerShape(15.dp),
                            clip      = false
                        )
                        .clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        // Show image passed from previous screen or fetched from DB
                        Image(
                            bitmap             = imageBitmap,
                            contentDescription = "Profile photo",
                            contentScale       = ContentScale.Crop,
                            modifier           = Modifier.fillMaxSize()
                        )
                    } else {
                        // Placeholder when no image available
                        Icon(
                            painter            = painterResource(id = R.drawable.profile_icon),
                            contentDescription = "Profile photo",
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Name * ────────────────────────────────────────────────────
                FieldLabel("Name *")
                Spacer(modifier = Modifier.height(8.dp))
                val nameInteractionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(15.dp), clip = false)
                        .clip(RoundedCornerShape(15.dp))
                ) {
                    OutlinedTextField(
                        value             = nameText,
                        onValueChange     = { nameText = it },
                        interactionSource = nameInteractionSource,
                        placeholder       = {
                            Text(
                                "Enter name",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter            = painterResource(id = R.drawable.profile_icon),
                                contentDescription = "Name",
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(22.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .indication(
                                interactionSource = nameInteractionSource,
                                indication        = ripple(bounded = true)
                            ),
                        shape  = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor   = MaterialTheme.colorScheme.surface,
                            focusedContainerColor     = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor      = Color.Transparent,
                            focusedBorderColor        = MaterialTheme.colorScheme.outline,
                            unfocusedTextColor        = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor          = MaterialTheme.colorScheme.onSurface,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedPlaceholderColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(29.dp))

                // ── Relation * ────────────────────────────────────────────────
                FieldLabel("Relation *")
                Spacer(modifier = Modifier.height(8.dp))
                val relationInteractionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(15.dp), clip = false)
                        .clip(RoundedCornerShape(15.dp))
                ) {
                    OutlinedTextField(
                        value             = relationText,
                        onValueChange     = { relationText = it },
                        interactionSource = relationInteractionSource,
                        placeholder       = {
                            Text(
                                "Enter relation e.g. Friend",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter            = painterResource(id = R.drawable.relation_icon),
                                contentDescription = "Relation",
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .indication(
                                interactionSource = relationInteractionSource,
                                indication        = ripple(bounded = true)
                            ),
                        shape  = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor   = MaterialTheme.colorScheme.surface,
                            focusedContainerColor     = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor      = Color.Transparent,
                            focusedBorderColor        = MaterialTheme.colorScheme.outline,
                            unfocusedTextColor        = MaterialTheme.colorScheme.onSurface,
                            focusedTextColor          = MaterialTheme.colorScheme.onSurface,
                            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedPlaceholderColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                // ── Address * + Add Voice * side by side ──────────────────────
                Row(
                    verticalAlignment     = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    // Left: label + address field
                    Column(modifier = Modifier.weight(1f).padding(top=26.dp)) {
                        FieldLabel("Location *")
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape     = RoundedCornerShape(12.dp),
                                    clip      = false
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = ripple(bounded = true)
                                ) { /* open map */ },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier          = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text     = selectedAddress.ifEmpty { "Get current location" },
                                    fontSize = 16.sp,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    painter            = painterResource(id = R.drawable.line_icon),
                                    contentDescription = null,
                                    tint               = Color.Unspecified,
                                    modifier           = Modifier
                                        .width(25.dp)
                                        .height(40.dp)
                                        .padding(end = 4.dp)
                                )
                                Icon(
                                    painter            = painterResource(id = R.drawable.location_icon),
                                    contentDescription = "Map pin",
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    // Right: label + microphone button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FieldLabel("Add Person's\nvoice sample *", 17.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ShadowButton(
                            width        = 72.dp,
                            height       = 51.dp,
                            color        = MaterialTheme.colorScheme.secondary,
                            cornerRadius = 15.dp,
                            onClick      = { /* navigate to voice recording */ }
                        ) {
                            Icon(
                                painter            = painterResource(id = R.drawable.microphone_icon),
                                contentDescription = "Record voice",
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(55.dp))

                // ── Save + Score buttons side by side ─────────────────────────
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(0.5f)) {
                        ShadowButtonFull(
                            height       = 56.dp,
                            color        = MaterialTheme.colorScheme.secondary,
                            cornerRadius = 28.dp,
                            onClick      = {
                                onSubmit(nameText, relationText, selectedAddress)
                            }
                        ) {
                            Text(
                                text       = "Save",
                                color      = MaterialTheme.colorScheme.onSecondary,
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        ShadowButtonFull(
                            height       = 56.dp,
                            color        = MaterialTheme.colorScheme.primary,
                            cornerRadius = 28.dp,
                            onClick      = { /* score action */ }
                        ) {
                            Text(
                                text       = "Record Conversation",
                                color      = MaterialTheme.colorScheme.onPrimary,
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun NotRecognisedPreview() {
    GreenTheme {
        // ── To test with a drawable image from res/drawable ──────────────────
        // Uncomment the lines below and replace R.drawable.your_image
        // with your actual image resource name

         val context = LocalContext.current
         val bitmap  = BitmapFactory.decodeResource(context.resources, R.drawable.loading_4)
         NotRecognisedContent(imageBitmap = bitmap.asImageBitmap())

        // ── Default preview with placeholder ──────────────────────────────────
//        NotRecognisedContent()
    }
}