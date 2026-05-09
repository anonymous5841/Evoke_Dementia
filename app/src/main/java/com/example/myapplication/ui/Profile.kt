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
import com.example.myapplication.ui.components.FieldLabel
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.components.ShadowButtonFull

// ── Mode enum ─────────────────────────────────────────────────────────────────
enum class PersonFormMode { ADD, EDIT }

// ── Activity ──────────────────────────────────────────────────────────────────
class AddPerson : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Pass mode via intent extra — ADD by default
        val mode = intent.getStringExtra("mode")
            ?.let { PersonFormMode.valueOf(it) }
            ?: PersonFormMode.ADD

        // Pass existing data for edit mode via intent extras
        val existingName    = intent.getStringExtra("name")    ?: ""
        val existingPhone   = intent.getStringExtra("phone")   ?: ""
        val existingEc1     = intent.getStringExtra("ec1")     ?: ""
        val existingEc2     = intent.getStringExtra("ec2")     ?: ""
        val existingEc3     = intent.getStringExtra("ec3")     ?: ""
        val existingEc4     = intent.getStringExtra("ec4")     ?: ""
        val existingEc5     = intent.getStringExtra("ec5")     ?: ""
        val existingAddress = intent.getStringExtra("address") ?: ""

        setContent {
            GreenTheme {
                PersonFormContent(
                    mode            = mode,
                    initialName     = existingName,
                    initialPhone    = existingPhone,
                    initialEc1      = existingEc1,
                    initialEc2      = existingEc2,
                    initialEc3      = existingEc3,
                    initialEc4      = existingEc4,
                    initialEc5      = existingEc5,
                    initialAddress  = existingAddress,
                    onAdd           = { name, phone, ec1, ec2, ec3, ec4, ec5, address ->
                        // TODO: save to database

                    },
                    onEdit          = { name, phone, ec1, ec2, ec3, ec4, ec5, address ->
                        // TODO: update in database
                    }
                )
            }
        }
    }
}
// ── Form composable ───────────────────────────────────────────────────────────
@Composable
fun PersonFormContent(
    mode           : PersonFormMode = PersonFormMode.ADD,
    initialName    : String         = "",
    initialPhone   : String         = "",
    initialEc1     : String         = "",
    initialEc2     : String         = "",
    initialEc3     : String         = "",
    initialEc4     : String         = "",
    initialEc5     : String         = "",
    initialAddress : String         = "",
    onAdd          : (String, String, String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onEdit         : (String, String, String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onBack: (() -> Unit)? = null,
) {
    var nameText by remember { mutableStateOf(initialName) }
    var phoneText by remember { mutableStateOf(initialPhone) }
    var emergencyContact1 by remember { mutableStateOf(initialEc1) }
    var emergencyContact2 by remember { mutableStateOf(initialEc2) }
    var emergencyContact3 by remember { mutableStateOf(initialEc3) }
    var emergencyContact4 by remember { mutableStateOf(initialEc4) }
    var emergencyContact5 by remember { mutableStateOf(initialEc5) }
    var selectedAddress by remember { mutableStateOf(initialAddress) }

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

                    // ── Name * ────────────────────────────────────────────────────
                    FieldLabel("Name *")
                    Spacer(modifier = Modifier.height(8.dp))
                    val nameInteractionSource = remember { MutableInteractionSource() }
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
                            value = nameText,
                            onValueChange = { nameText = it },
                            interactionSource = nameInteractionSource,
                            placeholder = {
                                Text(
                                    "Enter full name",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.profile_icon),
                                    contentDescription = "Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .indication(
                                    interactionSource = nameInteractionSource,
                                    indication = ripple(bounded = true)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(39.dp))

                    // ── Phone Number * ────────────────────────────────────────────
                    FieldLabel("Phone Number *")
                    Spacer(modifier = Modifier.height(8.dp))
                    val phoneInteractionSource = remember { MutableInteractionSource() }
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
                            value = phoneText,
                            onValueChange = { phoneText = it },
                            interactionSource = phoneInteractionSource,
                            placeholder = {
                                Text(
                                    "Enter phone number",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.phone_icon),
                                    contentDescription = "Phone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .indication(
                                    interactionSource = phoneInteractionSource,
                                    indication = ripple(bounded = true)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(39.dp))

                    // ── Emergency Contact * ───────────────────────────────────────
                    FieldLabel("Emergency Contact *")
                    Spacer(modifier = Modifier.height(8.dp))
                    val ec1InteractionSource = remember { MutableInteractionSource() }
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
                            value = emergencyContact1,
                            onValueChange = { emergencyContact1 = it },
                            interactionSource = ec1InteractionSource,
                            placeholder = {
                                Text(
                                    "Enter emergency contact",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.phone_icon),
                                    contentDescription = "Phone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .indication(
                                    interactionSource = ec1InteractionSource,
                                    indication = ripple(bounded = true)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(39.dp))

                    // ── Emergency Contact 2 ───────────────────────────────────────
                    FieldLabel("Emergency Contact 2")
                    Spacer(modifier = Modifier.height(8.dp))
                    val ec2InteractionSource = remember { MutableInteractionSource() }
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
                            value = emergencyContact2,
                            onValueChange = { emergencyContact2 = it },
                            interactionSource = ec2InteractionSource,
                            placeholder = {
                                Text(
                                    "Enter emergency contact 2",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.phone_icon),
                                    contentDescription = "Phone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .indication(
                                    interactionSource = ec2InteractionSource,
                                    indication = ripple(bounded = true)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(39.dp))

                    // ── Emergency Contact 3 ───────────────────────────────────────
                    FieldLabel("Emergency Contact 3")
                    Spacer(modifier = Modifier.height(8.dp))
                    val ec3InteractionSource = remember { MutableInteractionSource() }
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
                            value = emergencyContact3,
                            onValueChange = { emergencyContact3 = it },
                            interactionSource = ec3InteractionSource,
                            placeholder = {
                                Text(
                                    "Enter emergency contact 3",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.phone_icon),
                                    contentDescription = "Phone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .indication(
                                    interactionSource = ec3InteractionSource,
                                    indication = ripple(bounded = true)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(39.dp))

                    // ── Emergency Contact 4 ───────────────────────────────────────
                    FieldLabel("Emergency Contact 4")
                    Spacer(modifier = Modifier.height(8.dp))
                    val ec4InteractionSource = remember { MutableInteractionSource() }
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
                            value = emergencyContact4,
                            onValueChange = { emergencyContact4 = it },
                            interactionSource = ec4InteractionSource,
                            placeholder = {
                                Text(
                                    "Enter emergency contact 4",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.phone_icon),
                                    contentDescription = "Phone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .indication(
                                    interactionSource = ec4InteractionSource,
                                    indication = ripple(bounded = true)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(39.dp))

                    // ── Emergency Contact 5 ───────────────────────────────────────
                    FieldLabel("Emergency Contact 5")
                    Spacer(modifier = Modifier.height(8.dp))
                    val ec5InteractionSource = remember { MutableInteractionSource() }
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
                            value = emergencyContact5,
                            onValueChange = { emergencyContact5 = it },
                            interactionSource = ec5InteractionSource,
                            placeholder = {
                                Text(
                                    "Enter emergency contact 5",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 18.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.phone_icon),
                                    contentDescription = "Phone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .indication(
                                    interactionSource = ec5InteractionSource,
                                    indication = ripple(bounded = true)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(39.dp))

                    // ── Address * + Add Voice * side by side ──────────────────────
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Left: label + address field
                        Column(modifier = Modifier.weight(1f).padding(top = 26.dp)) {
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
                                    ) { /* open map */ },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = selectedAddress.ifEmpty { "Get current location" },
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.line_icon),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier
                                            .width(25.dp)
                                            .height(40.dp)
                                            .padding(end = 4.dp)
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.location_icon),
                                        contentDescription = "Map pin",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        // Right: label + microphone button
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FieldLabel("Add Person's\nvoice sample *", 17.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            ShadowButton(
                                width = 72.dp,
                                height = 51.dp,
                                color = MaterialTheme.colorScheme.secondary,
                                cornerRadius = 15.dp,
                                onClick = { /* navigate to voice recording */ }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.microphone_icon),
                                    contentDescription = "Record voice",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp))

                    // ── Buttons — change based on mode ────────────────────────────
                    if (mode == PersonFormMode.ADD) {
                        ShadowButtonFull(
                            height = 56.dp,
                            color = MaterialTheme.colorScheme.primary,
                            cornerRadius = 30.dp,
                            onClick = {
                                onAdd(
                                    nameText, phoneText,
                                    emergencyContact1, emergencyContact2,
                                    emergencyContact3, emergencyContact4,
                                    emergencyContact5, selectedAddress
                                )
                            }
                        ) {
                            Text(
                                text = "Add",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        ShadowButtonFull(
                            height = 56.dp,
                            color = MaterialTheme.colorScheme.primary,
                            cornerRadius = 30.dp,
                            onClick = {
                                onEdit(
                                    nameText, phoneText,
                                    emergencyContact1, emergencyContact2,
                                    emergencyContact3, emergencyContact4,
                                    emergencyContact5, selectedAddress
                                )
                            }
                        ) {
                            Text(
                                text = "Save Changes",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                    }
                }
                Spacer(modifier = Modifier.height(60.dp))

            }
            // Header title changes based on mode
            HeaderSection(
                title = if (mode == PersonFormMode.ADD) "Let us know you" else "Profile",
                spacing = if (mode == PersonFormMode.ADD) 72.dp else 80.dp,
                textSize = if (mode == PersonFormMode.ADD) 34.sp else 39.sp,
                leaves = 9.dp,
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