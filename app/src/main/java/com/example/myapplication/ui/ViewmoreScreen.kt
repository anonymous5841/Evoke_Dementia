package com.example.myapplication.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import com.example.myapplication.ui.components.BottomNavBar
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.theme.PompiereFont
import com.example.myapplication.ui.components.LocationPickerField
import android.os.Build
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex


// ══════════════════════════════════════════════════════
// WAVEFORM HEIGHTS — top level of file
// FIX: was inside Row causing 'private' modifier error and scope errors
// ══════════════════════════════════════════════════════
private val waveformHeights = listOf(
    6f, 12f, 18f, 8f, 22f, 14f, 26f, 10f, 20f, 16f,
    28f, 6f, 18f, 12f, 24f, 8f, 20f, 14f, 10f, 22f,
    6f, 16f, 26f, 12f, 18f
)

// ══════════════════════════════════════════════════════
// WAVEFORM ANIMATED — top level of file
// FIX: was defined inside Row composable causing all Unresolved reference errors
// [ANIMATION SPEED] change 380f
// [BAR WIDTH] change 3.5.dp
// [BAR COLOR] change Color(0xFF3E634F)
// ══════════════════════════════════════════════════════
@Composable
fun WaveformAnimated(
    modifier: Modifier = Modifier,
    speedMultiplier: Float = 1f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val baseDuration = (380f / speedMultiplier).toInt()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly, //changed space
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveformHeights.forEachIndexed { index, baseHeight ->
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = baseHeight,
                targetValue = baseHeight * 1.75f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = baseDuration,
                        delayMillis = index * 22
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3E634F))
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// WAVEFORM STATIC — top level of file
// FIX: was defined inside Row causing Unresolved reference errors
// [BAR COLOR] change Color(0xFF8FB89A)
// ══════════════════════════════════════════════════════
@Composable
fun WaveformStatic(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        waveformHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF8FB89A))
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// VIEW MORE SCREEN
// ══════════════════════════════════════════════════════
@Composable
fun ViewMoreScreen(
    onBackClick: () -> Unit = {},
    onEllipseClick: () -> Unit = {},
    onDeleteClick: (Int) -> Unit = {},
    onDateClick: (Int) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val recordings = remember { mutableStateListOf(1, 2, 3) }
    // FIX: isPlaying and speedMultiplier at screen level — passed down to RecordingItem
    // [PER CARD STATES] each card index has independent play/speed state
    val playingStates = remember { mutableStateMapOf<Int, Boolean>() }
    val speedStates = remember { mutableStateMapOf<Int, Float>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 36.dp) //new
    ) {
        //Spacer(modifier = Modifier.height(10.dp))

        // Back button row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onBackClick() }
                .padding(top = 20.dp)

        ) {
            Icon(
                painter = painterResource(id = R.drawable.back_icon), // [BACK ICON FILE] = back_icon.xml
                contentDescription = "Back",
                tint = Color(0xFF3E634F),
                modifier = Modifier.size(28.dp)                       // [BACK ICON SIZE]
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Back",
                color = Color(0xFFFFC006),                            // [BACK TEXT COLOR] = yellow
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PompiereFont                             // [BACK TEXT FONT] = Pompiere
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
// Search bar outer wrapper — allows ellipse and icon to move independently
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            // ── SEARCH INPUT BOX ──────────────
            // CHANGED: wrapped in outer Box, layered shadow approach
            // same pattern as SearchLocationScreen search input box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                // ── SHADOW LAYER for search input box ──
                // ADDED: separate shadow Box behind real input box
                // REMOVED: old .shadow() directly on the input Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .offset(x = 0.dp, y = 6.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(10.dp),  // matches existing 10.dp corner
                            clip = false,
                            ambientColor = Color(0xFF000000),
                            spotColor = Color(0xFF000000)
                        )
                        .background(Color.Transparent)
                )

                // ── REAL SEARCH INPUT BOX — on top, unchanged ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))        // unchanged
                        .background(Color(0xFFDBE1DD)),         // unchanged
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Search by date",
                                    color = Color(0xFF555555),
                                    fontFamily = MartelFont
                                )
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── SEARCH ELLIPSE — background shape only, fully independent ────────
            // CHANGED: wrapped in outer Box, layered shadow approach
            // same pattern as SearchLocationScreen search ellipse
            // REMOVED: old .shadow(elevation=60.dp) directly on ellipse Box
            // [ELLIPSE SIZE] change size(100.dp, 80.dp)
            // [ELLIPSE OFFSET X] change x — positive = right, negative = left
            // [ELLIPSE OFFSET Y] change y — negative = up, positive = down
            Box(
                modifier = Modifier
                    .size(100.dp, 80.dp)                    // unchanged
                    .align(Alignment.CenterEnd)              // unchanged
                    .offset(
                        x = -17.dp,                          // unchanged
                        y = -7.dp                            // unchanged
                    )
            ) {
                // ── SHADOW LAYER for search ellipse ──
                // ADDED: separate shadow Box behind real ellipse shape
                Box(
                    modifier = Modifier
                        .size(70.dp, 80.dp)
                        .offset(x = 15.dp, y = -3.dp)         // shifts shadow down below ellipse
                        .shadow(
                            elevation = 9.dp,
                            shape = RoundedCornerShape(50.dp), // matches existing clip shape
                            clip = false,
                            ambientColor = Color(0xFF444444),
                            spotColor = Color(0xFF444444)
                        )
                        .background(Color.Transparent)
                )

                // ── REAL SEARCH ELLIPSE — on top, unchanged ──
                Image(
                    painter = painterResource(id = R.drawable.search_shape),
                    contentDescription = "Search Background",
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(50.dp))     // unchanged
                )
            }

            // ── SEARCH ICON — fully independent of ellipse, unchanged ────────
            // [SEARCH ICON SIZE] change size(24.dp)
            // [SEARCH ICON OFFSET X] change x — positive = right, negative = left
            // [SEARCH ICON OFFSET Y] change y — negative = up, positive = down
            Icon(
                painter = painterResource(id = R.drawable.search_icon),
                contentDescription = "Search",
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(25.dp)                             // unchanged
                    .align(Alignment.CenterEnd)              // unchanged
                    .offset(
                        x = -51.dp,                          // unchanged
                        y = -12.dp                           // unchanged
                    )
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
        // List of recordings
        recordings.forEachIndexed { index, _ ->
            RecordingItem(
                index = index,
                onDeleteClick = { onDeleteClick(index) },
                onDateClick = { onDateClick(index) },
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
            Spacer(modifier = Modifier.height(28.dp))
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
    onDateClick: () -> Unit,
    onEllipseClick: () -> Unit,
    isPlaying: Boolean,
    speedMultiplier: Float,
    onPlayPauseClick: () -> Unit,
    onSpeedClick: () -> Unit
) {
    var selectedLocation by remember { mutableStateOf("") }   // ← add this

    Column {
        // Delete + Date (Outside card)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── DELETE ICON with layered shadow ──────────────
            Box(modifier = Modifier.size(34.dp)) {

                // SHADOW LAYER
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .offset(x = 4.dp, y = -5.dp)          // [ICON SHADOW OFFSET Y]
                        .shadow(
                            elevation = 4.dp,                 // [ICON SHADOW ELEVATION]
                            shape = RoundedCornerShape(50.dp),
                            clip = false,
                            ambientColor = Color(0xFFAAAAAA), // [ICON SHADOW COLOR]
                            spotColor = Color(0xFFAAAAAA)
                        )
                        .background(Color.Transparent)
                )

                // REAL ICON
                Icon(
                    painter = painterResource(id = R.drawable.delete_icon),
                    contentDescription = "Delete",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onDeleteClick() }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── DATE TEXT with layered shadow ────────────────
            // FIX: use wrapContentSize() on outer Box — no empty space
            Box(modifier = Modifier.wrapContentSize()) {  // [FIX] wrapContentSize not fillMaxWidth

                // REAL TEXT first — so shadow Box can measure it
                Text(
                    text = "Date",
                    fontSize = 25.sp,                         // [DATE TEXT SIZE]
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    fontFamily = MartelFont,
                    modifier = Modifier.clickable { onDateClick() }
                )

                // SHADOW LAYER — drawn behind using same size as text
                // FIX: placed after Text so it uses measured size, then drawn behind via zIndex
                Box(
                    modifier = Modifier
                        .height(35.dp)
                        .width(75.dp)
                        .offset(x = -6.dp, y = 3.dp)          // [TEXT SHADOW OFFSET Y]
                        .shadow(
                            elevation = 4.dp,                 // [TEXT SHADOW ELEVATION]
                            shape = RoundedCornerShape(35.dp),
                            clip = false,
                            ambientColor = Color(0xFFAAAAAA), // [TEXT SHADOW COLOR]
                            spotColor = Color(0xFFAAAAAA)
                        )
                        .background(Color.Transparent)
                        .zIndex(-1f)                          // [FIX] pushes shadow behind text
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
                    .background(Color(0xFFDDF2E4))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Location",    //Ag paragraph/medium font
                        fontSize = 14.sp,
                        color = Color.Black,
                        fontFamily = OutfitFont
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MaterialTheme(
                        colorScheme = MaterialTheme.colorScheme.copy(
                            primary = Color(0xFF3E634F)   // ← your desired icon color, scoped to just this block
                        )
                    ) {
                        LocationPickerField(
                            value = selectedLocation,
                            placeholder = "Open location in map",
                            onClick = { /* open map or location picker here */ },
                            modifier = Modifier.fillMaxWidth(0.70f),
                            backgroundColor = Color(0xFFC1D7C8),
                            showShadow = false
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Discussion Summary in Voice",   //Ag paragraph medium
                        fontSize = 15.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 17.dp,8.dp)
                    )
                }
            }
// ── GREEN ELLIPSE — background shape only, fully independent ──────────
// [ELLIPSE SIZE] change size(69.dp, 64.dp)
// [ELLIPSE OFFSET X] positive = right, negative = left
// [ELLIPSE OFFSET Y] negative = up, positive = down
            Box(
                modifier = Modifier
                    .size(80.dp, 80.dp)
                    .align(Alignment.TopEnd)
                    .offset(
                        x = 8.dp,
                        y = (-19).dp
                    )
            ) {
                // ── SHADOW LAYER — same image, darkened and blurred underneath ──
                // [SHADOW OFFSET X] change 3.dp — how far shadow goes right
                // [SHADOW OFFSET Y] change 6.dp — how far shadow goes down
                // [SHADOW ALPHA] change 0.6f — higher = darker shadow
                // [SHADOW BLUR] change BlurEffect radius — higher = more blurred
                Image(
                    painter = painterResource(id = R.drawable.add_shape),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        Color.Gray,              // CHANGED: was Color.Black, now dark grey
                        BlendMode.SrcIn
                    ),
                    modifier = Modifier
                        .matchParentSize()
                        .offset(x = -3.dp, y = 3.dp)    // CHANGED: was y=4.dp, increased to 6.dp for more drop below
                        .graphicsLayer {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                renderEffect = BlurEffect(
                                    radiusX = 10f,      // CHANGED: was 6f, increased for more spread
                                    radiusY = 10f,      // CHANGED: was 6f, increased for more spread
                                    edgeTreatment = TileMode.Decal
                                )
                            }
                            alpha = 0.6f               // CHANGED: was 0.35f, increased for darker shadow
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                )

                // ── REAL IMAGE on top — unchanged ──────────────────────────
                Image(
                    painter = painterResource(id = R.drawable.add_shape),
                    contentDescription = "Ellipse background",
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { onEllipseClick() }
                )
            }
// ── PLUS TEXT — fully independent of ellipse ──────────
// [PLUS TEXT SIZE] change fontSize 30.sp
// [PLUS TEXT OFFSET X] positive = right, negative = left
// [PLUS TEXT OFFSET Y] negative = up, positive = down
            Text(
                text = "+",
                color = Color(0xFFFFC006),
                fontSize = 60.sp,                        // [PLUS TEXT SIZE] — matches old icon size(30.dp)
                fontFamily = MartelFont,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)              // [PLUS TEXT ALIGNMENT] change freely
                    .offset(
                        x = (-10).dp,                      // [PLUS TEXT OFFSET X] tune to land on ellipse center
                        y = (-25).dp                          // [PLUS TEXT OFFSET Y] tune to land on ellipse center
                    )
            )
        // ══════════════════════════════════════════════
        // RECORDING BOX
        // FIX: WaveformAnimated, WaveformStatic, isPlaying,
        // speedMultiplier all now resolve correctly
        // [RECORDING BOX WIDTH] change 333.dp
        // [RECORDING BOX HEIGHT] change 60.dp
        // [RECORDING BOX COLOR] change Color(0xFFC1D7C8)
        // [RECORDING OVERLAP] change offset y value
        // ══════════════════════════════════════════════
        Box(
            modifier = Modifier
                .width(280.dp)                               // [RECORDING BOX WIDTH]
                .height(60.dp)                               // [RECORDING BOX HEIGHT]
                .align(Alignment.BottomCenter)        // ← anchor to BOTTOM of the 190dp wrapper
                .offset(y = 15.dp)                     // ← positive now, pushes it DOWN from the bottom edge (the overlap)                       // [RECORDING OVERLAP]
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFC1D7C8))               // [RECORDING BOX COLOR]
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart

        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play / Pause Button
                IconButton(onClick = { onPlayPauseClick() },
                    modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(
                            id = if (isPlaying) R.drawable.play_icon
                            else R.drawable.pause_icon
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) Color(0xFF3E634F) else Color.Black,  // green when playing, black when paused
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))

                // Waveform — fills remaining space between button and speed text
                if (isPlaying) {
                    WaveformAnimated(
                        modifier = Modifier.weight(1f),
                        speedMultiplier = speedMultiplier
                    )
                } else {
                    WaveformStatic(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Speed text — right corner of recording box
                Text(
                    text = when (speedMultiplier) {
                        1.5f -> "x1.5"
                        2f -> "x2"
                        else -> "x1"
                    },
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitFont,
                    modifier = Modifier
                        .width(40.dp)                    // ← ADD THIS — fixed width, enough for "x1.5"
                        .clickable { onSpeedClick() }
                )
            }
        }
        //BottomNavBar {  }
    }
}
    }