package com.example.myapplication.ui
//Viremorescreen.kt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.components.LocationPickerField
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import com.example.myapplication.ui.components.AddShapeButton
import com.example.myapplication.ui.components.BackIconButton
import com.example.myapplication.ui.components.IconWithShadow
import com.example.myapplication.ui.components.SearchFieldWithIcon
import com.example.myapplication.ui.components.VoicePlayerBar
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle

// ══════════════════════════════════════════════════════
// VIEW MORE SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun ViewMoreScreen(
    onBackClick: () -> Unit = {},
    onEllipseClick: () -> Unit = {},
    onDeleteClick: (Int) -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }
    val recordings = remember { mutableStateListOf(1, 2, 3) }
    // FIX: isPlaying and speedMultiplier at screen level — passed down to RecordingItem
    // [PER CARD STATES] each card index has independent play/speed state
    val playingStates = remember { mutableStateMapOf<Int, Boolean>() }
    val speedStates = remember { mutableStateMapOf<Int, Float>() }
    val appColors = AppTheme.colors

    Scaffold(
        containerColor = appColors.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Back button row
            BackIconButton(
                onBackClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search bar outer wrapper — allows ellipse and icon to move independently
            // ── SEARCH INPUT BOX ──────────────
            // CHANGED: wrapped in outer Box, layered shadow approach
            // same pattern as SearchLocationScreen search input box
            SearchFieldWithIcon(
                placeholder = "Search by Date",
                value = searchQuery,
                onValueChange = { searchQuery = it },
                greenIconOffset = DpOffset(x = (-20).dp, y = (-1).dp),
                searchIconOffset = DpOffset(x = (4.4).dp, y = (-9).dp),
                boxWidth = null   // ← fills available width instead of a fixed 280.dp
            )

            Spacer(modifier = Modifier.height(35.dp))
            // List of recordings
            recordings.forEachIndexed { index, _ ->
                RecordingItem(
                    index = index,
                    onDeleteClick = { onDeleteClick(index) },
                    onEllipseClick = onEllipseClick,
                    isPlaying = playingStates[index] ?: false,         // each card independent
                    speedMultiplier = speedStates[index] ?: 1f,        // each card independent
                    onPlayPauseClick = {
                        // only toggles THIS card's play state
                        playingStates[index] = !(playingStates[index] ?: false)
                    },
                    onSpeedClick = {
                        // only changes THIS card's speed
                        val current = speedStates[index] ?: 1f
                        speedStates[index] = when (current) {
                            1f -> 1.5f
                            1.5f -> 2f
                            else -> 1f
                        }
                    }
                )
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// RECORDING ITEM
// FIX: isPlaying, speedMultiplier, onPlayPauseClick, onSpeedClick
// now passed as parameters instead of defined inside
// ══════════════════════════════════════════════════════
@Composable
fun RecordingItem(
    index: Int,
    onDeleteClick: () -> Unit,
    onEllipseClick: () -> Unit,
    isPlaying: Boolean,
    speedMultiplier: Float,
    onPlayPauseClick: () -> Unit,
    onSpeedClick: () -> Unit
) {
    var selectedLocation by remember { mutableStateOf("") }   // ← add this
    val appColors = AppTheme.colors
    Column {
        // Delete + Date (Outside card)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ── DELETE ICON with layered shadow ──────────────
            IconWithShadow(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(27.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // ── DATE TEXT with layered shadow ────────────────
            // FIX: use wrapContentSize() on outer Box — no empty space
            Box(modifier = Modifier.wrapContentSize()) {  // [FIX] wrapContentSize not fillMaxWidth

                Text(
                    text = "24/5/2026",
                    fontSize = 22.sp,                         // [DATE TEXT SIZE]
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    fontFamily = MartelFont,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.4f), // [SHADOW OPACITY]
                            offset = Offset(x = 5f, y = 4f),          // [SHADOW OFFSET]
                            blurRadius = 5f                            // [SHADOW BLUR]
                        )
                    ),
                    modifier = Modifier.offset(y = 2.dp)
                )

            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Outer wrapper — NOT clipped, lets ellipse and icon escape freely
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            // Main Location Box — clipping happens HERE only, on the card background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(
                        topStart = 0.dp,       // ← top-left corner now square
                        topEnd = 16.dp,        // unchanged
                        bottomStart = 16.dp,   // unchanged
                        bottomEnd = 16.dp      // unchanged
                    ))
                    .clip(RoundedCornerShape(
                        topStart = 0.dp,       // ← top-left corner now square
                        topEnd = 16.dp,        // unchanged
                        bottomStart = 16.dp,   // unchanged
                        bottomEnd = 16.dp      // unchanged
                    ))
                    .background(appColors.boxOuter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Location",    //Ag paragraph/medium font
                        fontSize = 18.sp,
                        color = Color.Black,
                        fontFamily = OutfitFont
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LocationPickerField(
                        value = selectedLocation,
                        placeholder = "Open location in map",
                        onClick = { /* open map or location picker here */ },
                        modifier = Modifier.fillMaxWidth(0.70f),
                        backgroundColor = appColors.boxInner,
                        showShadow = false
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Discussion Summary in Voice",   //Ag paragraph medium
                        fontSize = 17.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 17.dp,8.dp)
                    )
                }
            }
            AddShapeButton(
                onClick = onEllipseClick,
                shapeColor = appColors.pagesText,
                plusColor = appColors.popupText,
                shapeShadowHeight = 70.dp,
                shapeShadowWidth = 88.dp,
                boxOffset = DpOffset(x = (280).dp, y = (-42).dp),
                plusFontSize = 67.sp,
                plusOffset = DpOffset(x = (271).dp, y = (-31).dp)
            )

            VoicePlayerBar(
                isPlaying = isPlaying,
                speedMultiplier = speedMultiplier,
                onPlayPauseClick = onPlayPauseClick,
                onSpeedClick = onSpeedClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 20.dp)
            )
        }
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ViewMorePreview(){
    GreenTheme {
        ViewMoreScreen()
    }}