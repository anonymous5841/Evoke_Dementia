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
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont

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
    onBack: (() -> Unit)? = null,
    onVoiceSampleClick: () -> Unit = {},
) {
    val appColors = AppTheme.colors

    var nameText by remember { mutableStateOf(initialName) }
    var phoneText by remember { mutableStateOf(initialPhone) }
    val emergencyContacts = remember {
        mutableStateListOf(*initialEmergencyContacts.toTypedArray())
    }
    var selectedAddress by remember { mutableStateOf(initialAddress) }

    Scaffold(containerColor = appColors.background) { innerPadding ->
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
                Spacer(modifier = Modifier.height(218.dp)) // ← match your headerHeight

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-40).dp)
                ) {

                    FieldLabel("Name *", 18.sp, OutfitFont, FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        placeholder = "Enter full name",
                        leadingIconRes = R.drawable.profile_icon,
                    )
                    Spacer(modifier = Modifier.height(39.dp))

                    FieldLabel("Phone Number *", 18.sp, OutfitFont, FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = phoneText,
                        onValueChange = { phoneText = it },
                        placeholder = "Enter phone number",
                        leadingIconRes = R.drawable.phone_icon,
                    )
                    Spacer(modifier = Modifier.height(39.dp))

                    // ── Emergency contacts, list-driven ─────────────────────────
                    emergencyContacts.forEachIndexed { index, contact ->
                        val label = if (index == 0) "Emergency Contact *" else "Emergency Contact ${index + 1}"
                        val placeholder = if (index == 0) "Enter emergency contact" else "Enter emergency contact ${index + 1}"

                        FieldLabel(label, 18.sp, OutfitFont, FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        ShadowTextField(
                            value = contact,
                            onValueChange = { emergencyContacts[index] = it },
                            placeholder = placeholder,
                            leadingIconRes = R.drawable.phone_icon,
                        )
                        Spacer(modifier = Modifier.height(39.dp))
                    }

                    // ── Address * + Add Voice * side by side ──────────────────────
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier
                            .weight(1f)
                            .padding(top = 16.dp)) {
                            FieldLabel("Location *", 20.sp, OutfitFont, FontWeight.Medium)
                            Spacer(modifier = Modifier.height(12.dp))
                            LocationPickerField(
                                value = selectedAddress,
                                placeholder = "Get current location",
                                onClick = { /* open map */ }
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FieldLabel("Add Person's\nvoice sample *", 17.sp, OutfitFont, FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            ShadowButton(
                                width = 72.dp,
                                height = 51.dp,
                                color = appColors.iconSelected,
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

                    Spacer(modifier = Modifier.height(60.dp))

                    // ── Submit button — same shape, different label/handler ──────
                    ShadowButton(
                        height = 56.dp,
                        color = appColors.pagesText,
                        cornerRadius = 30.dp,
                        onClick = {
                            if (mode == PersonFormMode.ADD) {
                                onAdd(nameText, phoneText, emergencyContacts.toList(), selectedAddress)
                            } else {
                                onEdit(nameText, phoneText, emergencyContacts.toList(), selectedAddress)
                            }
                        }
                    ) {
                        Text(
                            text = if (mode == PersonFormMode.ADD) "Add" else "Save Changes",
                            color = appColors.popupText,
                            fontSize = if (mode == PersonFormMode.ADD) 27.sp else 26.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = OutfitFont
                        )
                    }
                }
                Spacer(modifier = Modifier.height(if (mode == PersonFormMode.ADD) 10.dp else 60.dp))
            }

            HeaderSection(
                title = if (mode == PersonFormMode.ADD) "Let us know you" else "Profile",
                spacing = if (mode == PersonFormMode.ADD) 72.dp else 77.dp,
                textSize = if (mode == PersonFormMode.ADD) 39.sp else 44.sp,
                bottomspace = if (mode == PersonFormMode.ADD) 37.dp else 28.dp,
                leaves = 4.dp,
                headerHeight = 218.dp,
                onBack = onBack
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