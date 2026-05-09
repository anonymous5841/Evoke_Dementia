package com.example.myapplication.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import com.example.myapplication.ui.theme.GreenTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.material3.ripple
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import com.example.myapplication.ui.components.BottomNavBar
import com.example.myapplication.ui.components.FieldLabel
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.NavTab
import com.example.myapplication.ui.components.ShadowButton

class LocationSearch : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LocationSearchContent() }
    }
}

// ── Location picker row ───────────────────────────────────────────────────────
@Composable
fun LocationSearchContent(onHomeClick    : () -> Unit = {},
                          onProfileClick : () -> Unit = {}) {
    var titleText        by remember { mutableStateOf("") }
    var descriptionText  by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            HeaderSection(
                onBack = { }
            )

            Spacer(modifier = Modifier.height(0.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-40).dp)  // ← move up by 20dp, adjust as needed
            ){

                // ── Location ──────────────────────────────────────────────────
                FieldLabel("Location *")
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
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
                        ) { /* open map or location picker here */ },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text      = selectedLocation.ifEmpty { "Open location in map" },
                            fontSize  = 16.sp,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines  = 1,
                            overflow  = TextOverflow.Ellipsis,
                            modifier  = Modifier.weight(1f)
                        )
                        // Line separator icon
                        Icon(
                            painter            = painterResource(id = R.drawable.line_icon),
                            contentDescription = null,
                            tint               = Color.Unspecified,
                            modifier           = Modifier
                                .width(25.dp)      // ← very thin
                                .height(40.dp)    // ← match the row height
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
                            shape     = RoundedCornerShape(12.dp),
                            clip      = false
                        )
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OutlinedTextField(
                        value             = titleText,
                        onValueChange     = { titleText = it },
                        interactionSource = titleInteractionSource,  // ← shared interaction source
                        placeholder       = {
                            Text(
                                "Title for place e.g. Restaurant",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .indication(                             // ← ripple on the field itself
                                interactionSource = titleInteractionSource,
                                indication        = ripple(bounded = true)
                            ),
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor   = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor    = Color.Transparent,
                            focusedBorderColor      = MaterialTheme.colorScheme.outline,
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
                            shape     = RoundedCornerShape(12.dp),
                            clip      = false
                        )
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    OutlinedTextField(
                        value             = descriptionText,
                        onValueChange     = { descriptionText = it },
                        interactionSource = descInteractionSource,   // ← shared interaction source
                        placeholder       = {
                            Text(
                                "Enter Description e.g went with him/her",
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 18.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .indication(                             // ← ripple on the field itself
                                interactionSource = descInteractionSource,
                                indication        = ripple(bounded = true)
                            ),
                        shape  = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor   = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor    = Color.Transparent,
                            focusedBorderColor      = MaterialTheme.colorScheme.outline,
                        ),
                        maxLines = 6
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp)
            ) {
                // ── Green Edit Button ─────────────────────────────────────
                ShadowButton(
                    width        = 171.dp,
                    height       = 56.dp,
                    color        = MaterialTheme.colorScheme.primary,
                    cornerRadius = 30.dp,
                    onClick      = { }
                ) {
                    Text(
                        text       = "Edit",
                        color      = MaterialTheme.colorScheme.onPrimary,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // ── Yellow Delete Button ──────────────────────────────────
                ShadowButton(
                    width        = 171.dp,
                    height       = 56.dp,
                    color        = MaterialTheme.colorScheme.secondary,
                    cornerRadius = 30.dp,
                    onClick      = { }
                ) {
                    Text(
                        text       = "Delete",
                        color      = MaterialTheme.colorScheme.onSecondary,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

}


// ── Preview ───────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LocationSearchPreview() {
    GreenTheme {  // ← is this here?
        LocationSearchContent()
    }
}