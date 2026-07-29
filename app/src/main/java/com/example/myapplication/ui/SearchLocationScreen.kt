package com.example.myapplication.ui

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.ui.components.AddShapeButton
import com.example.myapplication.ui.components.BackIconButton
import com.example.myapplication.ui.components.LocationPickerField
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.components.ShadowButton
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.components.SearchFieldWithIcon
import androidx.compose.ui.res.stringResource

@Composable
fun SearchLocationScreen(
    onBack: () -> Unit = {},
    onEllipseClick: () -> Unit = {},
    onSearch: () -> Unit = {},

    ) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    val appColors = AppTheme.colors

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // insets already handled at root
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
                onBack
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),   // ← needed so there's room to push into
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                SearchFieldWithIcon(
                    value = searchQuery,
                    onValueChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.width(12.dp))

                ShadowButton(

                    width = 66.dp,

                    height = 51.dp,

                    color = appColors.popupText,

                    cornerRadius = 16.dp,

                    onClick = onSearch

                ) {

                    // Replace with your search drawable later
                    Icon(
                        painter = painterResource(R.drawable.location_icon),
                        contentDescription = stringResource(R.string.search),
                        tint = appColors.pagesText,

                        )
                }
            }

            Spacer(modifier = Modifier.height(37.dp))

            // 📍 Scrollable list of location fields
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(5) { index ->
                    Column {
                        // Distance label
                        Text(
                            text = stringResource(R.string.distance_format, 104),
                            color = Color.Black,
                            fontSize = 19.sp,
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
                                        shape = RoundedCornerShape(bottomStart = 16.dp),
                                        clip = false
                                    )
                                    .clip(RoundedCornerShape(bottomStart = 16.dp))
                                    .background(appColors.boxOuter)
                            ) {
                                // Location text
                                Text(
                                    text = stringResource(R.string.location),
                                    color = Color.Black,
                                    fontSize = 17.sp,
                                    fontFamily = OutfitFont,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 8.dp, top = 6.dp)
                                )
                            }
                            AddShapeButton(
                                onClick = onEllipseClick,
                                shapeColor = appColors.pagesText,
                                plusColor = appColors.popupText,
                            )

                        }   // ← closes outer unclipped wrapper

                        // ── LOCATION PICKER FIELD ──────────────
                        LocationPickerField(
                            value = selectedLocation,
                            placeholder = stringResource(R.string.open_location_in_map),
                            onClick = { /* open map or location picker here */ },
                            modifier = Modifier
                                .fillMaxWidth(0.70f)
                                .offset(
                                    x = 15.dp,      // [LOCATION PICKER OFFSET X] positive = right, negative = left
                                    y = -22.dp       // [LOCATION PICKER OFFSET Y] negative = up, positive = down
                                ),
                            backgroundColor = appColors.boxInner,
                            showShadow = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }   // ← closes per-item Column
                }   // ← closes items(5) { ... }
            }   // ← closes LazyColumn
        }   // ← closes screen's main Column
    }   // ← closes Scaffold
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchLocationreview(){
    GreenTheme {
        SearchLocationScreen()
    }
}