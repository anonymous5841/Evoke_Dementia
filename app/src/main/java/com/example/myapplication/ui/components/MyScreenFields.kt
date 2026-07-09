package com.example.myapplication.ui.components
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.ui.theme.PompiereFont
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Spacer

import androidx.compose.ui.text.style.TextAlign

import com.example.myapplication.ui.theme.OutfitFont

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ColorFilter

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.unit.TextUnit

val DarkGreen  = Color(0xFF2D5A27)
val LightGreen = Color(0xFFDBE1DD)
val MedGreen   = Color(0xFF3D7A35)
val YellowBtn  = Color(0xFFF5C518)
val TextDark   = Color(0xFF1A2E18)
val White      = Color(0xFFFFFFFF)
@Composable
fun DateDisplayField(
    date: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(110.dp)
            .height(51.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(15.dp),
                clip = false
            )
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = date,
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 16.sp,          // Same as Location field
            fontFamily = OutfitFont,   // Same font
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun DiscussionSummaryBox(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(15.dp),
                clip = false
            )
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surface)
    )
}

@Composable
fun DiscussionVoiceSnippet(
    modifier: Modifier = Modifier
) {

    var isPlaying by remember { mutableStateOf(true) }
    var speedMultiplier by remember { mutableStateOf(1f) }

    val waveformHeights = remember {
        listOf(
            6f, 12f, 18f, 8f, 22f,
            14f, 26f, 10f, 20f, 16f,
            28f, 6f, 18f, 12f, 24f,
            8f, 20f, 14f, 10f, 22f,
            6f, 16f, 26f, 12f, 18f
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(15.dp),
                clip = false
            ),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Play / Pause Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {

                IconButton(
                    modifier = Modifier.size(36.dp),
                    onClick = {
                        isPlaying = !isPlaying
                    }
                ) {

                    Icon(
                        imageVector =
                            if (isPlaying)
                                Icons.Default.Pause
                            else
                                Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Waveform
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                waveformHeights.forEachIndexed { index, baseHeight ->

                    val animatedHeight by infiniteTransition.animateFloat(

                        initialValue = baseHeight,

                        targetValue =
                            if (isPlaying)
                                baseHeight * 1.75f
                            else
                                baseHeight,

                        animationSpec = infiniteRepeatable(

                            animation = tween(
                                durationMillis = (380 / speedMultiplier).toInt(),
                                delayMillis = index * 22
                            ),

                            repeatMode = RepeatMode.Reverse

                        ),

                        label = "bar$index"
                    )

                    Box(
                        modifier = Modifier
                            .width(3.5.dp)
                            .height(
                                if (isPlaying)
                                    animatedHeight.dp
                                else
                                    baseHeight.dp
                            )
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isPlaying)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = .45f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(

                text = when (speedMultiplier) {
                    1.5f -> "x1.5"
                    2f -> "x2"
                    else -> "x1"
                },

                fontFamily = OutfitFont,

                fontSize = 14.sp,

                color = MaterialTheme.colorScheme.onSurface,

                modifier = Modifier.clickable {

                    speedMultiplier =
                        when (speedMultiplier) {
                            1f -> 1.5f
                            1.5f -> 2f
                            else -> 1f
                        }

                }
            )
        }
    }
}


@Composable
fun PersonCard(
    name: String,
    image: ImageBitmap? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
){

    val cardShape = RoundedCornerShape(
        topStart = 0.dp,
        topEnd = 22.dp,
        bottomStart = 22.dp,
        bottomEnd = 0.dp
    )

    Box(
        modifier = modifier
            .width(170.dp)
            .height(175.dp)
            .shadow(
                elevation = 6.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(Color(0xFFE8F7EF))
            .clickable { onClick() }
    ) {

        Box(
            modifier = Modifier
                .padding(
                    start = 13.dp,
                    top = 18.dp,
                    end = 13.dp
                )
                .fillMaxWidth()
                .height(110.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {

            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

        }

        Text(
            text = name,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 1.dp),
            fontFamily = MartelFont,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}


private val YellowAccent = Color(0xFFF5C518)

@Composable
fun BackIconButton(
    onBack: () -> Unit,
    iconRes: Int = R.drawable.ic_backgreen
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 16.dp, top = 10.dp)
            .clickable { onBack() }
    ) {

        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "Back",
            modifier = Modifier
                .size(22.dp)

        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "Back",
            color = YellowAccent,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = PompiereFont
        )
    }
}

@Composable
fun CameraPreviewPlaceholder() {

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(top = 30.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    0.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    RoundedCornerShape(18.dp)
                )
        )
    }
}

@Composable
fun GlowCaptureButton(
    size    : Dp = 90.dp,
    onClick : () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "captureGlow")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.15f,
        targetValue   = 0.55f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val glowRadius by infiniteTransition.animateFloat(
        initialValue  = 1.0f,
        targetValue   = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowRadius"
    )

    val animAlpha  = glowAlpha
    val animRadius = glowRadius

    // Just enough extra space for the subtle outer glow
    val totalSizeDp = size + 40.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(totalSizeDp)
            .drawBehind {
                // Tight glow — only slightly larger than the button itself
                val baseRadius  = size.toPx() / 2f
                val glowR       = baseRadius * animRadius * 1.3f

                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to MedGreen.copy(alpha = animAlpha),
                            0.45f to DarkGreen.copy(alpha = animAlpha * 0.9f),
                            0.75f to DarkGreen.copy(alpha = animAlpha * 0.5f),
                            1.00f to Color.Transparent
                        ),
                        radius = glowR
                    ),
                    radius = glowR
                )
            }
    ) {
        // Dark green border ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size + 10.dp)
                .clip(CircleShape)
                .border(5.dp, DarkGreen, CircleShape)
        ) {
            // Plain light green fill — no gradient
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(LightGreen)          // ← solid flat color, matches Figma
                    .clickable { onClick() }
            )
        }
    }
}
@Composable
fun SettingsRow(
    label: String,
    iconRes: Int,
    iconSize: Dp = 24.dp,
    onClick: () -> Unit

) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(15.dp),
                clip = false
            ),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface  // white/light bg
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp  // shadow comes from Modifier.shadow, not Card elevation
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                fontFamily = MartelFont,
                fontWeight = FontWeight.Normal,  // regular weight matches Figma
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(
                    MaterialTheme.colorScheme.primary  // green icon tint
                )
            )
        }
    }
}





