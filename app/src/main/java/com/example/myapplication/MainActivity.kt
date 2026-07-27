package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.myapplication.ui.navigation.AppNavigation
import com.example.myapplication.ui.theme.AppTheme

import android.content.Context
import android.content.ContextWrapper
import com.example.myapplication.utils.LanguageManager
import com.example.myapplication.utils.LocaleHelper

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.myapplication.utils.LanguageController

import com.example.myapplication.utils.LocalAppLanguage

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {

        val language = LanguageManager.getSavedLanguage(newBase)

        val context = LocaleHelper.setLocale(
            newBase,
            language
        )

        super.attachBaseContext(ContextWrapper(context))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            var isBlueTheme by rememberSaveable {
                mutableStateOf(false)
            }
            val context = LocalContext.current

            val currentLanguage = rememberSaveable {
                mutableStateOf(
                    LanguageManager.getSavedLanguage(context)
                )
            }
            LanguageController.languageState = currentLanguage

            val view = LocalView.current

            CompositionLocalProvider(

                LocalAppLanguage provides currentLanguage.value,

                LocalLayoutDirection provides LayoutDirection.Ltr

            ){

                AppTheme(
                    isBlue = isBlueTheme
                ) {
                    val barColor = AppTheme.colors.headerBg
                    val useDarkIcons = barColor.luminance() > 0.5f

                    SideEffect {
                        val window = (view.context as Activity).window
                        WindowInsetsControllerCompat(window, view).apply {
                            isAppearanceLightStatusBars = useDarkIcons
                            isAppearanceLightNavigationBars = useDarkIcons
                        }
                        if (android.os.Build.VERSION.SDK_INT >= 29) {
                            window.isNavigationBarContrastEnforced = false
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(barColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .clipToBounds()          // ← add this
                        ) {
                            AppNavigation(
                                isBlueTheme = isBlueTheme,
                                onThemeToggle = { isBlueTheme = it }
                            )
                        }
                    }
                }
            }
        }
    }
}