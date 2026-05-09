package com.example.myapplication.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.AddLocationContent
import com.example.myapplication.ui.PersonFormContent
import com.example.myapplication.ui.PersonFormMode
import com.example.myapplication.ui.LocationSearchContent
import com.example.myapplication.ui.NotRecognisedContent
import com.example.myapplication.ui.components.BottomNavBar
import com.example.myapplication.ui.components.NavTab

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val currentRoute by navController.currentBackStackEntryAsState()
    val currentDestination = currentRoute?.destination?.route

    val selectedTab = when (currentDestination) {
        "home"    -> NavTab.HOME
        "profile" -> NavTab.PROFILE
        else      -> NavTab.NONE
    }

    // ← add this — routes where nav bar is completely hidden
    val showNavBar = currentDestination != "addperson"

    GreenTheme {
        @Suppress("UnusedMaterial3ScaffoldPaddingParameter")
        Scaffold(
            bottomBar = {
                // ← wrap with if
                if (showNavBar) {
                    BottomNavBar(
                        selectedTab    = selectedTab,
                        onHomeClick    = {
                            navController.navigate("home") {
                                launchSingleTop = true
                            }
                        },
                        onProfileClick = {
                            navController.navigate("profile") {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        ) { _ ->
            NavHost(
                navController    = navController,
                startDestination = "addlocation",
                modifier         = Modifier
            ) {
                composable("addlocation") {
                    AddLocationContent(
                        onAddClick = { navController.navigate("notrecognised") }  // ← add this
                    )
                }
                composable("notrecognised") {
                    NotRecognisedContent()
                }
                composable("profile") {
                    PersonFormContent(mode = PersonFormMode.EDIT)
                }
                composable("home") {
                    LocationSearchContent()
                }
                // ← add this route for ADD mode (no nav bar)
                composable("addperson") {
                    PersonFormContent(mode = PersonFormMode.ADD)
                }
            }
        }
    }
}