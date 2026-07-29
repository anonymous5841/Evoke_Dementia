package com.example.myapplication.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    fun setLocale(
        context: Context,
        language: String
    ): Context {

        val locale = Locale(language)

        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)

        configuration.setLocale(locale)

        configuration.setLayoutDirection(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            context.resources.updateConfiguration(
                configuration,
                context.resources.displayMetrics
            )
            context
        }
    }
}