package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class NavTab { HOME, PROFILE, NONE }

@Composable
fun BottomNavBar(
    selectedTab    : NavTab     = NavTab.NONE,
    onHomeClick    : () -> Unit = {},
    onProfileClick : () -> Unit = {},
    modifier       : Modifier = Modifier          // ← add this

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
        },
        modifier      = modifier                    // ← forward it
    )
}

// BottomNavBar.kt

@Composable
fun rememberBottomNavBarHeight(): Dp {
    val navBarInset        = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val systemGestureInset = WindowInsets.systemGestures.asPaddingValues().calculateBottomPadding()
    val extraBottomPadding = if (systemGestureInset <= navBarInset) navBarInset * 0.4f else 0.dp
    return 108.dp + extraBottomPadding
}