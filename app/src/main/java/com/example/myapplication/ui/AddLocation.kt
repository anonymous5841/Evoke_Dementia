package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.FieldLabel
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.LocationPickerField
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowTextField
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont

@Composable
fun AddLocationContent(
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val appColors = AppTheme.colors
    var titleText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }

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
                        // ── Left: Label + location display ────────────────────────
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel("Location *")
                            Spacer(modifier = Modifier.height(8.dp))
                            LocationPickerField(
                                value = selectedLocation,
                                placeholder = "Open location in map",
                                onClick = { /* open map */ }
                            )
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
                                color = appColors.popupText,
                                cornerRadius = 15.dp,
                                onClick = { }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.location_icon),
                                    contentDescription = "Map pin",
                                    tint = appColors.pagesText,
                                    modifier = Modifier.size(35.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Title ─────────────────────────────────────────────────────
                    FieldLabel("Title *")
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        placeholder = "Title for place e.g. Restaurant",
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Description ───────────────────────────────────────────────
                    FieldLabel("Description *")
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        placeholder = "Enter Description e.g went with him/her",
                        height = 160.dp,
                        singleLine = false,
                        maxLines = 6,
                    )

                    Spacer(modifier = Modifier.height(55.dp))

                    // ── Green Add Button ───────────────────────────────────────────
                    ShadowButton(
                        height = 56.dp,
                        color = appColors.pagesText,
                        cornerRadius = 30.dp,
                        onClick = onAddClick
                    ) {
                        Text(
                            text = "Add",
                            color = appColors.popupText,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = OutfitFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            HeaderSection(
                "Add Location",
                spacing = 57.dp,
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