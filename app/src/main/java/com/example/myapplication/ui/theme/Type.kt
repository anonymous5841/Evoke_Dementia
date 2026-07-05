package com.example.myapplication.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.*
import com.example.myapplication.R


// ─── Baumans (single weight) ─────────────────────────────
val BaumansFont = FontFamily(
    Font(R.font.baumans_regular, FontWeight.Normal)
)

// ─── Martel (full serif family) ──────────────────────────
val MartelFont = FontFamily(
    Font(R.font.martel_extralight, FontWeight.ExtraLight),
    Font(R.font.martel_light, FontWeight.Light),
    Font(R.font.martel_regular, FontWeight.Normal),
    Font(R.font.martel_semibold, FontWeight.SemiBold),
    Font(R.font.martel_bold, FontWeight.Bold),
    Font(R.font.martel_extrabold, FontWeight.ExtraBold),
    Font(R.font.martel_black, FontWeight.Black)
)

// ─── Pompiere (decorative, single weight) ────────────────
val PompiereFont = FontFamily(
    Font(R.font.pompiere_regular, FontWeight.Normal)
)

// ─── Outfit (main UI font, full range) ───────────────────
val OutfitFont = FontFamily(
    Font(R.font.outfit_thin, FontWeight.Thin),
    Font(R.font.outfit_extralight, FontWeight.ExtraLight),
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
    Font(R.font.outfit_black, FontWeight.Black)

)

val AppTypography = Typography(

    // 🔹 Main headings (modern UI)
    titleLarge = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),

    titleMedium = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),

    // 🔹 Body text (readable serif)
    bodyLarge = TextStyle(
        fontFamily = MartelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = MartelFont,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp
    ),

    // 🔹 Buttons / UI labels
    labelLarge = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),

    // 🔹 Special UI (your green theme / names etc.)
    labelMedium = TextStyle(
        fontFamily = BaumansFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    // 🔹 Decorative (app name, splash screen)
    displayLarge = TextStyle(
        fontFamily = PompiereFont,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp
    )
)