package com.example.evoke.utils

import androidx.compose.runtime.compositionLocalOf

val LocalAppLanguage = compositionLocalOf {
    LanguageManager.ENGLISH
}