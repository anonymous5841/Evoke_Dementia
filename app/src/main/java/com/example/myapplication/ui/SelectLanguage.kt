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
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont

private val SelectedLanguage = Color(0xFFB7D4C0)
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

    var selectedLanguage by remember {
        mutableStateOf("English")
    }

    val languages = listOf(
        "English",
        "Urdu"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                Spacer(modifier = Modifier.height(240.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-20).dp),
                    verticalArrangement = Arrangement.spacedBy(35.dp)
                ) {

                    languages.forEach { language ->

                        val isSelected = language == selectedLanguage

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
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
                                        SelectedLanguage
                                    else
                                        DefaultLanguage
                            ),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {

                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {

                                androidx.compose.material3.Text(
                                    text = language,
                                    modifier = Modifier
                                        .padding(start = 20.dp)
                                        .align(androidx.compose.ui.Alignment.CenterStart),
                                    fontFamily = MartelFont,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            HeaderSection(
                title = "Select Language",
                secondaryTitle = null,
                headerHeight = 218.dp,
                textSize = 40.sp,
                spacing = 40.dp,
                bottomspace = 37.dp,
                leaves = (-9).dp,
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