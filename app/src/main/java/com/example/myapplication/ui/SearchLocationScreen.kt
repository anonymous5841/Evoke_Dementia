package com.example.myapplication.ui


import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.BottomNavBar
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import com.example.myapplication.ui.components.LocationPickerField
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.theme.PompiereFont
import com.example.myapplication.ui.components.ShadowButton


@Composable
fun SearchLocationScreen(
    onBackClick: () -> Unit = {},
    onEllipseClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }   // ADDED — was missing, needed by LocationPickerField below

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 36.dp)
    ) {
        // Back button row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onBackClick() }
                .padding(top = 20.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.back_icon),
                contentDescription = "Back",
                tint = Color(0xFF3E634F),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Back",
                color = Color(0xFFFFC006),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = PompiereFont
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Search bar + yellow location button, side by side
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Search bar outer wrapper — allows ellipse and icon to move independently
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
            ) {
                // ── SEARCH INPUT BOX ──────────────
                // CHANGED: wrapped in outer Box to apply layered shadow approach
                // (same pattern as PopupCard yellow ellipse)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    // ── SHADOW LAYER for search input box ──
                    // ADDED: separate shadow Box sitting behind the real input box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .offset(x = 0.dp, y = 6.dp)        // ADDED: shifts shadow down so it shows below
                            .shadow(
                                elevation = 8.dp,               // ADDED: shadow elevation
                                shape = RoundedCornerShape(20.dp), // ADDED: matches input box corner
                                clip = false,                   // ADDED: shadow renders outside boundary
                                ambientColor = Color(0xFF000000), // ADDED: dark shadow color
                                spotColor = Color(0xFF000000)   // ADDED: dark shadow spot color
                            )
                            .background(Color.Transparent)      // ADDED: gives shadow a surface to render against
                    )

                    // ── REAL SEARCH INPUT BOX — on top ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(20.dp))    // unchanged
                            .background(Color(0xFFDBE1DD)),     // unchanged
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
                                        "Search by name",
                                        color = Color(0xFF555555),
                                        fontFamily = MartelFont
                                        //modifier = Modifier.offset(y = (-5).dp)
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

                // ── SEARCH ELLIPSE ──────────────
                // CHANGED: wrapped in outer Box to apply layered shadow approach
                Box(
                    modifier = Modifier
                        .size(100.dp, 100.dp)
                        .align(Alignment.CenterEnd)
                        .offset(
                            x = -1.dp,
                            y = -9.dp
                        )
                ) {
                    // ── SHADOW LAYER for search ellipse ──
                    // ADDED: separate shadow Box behind the real ellipse shape
                    Box(
                        modifier = Modifier
                            .size(60.dp, 90.dp)
                            .offset(x = 18.dp, y = -3.dp)        // ADDED: shifts shadow down below ellipse
                            .shadow(
                                elevation = 9.dp,               // ADDED: shadow elevation
                                shape = RoundedCornerShape(50.dp), // ADDED: matches ellipse clip shape
                                clip = false,                   // ADDED: shadow renders outside boundary
                                ambientColor = Color(0xFF444444), // ADDED: dark shadow color
                                spotColor = Color(0xFF444444)   // ADDED: dark shadow spot color
                            )
                            .background(Color.Transparent)      // ADDED: gives shadow a surface to render against
                    )

                    // ── REAL SEARCH ELLIPSE — on top ──
                    Image(
                        painter = painterResource(id = R.drawable.search_shape),
                        contentDescription = "Search Background",
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(50.dp))    // unchanged
                    )
                }

                // ── SEARCH ICON — unchanged ──────────────
                Icon(
                    painter = painterResource(id = R.drawable.search_icon),
                    contentDescription = "Search",
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(25.dp)
                        .align(Alignment.CenterEnd)
                        .offset(
                            x = -37.dp,
                            y = -14.dp
                        )
                )
            }   // ← closes search bar outer Box

            // ── YELLOW LOCATION BUTTON ──────────────
            // CHANGED: wrapped in outer Box to apply layered shadow approach
            Box(
                modifier = Modifier
                    .size(65.dp, 56.dp)
            ) {
                // ── SHADOW LAYER for yellow button ──
                // ADDED: separate shadow Box behind the real yellow button
                Box(
                    modifier = Modifier
                        .size(65.dp, 56.dp)
                        .offset(x = 0.dp, y = 6.dp)            // ADDED: shifts shadow down below button
                        .shadow(
                            elevation = 8.dp,                   // ADDED: shadow elevation
                            shape = RoundedCornerShape(12.dp),  // ADDED: matches yellow button clip shape
                            clip = false,                       // ADDED: shadow renders outside boundary
                            ambientColor = Color(0xFF000000),   // ADDED: dark shadow color
                            spotColor = Color(0xFF000000)       // ADDED: dark shadow spot color
                        )
                        .background(Color.Transparent)          // ADDED: gives shadow a surface to render against
                )

                // ── REAL YELLOW BUTTON — on top ──
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(65.dp, 56.dp)
                        .clip(RoundedCornerShape(12.dp))        // unchanged
                        .background(Color(0xFFFFCD38))          // unchanged
                        .clickable { /* open map/location */ }  // unchanged
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.location_icon),
                        contentDescription = "Location",
                        tint = Color.Unspecified,               // unchanged
                        modifier = Modifier.size(27.dp)         // unchanged
                    )
                }
            }
        }// ← closes the new Row wrapping both

        Spacer(modifier = Modifier.height(20.dp))
        // 📍 Scrollable list of location fields
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.weight(1f)   // ← now resolves correctly since Column wasn't closed early
        ) {
            items(5) { index ->
                Column {
                    // Distance label
                    Text(
                        text = "Distance",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MartelFont,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    // Outer wrapper — NOT clipped, lets ellipse and plus text escape freely
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)   // match whatever height you want the overall area to reserve
                    ) {
                        // Location box — clipping happens HERE only, on the card background
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                                    clip = false
                                )
                                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                                .background(Color(0xFFDDF2E4))
                        ) {
                            // Location text
                            Text(
                                text = "Location",
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontFamily = OutfitFont,
                                fontWeight = FontWeight.Normal,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 8.dp, top = 6.dp)
                            )
                        }

                        // ── GREEN ELLIPSE — now a sibling of Location box, NOT clipped by it ──────
