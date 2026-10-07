package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.example.myapplication.ui.components.rememberBottomContentPadding
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.BlueTheme
import com.example.myapplication.ui.theme.OutfitFont

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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * RESPONSIVE DIMENSIONS
             * (24.dp padding / 92.dp button were the design values)
             */

            // Same rule as the other screens; 24.dp on a ~400.dp wide phone
            val horizontalPadding = (maxWidth * 0.06f)
                .coerceIn(20.dp, 32.dp)

            val contentWidth = maxWidth - horizontalPadding * 2

            // Right column (Get Location label + button) gets a fixed share of the
            // width. Before it grew to fit the label on one line and squeezed the
            // location field on small screens. The label wraps to 2 lines if needed.
            val getLocationColumnWidth = (contentWidth * 0.32f)
                .coerceIn(110.dp, 130.dp)

            val getLocationButtonWidth = (contentWidth * 0.26f)
                .coerceIn(80.dp, 92.dp)
                .coerceAtMost(getLocationColumnWidth)

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
                        .padding(horizontal = horizontalPadding)
                        .offset(y = (-40).dp)
                ) {

                    // Bottom-aligned: the location field and the button always line
                    // up, even when the right label wraps to 2 lines.
                    Row(
                        verticalAlignment = Alignment.Bottom,
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
                        // Fixed width, so the full-width FieldLabel is safe here.
                        Column(
                            modifier = Modifier.width(getLocationColumnWidth),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // One line: shrinks to fit the narrow column
                            // (the Urdu label used to wrap onto 2 lines).
                            FieldLabel(
                                stringResource(R.string.get_location),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ShadowButton(
                                width = getLocationButtonWidth,
                                // Same height as LocationPickerField (52.dp) so the
                                // edges match (51.dp was resized by ShadowButton).
                                height = 52.dp,
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

                Spacer(modifier = Modifier.height(rememberBottomContentPadding()))
            }

            HeaderSection(
                stringResource(R.string.add_location_header),
                spacing = 57.dp,
                onBack = { onBack() }
            )
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
