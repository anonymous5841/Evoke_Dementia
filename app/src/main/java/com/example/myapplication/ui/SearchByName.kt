package com.example.myapplication.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.ui.theme.OutfitFont

/** Which result list the shared search screen is currently showing. */
enum class SearchDisplayMode { PEOPLE, LOCATION }

data class PersonItem(
    val name: String,
    val image: androidx.compose.ui.graphics.ImageBitmap? = null
)

@Composable
fun SearchScreen(
    people: List<PersonItem> = List(20) { PersonItem(name = "Name") },
    onBack: () -> Unit = {},
    onPersonClick: () -> Unit = {},
    onAddLocationClick: () -> Unit = {}, // was onEllipseClick in SearchLocationScreen
) {
    val appColors = AppTheme.colors
    val bottomNavHeight = rememberBottomNavBarHeight()

    var searchQuery by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    // Default view - change to LOCATION if that should be the starting state instead
    var mode by remember { mutableStateOf(SearchDisplayMode.PEOPLE) }

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            BackIconButton(onBack)

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Shared header: search field + location toggle button ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SearchFieldWithIcon(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        mode = SearchDisplayMode.PEOPLE // typing a name switches to people results
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                ShadowButton(
                    width = 66.dp,
                    height = 51.dp,
                    color = appColors.popupText,
                    cornerRadius = 16.dp,
                    onClick = { mode = SearchDisplayMode.LOCATION } // no navigation - just flips the view
                ) {
                    Icon(
                        painter = painterResource(R.drawable.location_icon),
                        contentDescription = "Search by location",
                        tint = appColors.pagesText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(37.dp))

            // ---- The part that actually swaps ----
            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "searchModeSwitch"
            ) { currentMode ->
                when (currentMode) {
                    SearchDisplayMode.PEOPLE -> PeopleResultsGrid(
                        people = people,
                        bottomPadding = bottomNavHeight,
                        onPersonClick = onPersonClick
                    )
                    SearchDisplayMode.LOCATION -> LocationResultsList(
                        selectedLocation = selectedLocation,
                        onLocationChange = { selectedLocation = it },
                        onAddLocationClick = onAddLocationClick
                    )
                }
            }
        }
    }
}

@Composable
private fun PeopleResultsGrid(
    people: List<PersonItem>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onPersonClick: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        contentPadding = PaddingValues(bottom = bottomPadding),
        modifier = Modifier.fillMaxSize()
    ) {
        items(people) { person ->
            Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
                PersonCard(
                    name = person.name,
                    image = person.image,
                    modifier = Modifier,
                    onClick = onPersonClick
                )
            }
        }
    }
}

@Composable
private fun LocationResultsList(
    selectedLocation: String,
    onLocationChange: (String) -> Unit,
    onAddLocationClick: () -> Unit
) {
    val appColors = AppTheme.colors

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(5) { index ->
            Column {
                Text(
                    text = "104km",
                    color = Color.Black,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MartelFont,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
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
                        Text(
                            text = "Location",
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
                        onClick = onAddLocationClick,
                        shapeColor = appColors.pagesText,
                        plusColor = appColors.popupText,
                    )
                }

                LocationPickerField(
                    value = selectedLocation,
                    placeholder = "Open location in map",
                    onClick = { /* open map or location picker here */ },
                    modifier = Modifier
                        .fillMaxWidth(0.70f)
                        .offset(x = 15.dp, y = (-22).dp),
                    backgroundColor = appColors.boxInner,
                    showShadow = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchScreenReview(){
    GreenTheme {
        SearchScreen()
    }
}