package com.example.myapplication.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MartelFont

@Composable
fun FieldLabel(text: String, textsize: TextUnit  = 20.sp, fontFamily: FontFamily = MartelFont,  fontWeight: FontWeight = FontWeight.SemiBold ) {
    Text(
        text       = text,
        color      = MaterialTheme.colorScheme.onBackground,
        fontSize   = textsize,
        fontWeight = fontWeight,
        fontFamily = fontFamily
    )
}
