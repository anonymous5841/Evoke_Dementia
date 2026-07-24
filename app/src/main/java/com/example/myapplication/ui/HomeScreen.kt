package com.example.myapplication.ui
import android.annotation.SuppressLint
import androidx.compose.foundation.background
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
import com.example.myapplication.ui.components.MenuItemData
import com.example.myapplication.ui.components.RowContent
import com.example.myapplication.ui.components.RotatingRowGrid
import com.example.myapplication.ui.components.TextDark
import com.example.myapplication.ui.components.rememberBottomNavBarHeight
import com.example.myapplication.ui.theme.AppTheme

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter") @Composable
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
    val appColors = AppTheme.colors
    val bottomNavHeight = rememberBottomNavBarHeight()
    // Fixed row identities — top row is the "big" pair, then medium, then small.
    val rowsData = remember {
        listOf(
            RowContent(
                left  = MenuItemData(
                    "Recognize \n/\nRegister", appColors.recognizeIcon,
                    iconSizeSmall = 50.dp, iconSizeBig = 50.dp,
                    fontSizeSmall = 20.sp, fontSizeBig = 20.sp,
                    tintIcon = false
                ) { onRecognizeClick() },
                right = MenuItemData(
                    "Add location", R.drawable.ic_location,
                    iconSizeSmall = 44.dp, iconSizeBig = 44.dp,
                    fontSizeSmall = 20.sp, fontSizeBig = 20.sp
                ) { onAddLocationClick() }
            ),
            RowContent(
                left  = MenuItemData(
                    "Help", R.drawable.help_icon,
                    iconSizeSmall = 41.dp, iconSizeBig = 60.dp,
                    fontSizeSmall = 18.sp, fontSizeBig = 23.sp
                ) { onHelpClick() },
                right = MenuItemData(
                    "Search\nName / Location", R.drawable.ic_search,
                    iconSizeSmall = 18.dp, iconSizeBig = 50.dp,
                    fontSizeSmall = 16.sp, fontSizeBig = 18.sp
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

    val layoutDirection = LocalLayoutDirection.current
    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // insets already handled at root

    ) {innerpadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomNavHeight)

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
}}


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