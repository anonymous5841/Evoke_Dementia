package com.example.myapplication.utils

import android.app.Activity
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
        if (languageState.value == language)
            return

        // Update Compose state
        languageState.value = language

        // Save preference
        LanguageManager.saveLanguage(
            context,
            language
        )

        // Recreate Activity so Android reloads resources
        (context as? Activity)?.recreate()
    }
}