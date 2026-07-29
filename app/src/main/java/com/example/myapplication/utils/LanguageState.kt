package com.example.myapplication.utils

import androidx.compose.runtime.compositionLocalOf

val LocalAppLanguage = compositionLocalOf {
    LanguageManager.ENGLISH
}