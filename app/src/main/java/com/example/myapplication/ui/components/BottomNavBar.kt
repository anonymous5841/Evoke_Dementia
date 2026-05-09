package com.example.myapplication.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable

enum class NavTab { HOME, PROFILE, NONE }

@Composable
fun BottomNavBar(
    selectedTab    : NavTab     = NavTab.NONE,
    onHomeClick    : () -> Unit = {},
    onProfileClick : () -> Unit = {}
) {
    val models = listOf(
        MeowBottomNavigationModel(id = 1, icon = Icons.Default.Home),
        MeowBottomNavigationModel(id = 2, icon = Icons.Default.Person)
    )

    val selectedId = when (selectedTab) {
        NavTab.HOME    ->  1
        NavTab.PROFILE ->  2
        NavTab.NONE    -> -1
    }

    MeowBottomNavigation(
        models        = models,
        selectedId    = selectedId,
        onTabSelected = { model ->
            if (model.id == 1) onHomeClick() else onProfileClick()
        }
    )
}