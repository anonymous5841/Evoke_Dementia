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
import com.example.myapplication.ui.theme.AppTheme
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
    iconTint: Color = Color.Unspecified,
    textColor: Color
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
                color = textColor // [TEXT COLOR]
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
    onNavigationToHome: () -> Unit = {},
    onSendHelpMessage: () -> Unit = {},
    onEmergencyCall: () -> Unit = {},
    onChangeAppLanguage: () -> Unit = {},
    onChangeAppTheme: () -> Unit = {},
    onBackup: () -> Unit = {},
    onRecoveryData: () -> Unit = {},
    onCompleteDelete: () -> Unit = {},
) {
    val headerHeight = 247.dp // matches profile.kt's headerHeight

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(headerHeight))

                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)                       // ← reduce this fraction to make cards narrower
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp)
                        .offset(y = (-30).dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    val demoItems = listOf(
                        "Person Registration"   to onPersonRegistration,
                        "Person Recognition"    to onPersonRecognition,
                        "Add Location"          to onAddLocation,
                        "Search Location"       to onSearchLocation,
                        "Search Person by name" to onSearchPersonByName,
                        "Navigation to home"    to onNavigationToHome,
                        "Send help message"     to onSendHelpMessage,
                        "Emergency call for help" to onEmergencyCall,
                        "Change app language"   to onChangeAppLanguage,
                        "Change app Theme"      to onChangeAppTheme,
                        "Backup data on cloud"  to onBackup,
                        "Recovery data from cloud" to onRecoveryData,
                        "Completely delete app data" to onCompleteDelete
                    )

                    demoItems.forEach { (title, action) ->
                        if (title.isNotEmpty()) {
                            DemoItem(title = title, onClick = action)
                        }
                    }
                }
            }

            HeaderSection(
                "Demo",
                spacing = 73.dp,
                bottomspace = 44.dp,
                onBack = onBack
            )
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
    val appColors = AppTheme.colors

    ClickableSelectionCard(
        text = title,
        isSelected = false,                        // [SELECTED] always false for demo list
        icon = R.drawable.forward_icon,            // [ICON FILE] change to your arrow xml name
        onClick = onClick,
        selectedColor = appColors.textfield,         // [SELECTED COLOR]
        defaultColor =  appColors.textfield,          // [DEFAULT COLOR] light grey background
        iconTint = appColors.backButton,              // [ICON TINT] green arrow
        textColor = appColors.pagesText,
    )

}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DemoPreview() {
    GreenTheme {
        DemoScreen()
    }
}