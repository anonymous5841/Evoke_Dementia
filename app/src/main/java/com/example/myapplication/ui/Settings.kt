package com.example.myapplication.ui
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    onThemeToggle: (Boolean) -> Unit,
    onEraseClick: () -> Unit = {}
) {

    val appColors = AppTheme.colors

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * RESPONSIVE DIMENSIONS
             */

            // Horizontal screen padding
            val horizontalPadding = (maxWidth * 0.06f)
                .coerceIn(20.dp, 32.dp)

            // Space from top before settings content
            val topSpacing = (maxHeight * 0.28f)
                .coerceIn(180.dp, 270.dp)

            // Gap between Cloud Backup and Select Language
            val backupLanguageSpacing = (maxHeight * 0.025f)
                .coerceIn(16.dp, 24.dp)

            // Gap before Select Theme
            val languageThemeSpacing = (maxHeight * 0.035f)
                .coerceIn(24.dp, 32.dp)

            // Gap before Erase Data
            val themeEraseSpacing = (maxHeight * 0.012f)
                .coerceIn(8.dp, 12.dp)


            Column(
                modifier = Modifier
                    .fillMaxSize()

            ) {

                /*
                 * RESPONSIVE TOP POSITION
                 */
                Spacer(
                    modifier = Modifier.height(topSpacing)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),

                    verticalArrangement = Arrangement.Top
                ) {

                    /*
                     * CLOUD BACKUP
                     */

                    SettingsRow(
                        label = stringResource(R.string.cloud_backup),
                        iconRes = R.drawable.ic_backup,
                        iconSize = 38.dp,
                        onClick = {}
                    )


                    /*
                     * CLOUD BACKUP → SELECT LANGUAGE
                     */

                    Spacer(
                        modifier = Modifier.height(
                            backupLanguageSpacing
                        )
                    )


                    /*
                     * SELECT LANGUAGE
                     */

                    SettingsRow(
                        label = stringResource(R.string.select_language),
                        iconRes = R.drawable.ic_selectlanguage,
                        onClick = onSelectLanguage
                    )


                    /*
                     * SELECT LANGUAGE → SELECT THEME
                     */

                    Spacer(
                        modifier = Modifier.height(
                            languageThemeSpacing
                        )
                    )


                    /*
                     * SELECT THEME
                     */

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 20.dp,
                                end = 10.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = stringResource(
                                R.string.select_theme
                            ),

                            modifier = Modifier.weight(1f),

                            fontFamily = MartelFont,
                            fontSize = 22.sp,
                            color = Color.Black
                        )

                        ThemeToggleSwitch(
                            checked = isBlueTheme,
                            onCheckedChange = onThemeToggle,
                            checkedThumbColor =
                                BlueAppColors.toggleColor,
                            uncheckedThumbColor =
                                GreenAppColors.toggleColor
                        )
                    }


                    /*
                     * SELECT THEME → ERASE DATA
                     */

                    Spacer(
                        modifier = Modifier.height(
                            themeEraseSpacing
                        )
                    )


                    /*
                     * ERASE DATA
                     */

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEraseClick()
                            }
                            .padding(start = 20.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        IconWithShadow(
                            painter = painterResource(
                                R.drawable.ic_delete
                            ),

                            contentDescription = null,

                            tint = Color.Red,

                            modifier = Modifier.size(35.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Text(
                            text = stringResource(
                                R.string.erase_data
                            ),

                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Red,
                            fontFamily = MartelFont,

                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black.copy(
                                        alpha = 0.4f
                                    ),
                                    offset = Offset(
                                        x = 5f,
                                        y = 4f
                                    ),
                                    blurRadius = 5f
                                )
                            ),

                            modifier = Modifier.padding(
                                top = 15.dp
                            )
                        )
                    }
                }
            }


            /*
             * HEADER
             * We're leaving this unchanged as requested.
             */

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