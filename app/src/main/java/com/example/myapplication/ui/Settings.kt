package com.example.myapplication.ui

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.SettingsRow
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont

class SettingsScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreenTheme {
                SettingsContent(
                    onBack = {},
                    onSelectLanguage = {}
                )
            }
        }
    }
}

@Composable
fun SettingsContent(
    onBack: () -> Unit,
    onSelectLanguage: () -> Unit
) {

    var darkThemeEnabled by remember { mutableStateOf(false) }
    var showEraseDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                Spacer(modifier = Modifier.height(218.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-40).dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp) // remove auto spacing, control manually
                ) {

                    SettingsRow(
                        label = "Cloud Backup",
                        iconRes = R.drawable.ic_backup,
                        iconSize = 35.dp,
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(20.dp)) // gap between Cloud Backup and Select Language

                    SettingsRow(
                        label = "Select Language",
                        iconRes = R.drawable.ic_selectlanguage,
                        onClick = onSelectLanguage
                    )

                    Spacer(modifier = Modifier.height(28.dp)) // larger gap before Select Theme (no card)
                        //select theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Theme",
                            modifier = Modifier.weight(1f),
                            fontFamily = MartelFont,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Switch(
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = 1.5f,   // wider
                                    scaleY = 1.3f      // same height
                                )
                                .offset(x = (-6).dp), // move slightly left
                            checked = darkThemeEnabled,
                            onCheckedChange = { darkThemeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.surface,
                                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(23.dp)) // gap before Erase Data

                    Row(
                        modifier = Modifier
                            .clickable { showEraseDialog = true }
                            .padding(start = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
                            contentScale = ContentScale.Fit
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Erase Data",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
            }

        HeaderSection(
            title = "Settings",
            secondaryTitle = null,
            headerHeight = 218.dp,
            textSize = 40.sp,
            spacing = 60.dp,
            bottomspace = 37.dp,
            leaves = (-9).dp,
            onBack = onBack
        )
    }

}


        @Preview(
            showBackground = true,
            showSystemUi = true
        )
        @Composable
        private fun SettingsPreview() {

            GreenTheme {

                SettingsContent(
                    onBack = {},
                    onSelectLanguage = {}
                )

            }
        }