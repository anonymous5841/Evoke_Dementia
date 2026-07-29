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
import com.example.myapplication.ui.components.rememberBottomNavBarHeight
import androidx.compose.ui.res.stringResource
import com.example.myapplication.ui.theme.MartelFont
import com.example.myapplication.ui.theme.OutfitFont
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.navigation.SearchViewModel



/** Which result list the shared search screen is currently showing. */
enum class SearchDisplayMode { PEOPLE, LOCATION }

data class PersonItem(
    val name: String,
    val image: androidx.compose.ui.graphics.ImageBitmap? = null
)

@Composable
<<<<<<< HEAD
fun PersonListContent(
    people: List<PersonItem>? = null,
    onBack: () -> Unit = {},
    onSearch: () -> Unit = {},
    onPersonClick: () -> Unit = {},
) {
    val personList = people ?: List(20) {
        PersonItem(name = stringResource(R.string.name))
    }

=======
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onBack: () -> Unit = {},
    onPersonClick: () -> Unit = {},
    onMoreInfoLocationClick: () -> Unit = {}, // was onEllipseClick in SearchLocationScreen
) {
>>>>>>> origin/main
    val appColors = AppTheme.colors
    val bottomNavHeight = rememberBottomNavBarHeight()

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
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) }
                )

                Spacer(modifier = Modifier.width(12.dp))

                ShadowButton(
                    width = 66.dp,
                    height = 51.dp,
                    color = appColors.popupText,
                    cornerRadius = 16.dp,
                    onClick = { viewModel.onLocationButtonClick() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.location_icon),
<<<<<<< HEAD
                        contentDescription = stringResource(R.string.search),
=======
                        contentDescription = "Search by location",
>>>>>>> origin/main
                        tint = appColors.pagesText,
                    )
                }
            }

            Spacer(modifier = Modifier.height(37.dp))

<<<<<<< HEAD
            LazyVerticalGrid(

                columns = GridCells.Fixed(2),

                horizontalArrangement = Arrangement.spacedBy(10.dp),

                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(bottom = bottomNavHeight),
                modifier = Modifier.weight(1f)

            ) {

                items(personList) { person ->

                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.Center
                    ) {

                        PersonCard(

                            name = person.name,

                            image = person.image,

                            modifier = Modifier,

                            onClick = {

//                                onPersonClick(person)
                                onPersonClick()

=======
            // ---- The part that actually swaps ----
            AnimatedContent(
                targetState = viewModel.mode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "searchModeSwitch"
            ) { currentMode ->
                when (currentMode) {
                    SearchDisplayMode.PEOPLE -> {
                        val filtered = if (viewModel.searchQuery.isBlank()) {
                            viewModel.people
                        } else {
                            viewModel.people.filter {
                                it.name.startsWith(viewModel.searchQuery, ignoreCase = true)
>>>>>>> origin/main
                            }
                        }
                        PeopleResultsGrid(
                            people = filtered,
                            bottomPadding = bottomNavHeight,
                            onPersonClick = onPersonClick
                        )
                    }
                    SearchDisplayMode.LOCATION -> LocationResultsList(
                        selectedLocation = viewModel.selectedLocation,
                        bottomPadding = bottomNavHeight,
                        onLocationChange = { viewModel.onSelectedLocationChange(it) },
                        onAddLocationClick = onMoreInfoLocationClick
                    )
                }
            }
        }
    }
}

@Composable
<<<<<<< HEAD
fun PersonListPreview() {

    GreenTheme {

        PersonListContent(
            people = List(12) {
                PersonItem(
                    name = stringResource(R.string.name)
                )
            }
        )
=======
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
>>>>>>> origin/main
    }
}

@Composable
private fun LocationResultsList(
    selectedLocation: String,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onLocationChange: (String) -> Unit,
    onAddLocationClick: () -> Unit
) {
    val appColors = AppTheme.colors

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding),

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
                        boxOffset = DpOffset(x = (299).dp, y = (-39).dp),
                        plusOffset = DpOffset(x = (296).dp, y = (-26).dp),
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

//@Preview(showBackground = true, showSystemUi = true)
//@Composable
//fun SearchScreenLocationModePreview() {
//    val previewViewModel = SearchViewModel().apply {
//        onLocationButtonClick() // flips mode to LOCATION for this preview
//    }
//    GreenTheme {
//        SearchScreen(viewModel = previewViewModel)
//    }
//}