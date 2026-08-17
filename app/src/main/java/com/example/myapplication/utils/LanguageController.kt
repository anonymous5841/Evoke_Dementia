package com.example.myapplication.utils

import android.content.Context
import androidx.compose.runtime.MutableState

object LanguageController {

    lateinit var languageState: MutableState<String>

    fun currentLanguage(): String {
        return languageState.value
    }

    fun updateLanguage(
        context: Context,
        language: String
    ) {

        // Don't do anything if language didn't change
        if (languageState.value == language) {
            return
        }

        // Save language
        LanguageManager.saveLanguage(
            context.applicationContext,
            language
        )

        // Update Compose state
        // This will trigger recomposition.
        languageState.value = language
    }
}