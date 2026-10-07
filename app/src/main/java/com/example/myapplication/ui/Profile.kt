package com.example.myapplication.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.myapplication.ui.components.rememberBottomNavBarHeight
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont
import androidx.compose.ui.res.stringResource
// ── Mode enum ─────────────────────────────────────────────────────────────────
enum class PersonFormMode { ADD, EDIT }

// ── Activity ──────────────────────────────────────────────────────────────────
class AddPerson : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra("mode")
            ?.let { PersonFormMode.valueOf(it) }
            ?: PersonFormMode.ADD

        val existingName = intent.getStringExtra("name") ?: ""
        val existingPhone = intent.getStringExtra("phone") ?: ""
        val existingEcs = listOf("ec1", "ec2", "ec3", "ec4", "ec5")
            .map { intent.getStringExtra(it) ?: "" }
        val existingAddress = intent.getStringExtra("address") ?: ""

        setContent {
            GreenTheme {
                PersonFormContent(
                    mode = mode,
                    initialName = existingName,
                    initialPhone = existingPhone,
                    initialEmergencyContacts = existingEcs,
                    initialAddress = existingAddress,
                    onAdd = { name, phone, ecs, address ->
                        // TODO: save to database
                    },
                    onEdit = { name, phone, ecs, address ->
                        // TODO: update in database
                    },
                )
            }
        }
    }
}

