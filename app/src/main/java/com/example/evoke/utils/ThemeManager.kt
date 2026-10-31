package com.example.evoke.utils

import android.content.Context

object ThemeManager {

    private const val PREF_NAME = "theme_pref"
    private const val KEY_THEME = "selected_theme"

    const val GREEN = "green"
    const val BLUE = "blue"

    fun saveTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme)
            .apply()
    }

    fun getSavedTheme(context: Context): String {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, GREEN)
            ?: GREEN
    }

    fun isBlueTheme(context: Context): Boolean {
        return getSavedTheme(context) == BLUE
    }
}