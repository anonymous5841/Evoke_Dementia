package com.example.myapplication.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun FieldLabel(text: String, textsize: TextUnit  = 20.sp) {
    Text(
        text       = text,
        color      = MaterialTheme.colorScheme.onBackground,
        fontSize   = textsize,
        fontWeight = FontWeight.SemiBold
    )
}
