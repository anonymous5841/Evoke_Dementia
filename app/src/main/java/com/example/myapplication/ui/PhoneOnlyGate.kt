package com.example.myapplication.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val MIN_SUPPORTED_WIDTH_DP = 320   // excludes very small/legacy screens
private const val MAX_SUPPORTED_WIDTH_DP = 600   // excludes tablets/foldables-unfolded/desktop

@Composable
fun PhoneOnlyGate(content: @Composable () -> Unit) {
    val smallestWidthDp = LocalConfiguration.current.smallestScreenWidthDp
    when {
        smallestWidthDp < MIN_SUPPORTED_WIDTH_DP -> UnsupportedScreen("This app requires a larger screen.")
        smallestWidthDp > MAX_SUPPORTED_WIDTH_DP -> UnsupportedScreen("This app is designed for phones only.")
        else -> content()
    }
}

@Composable
private fun UnsupportedScreen(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
    }
}