// [ELLIPSE SIZE] change size(80.dp, 80.dp)
// [ELLIPSE OFFSET X] positive = right, negative = left
// [ELLIPSE OFFSET Y] negative = up, positive = down
                        // ── GREEN ELLIPSE — now a sibling of Location box, NOT clipped by it ──────
// [ELLIPSE SIZE] change size(80.dp, 80.dp)
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
                            // ── SHADOW LAYER — blurred dark copy of the real shape ──
                            // RESTORED: BlurEffect approach traces the actual irregular shape outline
                            // CHANGED: alpha increased from 0.35f to 0.6f for darker shadow
                            // CHANGED: offset increased from y=4.dp to y=6.dp for more visible drop
                            Image(
                                painter = painterResource(id = R.drawable.add_shape),
                                contentDescription = null,
                                colorFilter = ColorFilter.tint(
                                    Color.Gray,
                                    BlendMode.SrcIn
                                ),
                                modifier = Modifier
                                    .height(200.dp)
                                    .width(200.dp)
                                    .offset(x = -3.dp, y = 4.dp)        // CHANGED: was y=4.dp, increased for more shadow drop
                                    .graphicsLayer {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            renderEffect = BlurEffect(
                                                radiusX = 10f,            // CHANGED: was 6f, slightly more blur spread
                                                radiusY = 10f,            // CHANGED: was 6f, slightly more blur spread
                                                edgeTreatment = TileMode.Decal
                                            )
                                        }
                                        alpha = 0.6f                     // CHANGED: was 0.35f, darker shadow
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                            )

                            // ── REAL GREEN SHAPE — on top, unchanged ──
                            Image(
                                painter = painterResource(id = R.drawable.add_shape),
                                contentDescription = "Ellipse background",
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { onEllipseClick() }
                            )
                        }  // ← closes GREEN ELLIPSE Box

                        // ── PLUS TEXT — fully independent of ellipse and Location box ──────
                        Text(
                            text = "+",
                            color = Color(0xFFFFC006),
                            fontSize = 60.sp,
                            fontFamily = MartelFont,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(
                                    x = (-10).dp,
                                    y = (-32).dp
                                )
                        )
                    }   // ← closes outer unclipped wrapper

                        // ── LOCATION PICKER FIELD ──────────────
                        MaterialTheme(
                            colorScheme = MaterialTheme.colorScheme.copy(
                                primary = Color(0xFF3E634F)
                            )
                        ) {
                            LocationPickerField(
                                value = selectedLocation,
                                placeholder = "Open location in map",
                                onClick = { /* open map or location picker here */ },
                                modifier = Modifier.fillMaxWidth(0.70f)
                                .offset(
                                    x = 12.dp,      // [LOCATION PICKER OFFSET X] positive = right, negative = left
                                    y = -22.dp       // [LOCATION PICKER OFFSET Y] negative = up, positive = down
                            ),
                                backgroundColor = Color(0xFFC1D7C8),
                                showShadow = true
                            )
                        }
                    }   // ← closes Location box (was missing in your version — this Box needed to close BEFORE the Column below, not after Spacer)

                    Spacer(modifier = Modifier.height(16.dp))
                }   // ← closes per-item Column
            }   // ← closes items(5) { ... }
        }   // ← closes LazyColumn
    }   // ← closes screen's main Column
