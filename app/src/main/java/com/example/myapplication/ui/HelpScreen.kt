package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont

@Composable
fun HelpScreen(
    onBack: () -> Unit = {},
    onSendMessage: () -> Unit = {},
    onMapToHome: () -> Unit = {}
) {
    val appColors = AppTheme.colors
    val headerHeight = 260.dp // matches profile.kt's headerHeight — adjust if HelpScreen's header renders taller/shorter

    Scaffold(containerColor = appColors.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Scrollable content — full screen, stretches all the way to the bottom ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(headerHeight))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-30).dp), // small nudge so it peeks under the header's curved edge
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        HelpCard(
                            iconRes = R.drawable.message_icon,
                            title = "Send Message",
                            onClick = onSendMessage
                        )

                        HelpCard(
                            iconRes = R.drawable.map_to_home_icon,
                            title = "Map to Home",
                            onClick = onMapToHome,
                            iconWidth = 66.dp,
                            iconHeight = 56.dp
                        )
                    }
                }
            }

            // ── Header drawn on top, overlapping the scrolled content ───────────
            HeaderSection(
                "Help",
                spacing = 74.dp,
                bottomspace = 44.dp,
                onBack = onBack
            )
        }
    }
}

@Composable
fun HelpCard(
    iconRes: Int,
    title: String,
    onClick: () -> Unit,
    iconWidth: Dp = 55.dp,
    iconHeight: Dp = 47.dp
) {
    val appColors = AppTheme.colors
    Column(
        modifier = Modifier
            .width(170.dp)
            .height(125.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(appColors.textfield)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = AppTheme.colors.backButton,
            modifier = Modifier.size(iconWidth, iconHeight)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            fontSize = 19.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MartelFont,
            color = Color.Black
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HelpScreenPreview() {
    GreenTheme {
        HelpScreen()
    }
}