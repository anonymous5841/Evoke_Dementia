package com.example.myapplication.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Re-applies the app's selected language to [content].
 *
 * Why this is needed: a Compose Dialog (every `dialog(...)` route / popup) opens
 * in its own window, and that window resets LocalContext to the Activity's own
 * context — which still has the language the app was STARTED with. The
 * localized context that MainActivity provides doesn't reach it, so
 * stringResource() inside a popup showed the old language after switching.
 *
 * LocalAppLanguage does reach the dialog, so we rebuild the localized context
 * from it here (same thing MainActivity does at the root).
 */
@Composable
fun ProvideAppLocale(content: @Composable () -> Unit) {
    val language = LocalAppLanguage.current
    val baseContext = LocalContext.current

    val localizedContext = remember(baseContext, language) {
        LocaleHelper.setLocale(baseContext, language)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        content = content
    )
}