// ── Form composable ───────────────────────────────────────────────────────────
@Composable
fun PersonFormContent(
    mode: PersonFormMode = PersonFormMode.ADD,
    initialName: String = "",
    initialPhone: String = "",
    initialEmergencyContacts: List<String> = listOf("", "", "", "", ""),
    initialAddress: String = "",
    onAdd: (String, String, List<String>, String) -> Unit = { _, _, _, _ -> },
    onEdit: (String, String, List<String>, String) -> Unit = { _, _, _, _ -> },
    onBack:  () -> Unit = {},
    onVoiceSampleClick: () -> Unit = {},
) {
    val appColors = AppTheme.colors
    var nameText by remember { mutableStateOf(initialName) }
    var phoneText by remember { mutableStateOf(initialPhone) }
    val emergencyContacts = remember {
        mutableStateListOf(*initialEmergencyContacts.toTypedArray())
    }
    var selectedAddress by remember { mutableStateOf(initialAddress) }
    val bottomPadding = rememberBottomNavBarHeight()

    Scaffold(containerColor = appColors.background,
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

                Spacer(modifier = Modifier.height(218.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-40).dp)
                ) {

                    // ============================================================
                    // NAME, PHONE & EMERGENCY CONTACTS
                    // FieldLabel + ShadowTextField switch to the right in Urdu
                    // by themselves — no CompositionLocalProvider needed here.
                    // ============================================================

                    // ---------------- NAME ----------------

                    FieldLabel(
                        text = stringResource(R.string.name_label),
                        textsize = 18.sp,
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ShadowTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        placeholder = stringResource(R.string.enter_full_name),
                        leadingIconRes = R.drawable.profile_icon,
                    )

                    Spacer(modifier = Modifier.height(39.dp))


                    // ---------------- PHONE ----------------

                    FieldLabel(
                        text = stringResource(R.string.phone_number_label),
                        textsize = 18.sp,
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ShadowTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        placeholder = stringResource(R.string.enter_phone_number),
                        leadingIconRes = R.drawable.phone_icon,
                    )

                    Spacer(modifier = Modifier.height(39.dp))


                    // ---------------- EMERGENCY CONTACTS ----------------

                    emergencyContacts.forEachIndexed { index, contact ->

                        val label = if (index == 0) {
                            stringResource(R.string.emergency_contact_label)
                        } else {
                            stringResource(
                                R.string.emergency_contact_label_format,
                                index + 1
                            )
                        }

                        val placeholder = if (index == 0) {
                            stringResource(R.string.enter_emergency_contact)
                        } else {
                            stringResource(
                                R.string.enter_emergency_contact_format,
                                index + 1
                            )
                        }

                        FieldLabel(
                            text = label,
                            textsize = 18.sp,
                            fontFamily = OutfitFont,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ShadowTextField(
                            value = contact,
                            onValueChange = {
                                emergencyContacts[index] = it
                            },
                            placeholder = placeholder,
                            leadingIconRes = R.drawable.phone_icon,
                        )

                        Spacer(modifier = Modifier.height(39.dp))
                    }


                    // ============================================================
                    // LOCATION + VOICE SAMPLE
                    // Row order stays the same in both languages; only the
                    // labels and the location field content follow the language.
                    // ============================================================

                    // Bottom-aligned: the location field and the mic button stay
                    // level even when the voice label needs 2–3 lines (Urdu).
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        // LOCATION
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 45.dp),   // was height(45.dp)
                                contentAlignment = Alignment.BottomStart
                            ) {
                                FieldLabel(
                                    text = stringResource(R.string.location_label),
                                    textsize = 20.sp,
                                    fontFamily = OutfitFont,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LocationPickerField(
                                value = selectedAddress,
                                placeholder = stringResource(R.string.get_current_location),
                                onClick = { /* open map */ }
                            )
                        }


                        // VOICE SAMPLE
                        // Fixed width column, so a full-width FieldLabel is safe here.
                        Column(
                            modifier = Modifier.width(120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            // heightIn instead of a fixed 45.dp: a fixed height cut
                            // off the 2nd line of the Urdu label (Urdu letters are
                            // much taller than English ones).
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 45.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                FieldLabel(
                                    text = stringResource(R.string.add_person_voice_sample),
                                    textsize = 17.sp,
                                    fontFamily = OutfitFont,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            ShadowButton(
                                width = 72.dp,
                                // Same height as LocationPickerField (52.dp) so both
                                // line up (51.dp is resized to ~48.dp by ShadowButton).
                                height = 52.dp,
                                color = appColors.iconSelected,
                                cornerRadius = 15.dp,
                                onClick = { onVoiceSampleClick() }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.microphone_icon),
                                    contentDescription = stringResource(R.string.record),
                                    tint = appColors.pagesText,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // ============================================================
                    // SUBMIT BUTTON
                    // ============================================================

                    Spacer(modifier = Modifier.height(60.dp))

                    ShadowButton(
                        height = 56.dp,
                        color = appColors.pagesText,
                        cornerRadius = 30.dp,
                        onClick = {

                            if (mode == PersonFormMode.ADD) {

                                onAdd(
                                    nameText,
                                    phoneText,
                                    emergencyContacts.toList(),
                                    selectedAddress
                                )

                            } else {

                                onEdit(
                                    nameText,
                                    phoneText,
                                    emergencyContacts.toList(),
                                    selectedAddress
                                )
                            }
                        }
                    ) {
                        Text(
                            text = if (mode == PersonFormMode.ADD) {
                                stringResource(R.string.add_button)
                            } else {
                                stringResource(R.string.save_changes)
                            },
                            color = appColors.popupText,
                            fontSize = if (mode == PersonFormMode.ADD) {
                                27.sp
                            } else {
                                26.sp
                            },
                            fontWeight = FontWeight.Medium,
                            fontFamily = OutfitFont
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(
                        if (mode == PersonFormMode.ADD) {
                            10.dp
                        } else {
                            bottomPadding
                        }
                    )
                )
            }

            HeaderSection(
                title = if (mode == PersonFormMode.ADD)
                    stringResource(R.string.let_us_know_you)
                else
                    stringResource(R.string.profile),
                spacing = if (mode == PersonFormMode.ADD) 72.dp else 77.dp,
                textSize = if (mode == PersonFormMode.ADD) 39.sp else 44.sp,
                bottomspace = if (mode == PersonFormMode.ADD) 37.dp else 28.dp,
                leaves = appColors.headerDecorOffset2,
                headerHeight = 218.dp,
                onBack = { onBack() }
            )
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddPersonPreview() {
    GreenTheme {
        PersonFormContent(mode = PersonFormMode.EDIT)
    }
}
