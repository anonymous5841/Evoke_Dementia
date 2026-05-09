package com.example.myapplication.ui.theme
//
//import android.app.Activity
//import android.os.Build
//import androidx.compose.foundation.isSystemInDarkTheme
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.darkColorScheme
//import androidx.compose.material3.dynamicDarkColorScheme
//import androidx.compose.material3.dynamicLightColorScheme
//import androidx.compose.material3.lightColorScheme
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.platform.LocalContext
//
//private val DarkColorScheme = darkColorScheme(
//    primary = Purple80,
//    secondary = PurpleGrey80,
//    tertiary = Pink80
//)
//
//private val LightColorScheme = lightColorScheme(
//    primary = Purple40,
//    secondary = PurpleGrey40,
//    tertiary = Pink40
//
//    /* Other default colors to override
//    background = Color(0xFFFFFBFE),
//    surface = Color(0xFFFFFBFE),
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
//    */
//)
//
//@Composable
//fun DementiaTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
//
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = Typography,
//        content = content
//    )
//}

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


// ── Color tokens ──────────────────────────────────────────────────────────────
val White        = Color(0xFFFFFFFF)  // background
val DarkGreen    = Color(0xFF3E634F)  // back button (no header), labels, icons, cards, main buttons
val YellowAccent = Color(0xFFFFCD38)  // button color (mostly)
val GoldAccent   = Color(0xFFFFC006)  // back text color, search icon
val FieldBg      = Color(0xFFDBE1DD)  // field/input background
val TextInField  = Color(0xFFA7AEC1)  // placeholder / hint text inside fields

val HeaderText = Color(0xFFB0E420)
val MediumGreen = Color(0xFF3E7A52) //selected textfield outline

// ── Green color scheme ────────────────────────────────────────────────────────
private val GreenColorScheme = lightColorScheme(

    // ── Backgrounds ───────────────────────────────────────────────────────────
    background       = White,          // main screen background
    onBackground     = DarkGreen,      // text/icons on background

    // ── Primary = main green (buttons, cards, icons, labels) ──────────────────
    primary          = DarkGreen,      // main buttons, cards, icons, label text
    onPrimary        = YellowAccent,          // text/icons ON dark green buttons/cards

    // ── Secondary = yellow (back text, search icon, secondary buttons) ─────────
    secondary        = YellowAccent,   // most used button color
    onSecondary      = DarkGreen,      // text ON yellow buttons

    // ── Tertiary = gold (back text color, search icon) ─────────────────────────
    tertiary         = GoldAccent,     // back text, search icon
    onTertiary       = DarkGreen,      // text ON gold elements
    tertiaryContainer = White, // for back icon
    onTertiaryContainer = White, // for header text

    // ── Surface = fields, text field containers ────────────────────────────────
    surface          = FieldBg,        // text field background, card background
    onSurface        = DarkGreen,      // typed text color inside fields
    onSurfaceVariant = TextInField,    // placeholder/hint text inside fields

    // ── Outline = field borders ────────────────────────────────────────────────
    outline          = MediumGreen,      // focused border color

    inversePrimary = HeaderText,
)

@Composable
fun GreenTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GreenColorScheme) {
        content()
    }
}
