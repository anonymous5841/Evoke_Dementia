package com.example.evoke

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import com.example.evoke.ui.navigation.AppNavigation
import com.example.evoke.ui.theme.AppTheme
import android.content.Context
import android.content.ContextWrapper
import com.example.evoke.utils.LanguageManager
import com.example.evoke.utils.LocaleHelper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.evoke.ui.PhoneOnlyGate
import com.example.evoke.utils.LanguageController

import com.example.evoke.utils.LocalAppLanguage
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.viewmodel.AppViewModelFactory
import com.example.evoke.utils.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

//        if (BuildConfig.DEBUG) {
//            com.example.evoke.debug.DebugSeed.seedIfEmpty(applicationContext)
//        }
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
/*
create startupcleaning.kt:
package com.example.evoke

import android.util.Log
import com.example.evoke.database.viewmodel.AppViewModelFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object StartupCleanup {

    private val started = AtomicBoolean(false)
    private val finished = CompletableDeferred<Unit>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Runs at most once per process, so recreating the Activity can
    // never delete a recording that is saved but not yet in the database.
    fun runOnce(factory: AppViewModelFactory) {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            try {
                factory.cleanupOrphanedAudio()
            } catch (e: Exception) {
                Log.e("StartupCleanup", "Cleanup failed", e)
            } finally {
                finished.complete(Unit)
            }
        }
    }

    suspend fun awaitFinished() = finished.await()
}

replace lifecycleScope.launch(Dispatchers.IO) { factory.cleanupOrphanedAudio() } with StartupCleanup.runOnce(factory)
do this only if you want the things like :
Cleanup must finish before any recording is saved.
Cleanup must not run again when the Activity is recreated, for example on screen rotation.
 */
//        val factory =
//            AppViewModelFactory(AppDatabase.getDatabase(applicationContext), applicationContext)
//        lifecycleScope.launch(Dispatchers.IO) { factory.cleanupOrphanedAudio() }

        setContent {

            val context = LocalContext.current

            var isBlueTheme by rememberSaveable {
                mutableStateOf(ThemeManager.isBlueTheme(context))
            }
            var isSplashScreen by rememberSaveable {
                mutableStateOf(true)
            }
            val currentLanguage = rememberSaveable {
                mutableStateOf(
                    LanguageManager.getSavedLanguage(context)
                )
            }

            /*
             * Shared language state used by:
             *
             * - Splash language selector
             * - Settings language selector
             */
            LanguageController.languageState = currentLanguage

            /*
             * Create a localized Context whenever the language changes.
             *
             * IMPORTANT:
             * We do NOT recreate the Activity.
             */
            val localizedContext = remember(
                context,
                currentLanguage.value
            ) {
                LocaleHelper.setLocale(
                    context,
                    currentLanguage.value
                )
            }

            val view = LocalView.current

            CompositionLocalProvider(

                // Explicitly provide the Activity Result owner
                LocalActivity provides this@MainActivity,
                LocalActivityResultRegistryOwner provides this@MainActivity,
                // Shared language state
                LocalAppLanguage provides currentLanguage.value,

                // Localized context
                LocalContext provides localizedContext,

                // Keep your application LTR
                LocalLayoutDirection provides LayoutDirection.Ltr

            ) {

                AppTheme(
                    isBlue = isBlueTheme
                ) {

                    val barColor = if (isSplashScreen) {
                        Color(0xFF3E634F)
                    } else {
                        AppTheme.colors.headerBg
                    }
                    val useDarkIcons =
                        barColor.luminance() > 0.5f

                    SideEffect {

                        val window =
                            (view.context as Activity).window

                        WindowInsetsControllerCompat(
                            window,
                            view
                        ).apply {

                            isAppearanceLightStatusBars =
                                useDarkIcons

                            isAppearanceLightNavigationBars =
                                useDarkIcons
                        }

                        if (android.os.Build.VERSION.SDK_INT >= 29) {

                            window.isNavigationBarContrastEnforced =
                                false
                        }
                    }

                    /*
                     * KEEP THIS.
                     *
                     * This is what gives your screenshot the
                     * green/blue status and navigation bar areas.
                     */
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(barColor)
                    ) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(
                                    WindowInsets.statusBars
                                )
                                .clipToBounds()
                        ) {

                            PhoneOnlyGate {

                                AppNavigation(
                                    isBlueTheme = isBlueTheme,
                                    onThemeToggle = {
                                        isBlueTheme = it

                                        ThemeManager.saveTheme(
                                            context,
                                            if (it) ThemeManager.BLUE else ThemeManager.GREEN
                                        )
                                    },
                                    onSplashScreenChanged = {
                                        isSplashScreen = it
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

//lifecycleScope.launch {
//    appViewModelFactory.cleanupOrphanedAudio()
//    recognitionViewModel.loadPendingConversationCount()
//}