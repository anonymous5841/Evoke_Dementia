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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.myapplication.R

// ── Green color tokens ────────────────────────────────────────────────────────
val White        = Color(0xFFFFFFFF)
val DarkGreen    = Color(0xFF3E634F)
val YellowAccent = Color(0xFFFFCD38)
val GoldAccent   = Color(0xFFFFC006)
val FieldBg      = Color(0xFFDBE1DD)
val TextInField  = Color(0xFFA7AEC1)
val HeaderText   = Color(0xFFB0E420)
val MediumGreen  = Color(0xFF3E7A52)

// ── Blue color tokens ─────────────────────────────────────────────────────────
val BlueHeader      = Color(0xFFBAEAFF)
val BlueShadow      = Color(0xFFBED4E7)
val BlueDeep        = Color(0xFF14445F)
val BlueNavy        = Color(0xFF00324E)
val BlueOrange      = Color(0xFFFF9838)
val BlueFieldBg     = Color(0xFFDBE0E1)
val BlueOutline     = Color(0xFF518AAF)
val BlueSelectedBtn = Color(0xFFB3C7D1)
val BlueButtonMain  = Color(0xFFF0FFFF)
val BlueCameraOuter = Color(0xFF638AA0)
val BlueCameraInner = Color(0xFFBAEAFF)
val BlueBoxOuter    = Color(0xFFECFCFF)
val BlueBoxInner    = Color(0xFFC6E9ED)
val BlueToggle      = Color(0xFF5891B5)
val BluePictureBox  = Color(0xFFDFF2F3)
val BlueAfterPlay   = Color(0xFF002B31)


// ── App color set — single source of truth ─────────────────────────────────────
data class AppColors(
    val headerDecorationRes : Int,    // leaves.xml / clouds.xml
    val background          : Color,
    val headerBg            : Color,  // header bg + footer/nav bg
    val headerShadow        : Color,  // header border/highlight
    val headerinnershadow   : Color,
    val headerText          : Color,  // header text + footer icon unselected
    val headerSecondaryText : Color,  // secondary header text (was inversePrimary)
    val buttonshadow        : Color,
    val headerButton        : Color,
    val headerButtonText    : Color,
    val backSign            : Color,  // back arrow icon color (with header)
    val backText            : Color,  // "Back" text color (with header)
    val backButton           : Color, // back button + main page icon color (no header)
    val pagesText            : Color, // pages text, button1 color, button2 text,
    // card color, cross color, waves, demo button text
    val iconSelected         : Color, // footer icon selected, button2 color, button1 text
    val selectedLangBtn      : Color,
    val mainButton           : Color,
    val textfield            : Color, // also text field bg
    val textFieldHint        : Color,
    val selectedFieldOutline : Color,
    val cameraOuter          : Color,
    val cameraInner          : Color,
    val popupText            : Color,
    val afterPlayColor       : Color,
    val boxOuter             : Color,
    val boxInner             : Color,
    val toggleColor          : Color,
    val pictureBox           : Color,
)


// ── Green values ──────────────────────────────────────────────────────────────
val GreenAppColors = AppColors(
    headerDecorationRes  = R.drawable.leaves,
    background           = White,
    headerBg             = DarkGreen,
    headerShadow         = Color(0xFF95A79D),
    headerinnershadow    = Color(0xFF000000),
    headerText           = White,
    headerSecondaryText  = HeaderText,
    buttonshadow         = White,
    headerButton         = Color(0xFFFFC107),
    headerButtonText     = Color(0xFF000000),
    backSign             = White,
    backText             = YellowAccent,
    backButton           = DarkGreen,
    pagesText            = DarkGreen,
    iconSelected         = YellowAccent,
    selectedLangBtn      = Color(0xFFB7D4C0),
    mainButton           = FieldBg,
    textfield            = FieldBg,
    textFieldHint        = TextInField,
    selectedFieldOutline = MediumGreen,
    cameraOuter          = DarkGreen,
    cameraInner          = Color(0xFFDDF2E4),
    popupText            = GoldAccent,
    afterPlayColor       = Color(0xFF003117),
    boxOuter             = Color(0xFFDDF2E4),
    boxInner             = Color(0xFFC1D7C8),
    toggleColor          = DarkGreen,
    pictureBox           = Color(0xFFDDF2E4),
)

// ── Blue values ───────────────────────────────────────────────────────────────
val BlueAppColors = AppColors(
    headerDecorationRes  = R.drawable.clouds,
    background           = White,
    headerBg             = BlueHeader,
    headerShadow         = BlueShadow,
    headerinnershadow    = White,
    headerText           = BlueDeep,
    headerSecondaryText  = Color(0xFF000000),
    buttonshadow         = Color(0xFF000000),
    headerButton         = BlueOrange,
    headerButtonText     = White,
    backSign             = BlueNavy,
    backText             = BlueNavy,
    backButton           = BlueNavy,
    pagesText            = BlueDeep,
    iconSelected         = BlueOrange,
    selectedLangBtn      = BlueSelectedBtn,
    mainButton           = BlueButtonMain,
    textfield            = BlueFieldBg,
    textFieldHint        = TextInField,
    selectedFieldOutline = BlueOutline,
    cameraOuter          = BlueCameraOuter,
    cameraInner          = BlueCameraInner,
    popupText            = BlueOrange,
    afterPlayColor       = BlueAfterPlay,
    boxOuter             = BlueBoxOuter,
    boxInner             = BlueBoxInner,
    toggleColor          = BlueToggle,
    pictureBox           = BluePictureBox,
)


// ── CompositionLocal ──────────────────────────────────────────────────────────
val LocalAppColors = staticCompositionLocalOf { GreenAppColors }

object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current
}


// ── Theme composables ─────────────────────────────────────────────────────────
@Composable
fun GreenTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides GreenAppColors) {
        MaterialTheme(typography = AppTypography, content = content)
    }
}

@Composable
fun BlueTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppColors provides BlueAppColors) {
        MaterialTheme(typography = AppTypography, content = content)
    }
}