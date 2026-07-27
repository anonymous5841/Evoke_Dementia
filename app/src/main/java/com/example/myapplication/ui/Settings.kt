package com.example.myapplication.ui

import android.os.Build
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.components.IconWithShadow
import com.example.myapplication.ui.components.SettingsRow
import com.example.myapplication.ui.components.ThemeToggleSwitch
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.BlueAppColors
import com.example.myapplication.ui.theme.GreenAppColors
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont

//class SettingsScreen : ComponentActivity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        setContent {
//            GreenTheme {
//                SettingsContent(
//                    onBack = {},
//                    onSelectLanguage = {}
//                )
//            }
//        }
//    }
//}

@Composable
fun SettingsContent(
    onBack: () -> Unit,
    onSelectLanguage: () -> Unit,
    isBlueTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit
) {

    var showEraseDialog by remember { mutableStateOf(false) }
    val appColors = AppTheme.colors

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
                modifier = Modifier.fillMaxSize()
            ) {

                Spacer(modifier = Modifier.height(270.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-40).dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp) // remove auto spacing, control manually
                ) {

                    SettingsRow(
                        label = stringResource(R.string.cloud_backup),
                        iconRes = R.drawable.ic_backup,
                        iconSize = 38.dp,
                        onClick = { }
                    )

                    Spacer(modifier = Modifier.height(20.dp)) // gap between Cloud Backup and Select Language

                    SettingsRow(
                        label = stringResource(R.string.select_language),
                        iconRes = R.drawable.ic_selectlanguage,
                        onClick = onSelectLanguage
                    )

                    Spacer(modifier = Modifier.height(28.dp)) // larger gap before Select Theme (no card)
                    //select theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.select_theme),
                            modifier = Modifier.weight(1f),
                            fontFamily = MartelFont,
                            fontSize = 22.sp,
                            color = Color.Black
                        )

                        ThemeToggleSwitch(
                            checked = isBlueTheme,
                            onCheckedChange = onThemeToggle,
                            checkedThumbColor = BlueAppColors.toggleColor,
                            uncheckedThumbColor = GreenAppColors.toggleColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp)) // larger gap before Select Theme (no card)

                    Row(
                        modifier = Modifier
                            .clickable { showEraseDialog = true }
                            .padding(start = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconWithShadow(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(35.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(R.string.erase_data),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Red,
                            fontFamily = MartelFont,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.4f), // [SHADOW OPACITY]
                                    offset = Offset(x = 5f, y = 4f),          // [SHADOW OFFSET]
                                    blurRadius = 5f                            // [SHADOW BLUR]
                                )
                            ),
                            modifier = Modifier.padding(top = 15.dp)
                        )
                    }
                }
            }

            HeaderSection(
                title = stringResource(R.string.settings),
                spacing = 65.dp,
                onBack = onBack
            )
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SettingsPreview() {
    var isBlue by remember { mutableStateOf(false) }
    GreenTheme {
        SettingsContent(
            onBack = {},
            onSelectLanguage = {},
            isBlueTheme = isBlue,
            onThemeToggle = { isBlue = it }
        )
    }
}