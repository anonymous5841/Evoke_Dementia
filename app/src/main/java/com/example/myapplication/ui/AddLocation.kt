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
import com.example.myapplication.ui.theme.BlueTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont
import androidx.compose.ui.res.stringResource

@Composable
fun AddLocationContent(
    onAddClick: () -> Unit = {},
    onBack: () -> Unit = {}

) {
    val appColors = AppTheme.colors
    var titleText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // insets already handled at root
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
                            FieldLabel(stringResource(R.string.location_required))
                            Spacer(modifier = Modifier.height(8.dp))
                            LocationPickerField(
                                value = selectedLocation,
                                placeholder = stringResource(R.string.open_location_in_map),
                                onClick = { /* open map */ }
                            )
                        }

                        // ── Right: Label + button ─────────────────────────────────
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FieldLabel(stringResource(R.string.get_location))
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
                                    contentDescription = stringResource(R.string.map_pin),
                                    tint = appColors.pagesText,
                                    modifier = Modifier.size(35.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Title ─────────────────────────────────────────────────────
                    FieldLabel(stringResource(R.string.title_required))
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        placeholder = stringResource(R.string.title_placeholder),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Description ───────────────────────────────────────────────
                    FieldLabel(stringResource(R.string.description_required))
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        placeholder = stringResource(R.string.description_placeholder),
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
                            text = stringResource(R.string.add),
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
                stringResource(R.string.add_location_header),
                spacing = 57.dp,
                onBack = { onBack()})

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddLocationPreview() {
    BlueTheme {
        AddLocationContent()
    }
}