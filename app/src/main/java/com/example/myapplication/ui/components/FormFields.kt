package com.example.myapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.OutfitFont

/**
 * Shared text-input field: shadow + clipped OutlinedTextField.
 * Does NOT render a label — call FieldLabel separately above it, same as before.
 * Works for single-line fields (Name, Phone, Title...) and multiline fields
 * (Description) by passing singleLine = false and a maxLines value.
 */
@Composable
fun ShadowTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIconRes: Int? = null,
    height: Dp = 56.dp,
    cornerRadius: Dp = 12.dp,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(cornerRadius), clip = false)
            .clip(RoundedCornerShape(cornerRadius))
            .then(                                     // ← add this block
                if (onClick != null)
                    Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onClick
                        )
                else Modifier
            )
    ) {
        if (onClick != null) {
            Text(
                text = value.ifEmpty { placeholder },
                color = if (value.isEmpty())
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontFamily = OutfitFont,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp,
                        fontFamily = OutfitFont
                    )
                },
                leadingIcon = leadingIconRes?.let {
                    {
                        Icon(
                            painter = painterResource(id = it),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true)
                    ),
                shape = RoundedCornerShape(cornerRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                singleLine = singleLine,
                maxLines = if (singleLine) 1 else maxLines
            )
        }
    }
}

/**
 * Read-only, clickable row showing a chosen location (or placeholder text),
 * a thin line-separator icon, and a map-pin icon. Never accepts typed input —
 * tapping it navigates elsewhere (e.g. to a map picker).
 */
@Composable
fun LocationPickerField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    cornerRadius: Dp = 12.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    shadowElevation: Dp = 6.dp,
    showShadow: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(
                if (showShadow) Modifier.shadow(elevation = shadowElevation, shape = RoundedCornerShape(cornerRadius), clip = false)
                else Modifier
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = value.ifEmpty { placeholder },
                fontSize = 18.sp,
                fontFamily = OutfitFont,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(id = R.drawable.line_icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .width(25.dp)
                    .height(40.dp)
                    .padding(end = 4.dp)
            )
            Icon(
                painter = painterResource(id = R.drawable.location_icon),
                contentDescription = "Map pin",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}