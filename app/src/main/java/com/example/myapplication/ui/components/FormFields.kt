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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.OutfitFont
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight

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
    fontSize: TextUnit = 18.sp,
    onClick: (() -> Unit)? = null,
    placeholderColor: Color = Color(0xFFA7AEC1),
) {
    val appColors = AppTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(cornerRadius), clip = false)
            .clip(RoundedCornerShape(cornerRadius))
            .then(                                     // ← add this block
                if (onClick != null)
                    Modifier
                        .background(appColors.textfield)
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
                    placeholderColor
                else
                    Color(0xFF000000),
                fontSize = fontSize,
                fontFamily = OutfitFont,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                interactionSource = interactionSource,
                textStyle = LocalTextStyle.current.copy(fontSize = fontSize, fontFamily = OutfitFont),
                placeholder = {
                    Text(
                        placeholder,
                        color = placeholderColor,
                        fontSize = fontSize,
                        fontFamily = OutfitFont
                    )
                },
                leadingIcon = leadingIconRes?.let {
                    {
                        Icon(
                            painter = painterResource(id = it),
                            contentDescription = null,
                            tint = appColors.backButton,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = height)
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true)
                    ),
                shape = RoundedCornerShape(cornerRadius),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor   = appColors.textfield,
                    focusedContainerColor     = appColors.textfield,
                    unfocusedBorderColor      = Color.Transparent,
                    focusedBorderColor        = appColors.selectedFieldOutline,
                    unfocusedTextColor        = appColors.pagesText,
                    focusedTextColor          = appColors.pagesText,
                    unfocusedPlaceholderColor = appColors.textFieldHint,
                    focusedPlaceholderColor   = appColors.textFieldHint,
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
    backgroundColor: Color = AppTheme.colors.textfield,
    shadowElevation: Dp = 6.dp,
    showShadow: Boolean = true,
) {
    val appColors = AppTheme.colors
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
                color = Color(0xFFA7AEC1),
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
                painter = painterResource(id = R.drawable.light_location_icon),
                contentDescription = "Map pin",
                tint = appColors.backButton,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}


@Composable
fun RecordConversationField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    cornerRadius: Dp = 12.dp,
    backgroundColor: Color = AppTheme.colors.textfield,
    shadowElevation: Dp = 6.dp,
    showShadow: Boolean = true,
) {
    val appColors = AppTheme.colors
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
                color = Color(0xFFA7AEC1),
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
                painter = painterResource(id = R.drawable.recording_icon),
                contentDescription = "Map pin",
                tint = appColors.backButton,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}


@Composable
fun IconWithShadow(
    painter: Painter,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
    shadowColor: Color = Color.Black.copy(alpha = 0.35f),
    blurRadius: Dp = 4.dp,
    offsetY: Dp = 2.dp,
    offsetX: Dp = 0.dp
) {
    Box(modifier = modifier) {
        // Blurred silhouette of the icon, offset behind — only this part is blurred
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Image(
                painter = painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(shadowColor),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = offsetX, y = offsetY)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        renderEffect = BlurEffect(
                            radiusX = blurRadius.toPx(),
                            radiusY = blurRadius.toPx(),
                            edgeTreatment = TileMode.Decal
                        )
                    }
            )
        }
        // The real icon on top, unblurred
        Image(
            painter = painter,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(tint),
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )
    }
}

// ══════════════════════════════════════════════════════
// INFO NOTE PILL — outlined rounded box with red text
// [BORDER COLOR] change Color(0xFFA13B3B)
// [TEXT COLOR] change Color(0xFFA13B3B)
// [CORNER RADIUS] change 50.dp (fully rounded / pill shape)
// ══════════════════════════════════════════════════════
@Composable
fun InfoNotePill(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = Color(0xFFA13B3B),
                shape = RoundedCornerShape(50.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFFA13B3B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            fontFamily = OutfitFont // swap for whatever font matches your app
        )
    }
}