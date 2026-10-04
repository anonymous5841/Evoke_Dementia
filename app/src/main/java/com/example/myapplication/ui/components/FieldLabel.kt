package com.example.myapplication.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.utils.LanguageManager
import com.example.myapplication.utils.LocalAppLanguage

// ── Language helpers ──────────────────────────────────────────────────────────
// Read inside composition, so every caller recomposes when the language changes.

@Composable
@ReadOnlyComposable
fun isUrduSelected(): Boolean = LocalAppLanguage.current == LanguageManager.URDU

/**
 * Left in English, Right in Urdu. Absolute, so it does not depend on the
 * surrounding LayoutDirection. Use for a Column's horizontalAlignment when its
 * children should line up with the labels.
 */
@Composable
@ReadOnlyComposable
fun languageStartAlignment(): Alignment.Horizontal =
    if (isUrduSelected()) AbsoluteAlignment.Right else AbsoluteAlignment.Left

/**
 * Lays out [content] RTL when Urdu is selected, LTR otherwise.
 * The rest of the app stays LTR — only what is wrapped here mirrors.
 */
@Composable
fun AppLanguageDirection(
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        content()
        return
    }
    val direction = if (isUrduSelected()) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction, content = content)
}

/**
 * Field label that sits on the left in English and on the right in Urdu.
 *
 * fillMaxWidth = true (default): the label takes the full width of its parent so
 * it can move to the right edge in Urdu. Use this in normal Columns.
 *
 * fillMaxWidth = false: the label wraps its text. Use this when the label is inside
 * a Row column that has no weight or fixed width (e.g. "Date", "Get Location",
 * "Add voice sample"). A full-width label there would take all the space and
 * squeeze the weight(1f) column next to it.
 */
@Composable
fun FieldLabel(
    text: String,
    textsize: TextUnit = 20.sp,
    fontFamily: FontFamily = MartelFont,
    fontWeight: FontWeight = FontWeight.SemiBold,
    modifier: Modifier = Modifier,
    fillMaxWidth: Boolean = true,
    textAlign: TextAlign = TextAlign.Start, // Start = left in English, right in Urdu
) {
    AppLanguageDirection {
        Text(
            text = text,
            color = AppTheme.colors.pagesText,
            fontSize = textsize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            modifier = if (fillMaxWidth) modifier.fillMaxWidth() else modifier
        )
    }
}
