package com.example.myapplication.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont

// ══════════════════════════════════════════════════════
// CLICKABLE SELECTION CARD — inline in this file
// Same as your friend's component but defined here
// so no separate import needed
// [CARD HEIGHT] change height(48.dp)
// [CARD CORNER] change RoundedCornerShape(15.dp)
// [SHADOW ELEVATION] change elevation(10.dp)
// [TEXT SIZE] change fontSize(18.sp)
// [TEXT FONT] change fontFamily
// ══════════════════════════════════════════════════════
@Composable
fun ClickableSelectionCard(
    text: String,
    isSelected: Boolean,
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color,
    defaultColor: Color,
    iconTint: Color = Color.Unspecified
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)                         // [CARD HEIGHT] change dp
            .shadow(
                elevation = 10.dp,                 // [SHADOW ELEVATION] change dp
                shape = RoundedCornerShape(15.dp), // [SHADOW SHAPE] matches card corner
                clip = false,                      // shadow renders outside boundary
                ambientColor = Color.LightGray.copy(alpha = 0.3f), // [SHADOW COLOR]
                spotColor = Color.LightGray.copy(alpha = 0.3f)     // [SHADOW SPOT COLOR]
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),         // [CARD CORNER] change dp
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) selectedColor else defaultColor
        ),
        elevation = CardDefaults.cardElevation(8.dp) // [CARD ELEVATION] change dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // [CARD TEXT] title shown on left
            // [TEXT COLOR] change color
            // [TEXT SIZE] change fontSize
            // [TEXT FONT] change fontFamily
            Text(
                text = text,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 20.dp),       // [TEXT LEFT PADDING] change dp
                fontFamily = MartelFont,           // [TEXT FONT]
                fontSize = 18.sp,                  // [TEXT SIZE]
                fontWeight = FontWeight.Normal,    // [TEXT WEIGHT]
                color = MaterialTheme.colorScheme.onBackground // [TEXT COLOR]
            )
            // [CARD ICON] shown on right end
            // [ICON SIZE] no size set — uses icon's natural size
            // [ICON TINT] passed from caller
            Icon(
                painter = painterResource(id = icon), // [ICON FILE]
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp),         // [ICON RIGHT PADDING] change dp
                tint = iconTint                    // [ICON TINT]
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// DEMO SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun DemoScreen(
    onBack: () -> Unit = {},
    onPersonRegistration: () -> Unit = {},
    onPersonRecognition: () -> Unit = {},
    onAddLocation: () -> Unit = {},
    onSearchLocation: () -> Unit = {},
    onSearchPersonByName: () -> Unit = {},
    onNavigationToHome: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // ── Header ────────────────────────────────────
        HeaderSection(
            title = "Demo",
            null,
            308.dp,
            33.sp,
            72.dp,
            (42).dp,
            (-9).dp,
            onBack = { }
        )

        // ── Scrollable list of demo items ─────────────
        // [LIST SPACING] change spacedBy(28.dp)
        // [LIST PADDING] change padding horizontal
        // [LIST OFFSET] change offset y — moves list up/down
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .offset(y = (-110).dp)             // [LIST OFFSET] moves list up into header
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp) // [LIST PADDING]
               .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp) // [LIST SPACING]
        ) {
            val demoItems = listOf(
                "Person Registration"   to onPersonRegistration,
                "Person Recognition"    to onPersonRecognition,
                "Add Location"          to onAddLocation,
                "Search Location"       to onSearchLocation,
                "Search Person by name" to onSearchPersonByName,
                "Navigation to home"    to onNavigationToHome
            )

            demoItems.forEach { (title, action) ->
                if (title.isNotEmpty()) {              // ← ADD THIS — skip empty title cards
                    DemoItem(title = title, onClick = action)
                }
            }
            }
        }
    }

// ══════════════════════════════════════════════════════
// DEMO ITEM — wrapper that calls ClickableSelectionCard
// [CARD COLOR] change defaultColor and selectedColor
// [ICON FILE] change R.drawable.forward_icon
// [ICON TINT] change iconTint color
// ══════════════════════════════════════════════════════
@Composable
fun DemoItem(
    title: String,
    onClick: () -> Unit
) {
    ClickableSelectionCard(
        text = title,
        isSelected = false,                        // [SELECTED] always false for demo list
        icon = R.drawable.forward_icon,            // [ICON FILE] change to your arrow xml name
        onClick = onClick,
        selectedColor = Color(0xFFDBE1DD),         // [SELECTED COLOR]
        defaultColor = Color(0xFFDBE1DD),          // [DEFAULT COLOR] light grey background
        iconTint = Color(0xFF3E634F)               // [ICON TINT] green arrow
    )

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DemoPreview() {
    GreenTheme {
        DemoScreen()
    }
}