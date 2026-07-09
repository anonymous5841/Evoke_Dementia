package com.example.myapplication.ui
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.myapplication.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont

// ─── Extracted components — import these ───────────────────────────────────
import com.example.myapplication.ui.components.MenuItemData
import com.example.myapplication.ui.components.RowContent
import com.example.myapplication.ui.components.RotatingRowGrid
import com.example.myapplication.ui.components.White
import com.example.myapplication.ui.components.TextDark

@Composable
fun HomeScreen(
    onSettingsClick     : () -> Unit,
    onSearchClick       : () -> Unit = {},
    onRecognizeClick    : () -> Unit = {},
    onHelpClick         : () -> Unit = {},
    onDemoClick         : () -> Unit = {},
    onAddLocationClick  : () -> Unit = {},
    initialPage         : Int = 0,
    startExpanded       : Boolean = false
) {

    // Fixed row identities — top row is the "big" pair, then medium, then small.
    val rowsData = remember {
        listOf(
            RowContent(
                left  = MenuItemData(
                    "Recognize \n/\nRegister", R.drawable.ic_recognize,
                    iconSizeSmall = 50.dp, iconSizeBig = 50.dp,
                    fontSizeSmall = 20.sp, fontSizeBig = 20.sp
                ) { onRecognizeClick() },
                right = MenuItemData(
                    "Add location", R.drawable.ic_location,
                    iconSizeSmall = 44.dp, iconSizeBig = 44.dp,
                    fontSizeSmall = 20.sp, fontSizeBig = 20.sp
                ) { onAddLocationClick() }
            ),
            RowContent(
                left  = MenuItemData(
                    "Help", R.drawable.ic_help,
                    iconSizeSmall = 45.dp, iconSizeBig = 45.dp,
                    fontSizeSmall = 22.sp, fontSizeBig = 22.sp
                ) { onHelpClick() },
                right = MenuItemData(
                    "Search\n\nName / Location", R.drawable.ic_search,
                    iconSizeSmall = 20.dp, iconSizeBig = 50.dp,
                    fontSizeSmall = 10.sp, fontSizeBig = 18.sp
                ) { onSearchClick() }
            ),
            RowContent(
                left  = MenuItemData(
                    "Demo", R.drawable.ic_demo,
                    iconSizeSmall = 48.dp, iconSizeBig = 48.dp,
                    fontSizeSmall = 18.sp, fontSizeBig = 18.sp
                ) { onDemoClick() },
                right = MenuItemData(
                    "Settings", R.drawable.ic_settings,
                    iconSizeSmall = 58.dp, iconSizeBig = 58.dp,
                    fontSizeSmall = 22.sp, fontSizeBig = 22.sp
                ) { onSettingsClick() }
            )
        )
    }
    Scaffold(
        containerColor = White,
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start  = innerPadding.calculateStartPadding(layoutDirection),
                    end    = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            HeaderSection(
                title        = "Home",
                centerButton = "Emergency Call",
                onCenterButton = {
                    // emergency action
                }
            )

            Spacer(Modifier.height(10.dp))
            Text(
                text       = "Hi User",
                fontFamily = OutfitFont,
                fontSize   = 28.sp,
                fontWeight = FontWeight.Bold,
                color      = TextDark,
                modifier   = Modifier.padding(horizontal = 24.dp).offset(y = (-20).dp)
            )
            Spacer(Modifier.height(30.dp))

            RotatingRowGrid(
                rowsData = rowsData,
                startExpanded = startExpanded,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )
        }
    }
}

@Preview(name = "Home", showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    GreenTheme {
        HomeScreen(
            onSettingsClick = {}
        )
    }
}

@Preview(name = "Home - Expanded", showSystemUi = true)
@Composable
fun HomeScreenExpandedPreview() {
    GreenTheme {
        HomeScreen(
            onSettingsClick = {},
            startExpanded   = true
        )
    }
}