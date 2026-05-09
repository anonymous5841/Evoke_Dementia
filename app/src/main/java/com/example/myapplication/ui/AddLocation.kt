package com.example.myapplication.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.horizontalScroll
import com.example.myapplication.ui.components.NavTab


@Composable
fun AddLocationContent(    onHomeClick    : () -> Unit = {},
                           onProfileClick : () -> Unit = {},
                           onAddClick     : () -> Unit = {}   // ← add this

) {

    var titleText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }

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
                // Spacer pushes content down below header height
                Spacer(modifier = Modifier.height(260.dp))  // ← match your headerHeight


                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-40).dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // ── Left: Label + text field ──────────────────────────────
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Location *")
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .shadow(
                                        elevation = 6.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        clip = false
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = true)
                                    ) { },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = selectedLocation.ifEmpty { "Open location in map" },
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // ── Right: Label + button ─────────────────────────────────
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FieldLabel("Get Location *")
                            Spacer(modifier = Modifier.height(8.dp))
                            ShadowButton(
                                width = 92.dp,
                                height = 51.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                cornerRadius = 15.dp,
                                onClick = { }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.location_icon),
                                    contentDescription = "Map pin",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(35.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Title ─────────────────────────────────────────────────────
                    FieldLabel("Title *")
                    Spacer(modifier = Modifier.height(8.dp))
                    val titleInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(12.dp),
                                clip = false
                            )
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        OutlinedTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            interactionSource = titleInteractionSource,
                            placeholder = {
                                Text(
                                    "Title for place e.g. Restaurant",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .indication(
                                    interactionSource = titleInteractionSource,
                                    indication = ripple(bounded = true)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Description ───────────────────────────────────────────────
                    FieldLabel("Description *")
                    Spacer(modifier = Modifier.height(8.dp))
                    val descInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(12.dp),
                                clip = false
                            )
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        OutlinedTextField(
                            value = descriptionText,
                            onValueChange = { descriptionText = it },
                            interactionSource = descInteractionSource,
                            placeholder = {
                                Text(
                                    "Enter Description e.g went with him/her",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .indication(
                                    interactionSource = descInteractionSource,
                                    indication = ripple(bounded = true)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                            maxLines = 6
                        )
                    }

                    Spacer(modifier = Modifier.height(55.dp))

                    // ── Green Add Button ───────────────────────────────────────────
                    ShadowButtonFull(
                        height = 56.dp,
                        color = MaterialTheme.colorScheme.primary,
                        cornerRadius = 30.dp,
                        onClick = onAddClick
                    ) {
                        Text(
                            text = "Add",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            HeaderSection(
                "Add Location",
                spacing = 65.dp,
                onBack = { })

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddLocationPreview() {
    GreenTheme {
        AddLocationContent()
    }
}