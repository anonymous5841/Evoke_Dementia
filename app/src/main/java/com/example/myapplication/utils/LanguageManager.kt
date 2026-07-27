package com.example.myapplication.utils

import android.content.Context

object LanguageManager {

    private const val PREF_NAME = "language_pref"
    private const val KEY_LANGUAGE = "selected_language"

    const val ENGLISH = "en"
    const val URDU = "ur"

    fun saveLanguage(context: Context, language: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun getSavedLanguage(context: Context): String {
        return context
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, ENGLISH)
            ?: ENGLISH
    }
}