// ─── Sizing: matches the attached code's original big(190)/small(100) values,
// alternating per column (checkerboard) exactly like the attached file did for
// its top two rows. Row 2 (new, added row) starts compact/small on both sides.
// index = row (0,1,2), value = that column's height at that row, collapsed.
private val LEFT_HEIGHTS  = listOf(190f, 100f, 24f)
private val RIGHT_HEIGHTS = listOf(100f, 190f, 24f)
private const val SLOT_SPACING = 15f
private const val BIG_HEIGHT   = 190f
private const val SMALL_HEIGHT = 24f

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

// ─── Cascading 3-row grid ──────────────────────────────────────────────────
// Rows NEVER change stacking order — row 1 is always drawn first (top), row 2
// second, row 3 last (bottom). Only their SIZE animates, via a single
// collapsed<->expanded progress value:
//
//   Collapsed (default) : row1 = BIG, row2 = MEDIUM, row3 = SMALL
//   Expanded (after tap): row1 = SMALL (matches row3's old look, content hidden),
//                         row2 = BIG   (takes over row1's old shape),
//                         row3 = MEDIUM (takes over row2's old shape, content shown)
//
// Because it's a normal Column, when row1 shrinks the rows below it
// automatically shift upward to fill the freed space — no manual offsets
// or z-index needed for the "moves above" effect.

