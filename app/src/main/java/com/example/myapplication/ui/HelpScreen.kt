package com.example.myapplication.ui
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.BottomNavBar
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.theme.MartelFont

@Composable
fun HelpScreen(
    onBack: () -> Unit = {},
    onSendMessage: () -> Unit = {},
    onMapToHome: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // ── Header ────────────────────────────────────────────────────────
        HeaderSection(
            title = "Help",
            null,
            308.dp,
            33.sp,
            72.dp,
            (42).dp,
            (-9).dp,
            onBack = { })
        // Main Content Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .offset(y = (-70).dp)              // pulls content up to overlap header's extra space
                .padding(horizontal = 20.dp)
                .padding(top = 5.dp, bottom = 70.dp),  // bottom padding matches the offset, prevents content loss
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                HelpCard(
                    iconRes = R.drawable.message_icon,
                    title = "Send Message", //Martel
                    onClick = onSendMessage

                )

                HelpCard(
                    iconRes = R.drawable.map_to_home_icon,
                    title = "Map to Home", //Martel
                    onClick = onMapToHome
                )
            }
        }

        // Bottom Navigation
    }
}

@Composable
fun HelpCard(
    iconRes: Int,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .height(125.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFDBE1DD))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,          // changed — was Color(0xFF1A1A1A)
            modifier = Modifier.size(50.dp, 45.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MartelFont,
            color = Color.Black
        )
    }
}