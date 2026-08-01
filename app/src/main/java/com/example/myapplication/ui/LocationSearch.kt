package com.example.myapplication.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.FieldLabel
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.LocationPickerField
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowTextField
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont
import androidx.compose.ui.res.stringResource
import com.example.myapplication.R
import com.example.myapplication.ui.components.rememberBottomContentPadding

class LocationSearch : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LocationSearchContent() }
    }
}


// ── Location picker row ───────────────────────────────────────────────────────
@Composable
fun LocationSearchContent(
    onBack: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
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
                        .offset(y = (-40).dp)  // ← move up by 20dp, adjust as needed
                ) {

                    // ── Location ──────────────────────────────────────────────────
                    FieldLabel(stringResource(R.string.location_required))
                    Spacer(modifier = Modifier.height(8.dp))
                    LocationPickerField(
                        value = selectedLocation,
                        placeholder = stringResource(R.string.open_location_in_map),
                        onClick = { /* open map or location picker here */ },
                        modifier = Modifier.fillMaxWidth(0.70f)
                    )

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

                    // ── Edit / Delete row — now inside the same offset column ──────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        modifier = Modifier.fillMaxWidth()
                        // no extra horizontal padding here — already inside the
                        // 24.dp-padded column, matching Add's single button
                    ) {
                        ShadowButton(
                            modifier = Modifier.weight(1f),
                            height = 56.dp,
                            color = appColors.pagesText,
                            cornerRadius = 30.dp,
                            onClick = { onEdit() }
                        ) {
                            Text(
                                text = stringResource(R.string.edit),
                                color = appColors.popupText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = OutfitFont
                            )
                        }

                        ShadowButton(
                            modifier = Modifier.weight(1f),
                            height = 56.dp,
                            color = appColors.popupText,
                            cornerRadius = 30.dp,
                            onClick = { onDelete() }
                        ) {
                            Text(
                                text = stringResource(R.string.delete),
                                color = appColors.pagesText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = OutfitFont
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(rememberBottomContentPadding()))
            }

            HeaderSection(
                spacing = 68.dp,
                onBack = { onBack()}
            )

        }
    }
}
// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LocationSearchPreview() {
    GreenTheme {
        LocationSearchContent()
    }
}