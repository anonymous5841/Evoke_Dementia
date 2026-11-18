package com.example.evoke.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evoke.ui.theme.AppTheme
import com.example.evoke.ui.theme.OutfitFont

private val SPEEDS = listOf(0.5f, 1f, 1.5f, 2f)

@Composable
fun SpeedSelector(
    speed: Float,
    onSpeedSelected: (Float) -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Black,
    fontSize: TextUnit = 12.sp
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = AppTheme.colors

    Box(modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${speed}x", color = textColor, fontSize = fontSize,
                fontWeight = FontWeight.Bold, fontFamily = OutfitFont)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                tint = textColor, modifier = Modifier.size(18.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(76.dp),
            containerColor = colors.boxInner,          // adjust to taste
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            offset = DpOffset(0.dp, (-12).dp)
        ) {
            SPEEDS.forEach { option ->
                val selected = option == speed
                DropdownMenuItem(
                    text = {
                        Box(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) colors.boxOuter else Color.Transparent)
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${option}x", fontSize = 12.sp, fontFamily = OutfitFont,
                                color = if (selected) colors.pagesText else Color(0xFF333333),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        }
                    },
                    onClick = { expanded = false; onSpeedSelected(option) },
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}