@Composable
fun RotatingRowGrid(
    rowsData      : List<RowContent>,
    modifier      : Modifier = Modifier,
    startExpanded : Boolean = false
) {
    var expanded by remember { mutableStateOf(startExpanded) }

    val progress by animateFloatAsState(
        targetValue   = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)),
        label         = "cascadeProgress"
    )

    // Row 1 starts fully visible (big) and hides its text/icon as it shrinks to small.
    // Row 3 starts hidden (small, no content) and reveals its text/icon as it grows.
    // Row 2 is never in the "small" state, so it stays fully visible throughout.
    val row1ContentAlpha = 1f - progress
    val row3ContentAlpha = progress

    fun expand()   { expanded = true }
    fun collapse() { expanded = false }

    // heightFor(column, rowIndex): rotates that column's 3 collapsed heights
    // one step — row0 ends where row2 started, row1 ends where row0 started,
    // row2 ends where row1 started — same rotation as before, just evaluated
    // per column so left/right can keep their own big/small values.
    fun heightFor(heights: List<Float>, rowIndex: Int): Float {
        val targetIdx = ((rowIndex - 1) % 3 + 3) % 3
        return lerp(heights[rowIndex], heights[targetIdx], progress)
    }

    fun cornerFor(height: Float): Float {
        val t = ((height - SMALL_HEIGHT) / (BIG_HEIGHT - SMALL_HEIGHT)).coerceIn(0f, 1f)
        return lerp(22f, 16f, t) // small -> rounder, big -> standard 16dp
    }

    // sizeTFor(height): 0 at the small collapsed height, 1 at the big collapsed
    // height — used to lerp each item's own small/big icon & font sizes as its
    // card grows or shrinks between slots.
    fun sizeTFor(height: Float): Float =
        ((height - SMALL_HEIGHT) / (BIG_HEIGHT - SMALL_HEIGHT)).coerceIn(0f, 1f)

    fun iconSizeFor(item: MenuItemData, t: Float): Dp =
        item.iconSizeSmall + (item.iconSizeBig - item.iconSizeSmall) * t

    fun fontSizeFor(item: MenuItemData, t: Float): TextUnit =
        (item.fontSizeSmall.value + (item.fontSizeBig.value - item.fontSizeSmall.value) * t).sp

    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Left column (stacks independently, tight spacing) ──────────────
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SLOT_SPACING.dp)
        ) {
            val h0 = heightFor(LEFT_HEIGHTS, 0)
            val h1 = heightFor(LEFT_HEIGHTS, 1)
            val h2 = heightFor(LEFT_HEIGHTS, 2)
            val t0 = sizeTFor(h0)
            val t1 = sizeTFor(h1)
            val t2 = sizeTFor(h2)

            CascadeCard(
                item = rowsData[0].left, corner = cornerFor(h0), contentAlpha = row1ContentAlpha,
                iconSize = iconSizeFor(rowsData[0].left, t0), fontSize = fontSizeFor(rowsData[0].left, t0),
                onClick = { rowsData[0].left.onClick(); if (expanded) collapse() },
                modifier = Modifier.fillMaxWidth().height(h0.dp)
            )
            CascadeCard(
                item = rowsData[1].left, corner = cornerFor(h1), contentAlpha = 1f,
                iconSize = iconSizeFor(rowsData[1].left, t1), fontSize = fontSizeFor(rowsData[1].left, t1),
                onClick = { rowsData[1].left.onClick() },
                modifier = Modifier.fillMaxWidth().height(h1.dp)
            )
            CascadeCard(
                item = rowsData[2].left, corner = cornerFor(h2), contentAlpha = row3ContentAlpha,
                iconSize = iconSizeFor(rowsData[2].left, t2), fontSize = fontSizeFor(rowsData[2].left, t2),
                onClick = { rowsData[2].left.onClick(); if (!expanded) expand() },
                modifier = Modifier.fillMaxWidth().height(h2.dp)
            )
        }

        // ── Right column (stacks independently, tight spacing) ─────────────
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SLOT_SPACING.dp)
        ) {
            val h0 = heightFor(RIGHT_HEIGHTS, 0)
            val h1 = heightFor(RIGHT_HEIGHTS, 1)
            val h2 = heightFor(RIGHT_HEIGHTS, 2)
            val t0 = sizeTFor(h0)
            val t1 = sizeTFor(h1)
            val t2 = sizeTFor(h2)

            CascadeCard(
                item = rowsData[0].right, corner = cornerFor(h0), contentAlpha = row1ContentAlpha,
                iconSize = iconSizeFor(rowsData[0].right, t0), fontSize = fontSizeFor(rowsData[0].right, t0),
                onClick = { rowsData[0].right.onClick(); if (expanded) collapse() },
                modifier = Modifier.fillMaxWidth().height(h0.dp)
            )
            CascadeCard(
                item = rowsData[1].right, corner = cornerFor(h1), contentAlpha = 1f,
                iconSize = iconSizeFor(rowsData[1].right, t1), fontSize = fontSizeFor(rowsData[1].right, t1),
                onClick = { rowsData[1].right.onClick() },
                modifier = Modifier.fillMaxWidth().height(h1.dp)
            )
            CascadeCard(
                item = rowsData[2].right, corner = cornerFor(h2), contentAlpha = row3ContentAlpha,
                iconSize = iconSizeFor(rowsData[2].right, t2), fontSize = fontSizeFor(rowsData[2].right, t2),
                onClick = { rowsData[2].right.onClick(); if (!expanded) expand() },
                modifier = Modifier.fillMaxWidth().height(h2.dp)
            )
        }
    }
}



@Composable
fun CascadeCard(
    item         : MenuItemData,
    corner       : Float,
    contentAlpha : Float,
    iconSize     : Dp,
    fontSize     : TextUnit,
    onClick      : () -> Unit,
    modifier     : Modifier = Modifier
) {

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(corner.dp),
        colors = CardDefaults.cardColors(containerColor = LightGreen),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier.fillMaxSize().padding(8.dp)
        ) {
            CardContent(
                item     = item,
                iconSize = iconSize,
                fontSize = fontSize,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha }
            )
        }
    }
}



@Composable
fun CardContent(
    item     : MenuItemData,
    iconSize : Dp,
    fontSize : TextUnit,
    modifier : Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier            = modifier
    ) {
        Image(
            painter            = painterResource(id = item.iconRes),
            contentDescription = item.label,
            modifier           = Modifier.size(iconSize)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = item.label,
            fontSize   = fontSize,
            fontFamily = MartelFont,
            fontWeight = FontWeight.Normal,
            color      = TextDark,
            lineHeight = fontSize,
            textAlign  = TextAlign.Center
        )
    }
}


data class MenuItemData(
    val label        : String,
    val iconRes      : Int,
    val iconSizeSmall: Dp       = 32.dp,
    val iconSizeBig  : Dp       = 44.dp,
    val fontSizeSmall: TextUnit = 14.sp,
    val fontSizeBig  : TextUnit = 17.sp,
    val onClick      : () -> Unit = {}
)

// One horizontal row of the grid: a left button + a right button.
// Identity never changes — only which vertical slot (and therefore
// size/shape) it currently occupies changes.
data class RowContent(
    val left  : MenuItemData,
    val right : MenuItemData
)