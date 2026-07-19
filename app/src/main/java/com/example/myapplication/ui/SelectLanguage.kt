package com.example.myapplication.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont

private val DefaultLanguage  = Color(0xFFDBE1DD)
class SelectLanguageScreen : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GreenTheme {
                SelectLanguageContent(
                    onBack = {}
                )
            }
        }
    }
}

@Composable
fun SelectLanguageContent(
    onBack: () -> Unit
) {

    val appColors = AppTheme.colors

    var selectedLanguage by remember {
        mutableStateOf("English")
    }

    val languages = listOf(
        "English",
        "Urdu"
    )

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
                modifier = Modifier.fillMaxWidth()
            ) {

                Spacer(modifier = Modifier.height(260.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-20).dp),
                    verticalArrangement = Arrangement.spacedBy(37.dp)
                ) {

                    languages.forEach { language ->

                        val isSelected = language == selectedLanguage

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(15.dp),
                                    clip = false
                                )
                                .clickable {
                                    selectedLanguage = language
                                },
                            shape = RoundedCornerShape(15.dp),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    if (isSelected)
                                        appColors.selectedLangBtn
                                    else
                                        appColors.textfield
                            ),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {

                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {

                                androidx.compose.material3.Text(
                                    text = language,
                                    modifier = Modifier
                                        .padding(start = 30.dp)
                                        .align(androidx.compose.ui.Alignment.CenterStart),
                                    fontFamily = MartelFont,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if(isSelected)
                                            appColors.pagesText
                                            else
                                            Color.Black
                                )
                            }
                        }
                    }
                }
            }

            HeaderSection(
                title = "Select Language",
                spacing = 52.dp,
                onBack = onBack
            )

        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun SelectLanguagePreview() {

    GreenTheme {
        SelectLanguageContent(
            onBack = {}
        )
    }
}