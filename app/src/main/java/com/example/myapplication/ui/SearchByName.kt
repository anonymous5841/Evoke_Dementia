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
import androidx.compose.ui.unit.Dp

import androidx.compose.runtime.*
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
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onBack: () -> Unit = {},
    onPersonClick: () -> Unit = {},
    onMoreInfoLocationClick: () -> Unit = {},
) {

    val appColors = AppTheme.colors
    val bottomNavHeight = rememberBottomNavBarHeight()

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * RESPONSIVE DIMENSIONS
             */

            // Overall horizontal screen padding
            val horizontalPadding = (maxWidth * 0.045f)
                .coerceIn(16.dp, 28.dp)

            // Space from top to back button
            val topSpacing = (maxHeight * 0.022f)
                .coerceIn(14.dp, 24.dp)

            // Back button → search bar
            val backSearchSpacing = (maxHeight * 0.028f)
                .coerceIn(20.dp, 32.dp)

            // Search field → results
            val searchResultsSpacing = (maxHeight * 0.035f)
                .coerceIn(28.dp, 42.dp)

            // Search field / location button gap
            val searchButtonSpacing = (maxWidth * 0.03f)
                .coerceIn(10.dp, 16.dp)

            // Location button dimensions
            val locationButtonWidth = (maxWidth * 0.17f)
                .coerceIn(58.dp, 70.dp)

            val locationButtonHeight = (maxHeight * 0.065f)
                .coerceIn(48.dp, 54.dp)

            /*
             * RESPONSIVE CONTENT
             */

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
            ) {

                /*
                 * TOP → BACK BUTTON
                 */

                Spacer(
                    modifier = Modifier.height(topSpacing)
                )

                BackIconButton(
                    onBack = onBack
                )

                /*
                 * BACK BUTTON → SEARCH BAR
                 */

                Spacer(
                    modifier = Modifier.height(backSearchSpacing)
                )

                /*
                 * SEARCH FIELD + LOCATION BUTTON
                 */

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    SearchFieldWithIcon(
                        value = viewModel.searchQuery,
                        onValueChange = {
                            viewModel.onSearchQueryChange(it)
                        }
                    )

                    Spacer(
                        modifier = Modifier.width(searchButtonSpacing)
                    )

                    ShadowButton(
                        width = locationButtonWidth,
                        height = locationButtonHeight,
                        color = appColors.popupText,
                        cornerRadius = 16.dp,
                        onClick = {
                            viewModel.onLocationButtonClick()
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                R.drawable.location_icon
                            ),
                            contentDescription = stringResource(
                                R.string.search
                            ),
                            tint = appColors.pagesText
                        )
                    }
                }

                /*
                 * SEARCH BAR → RESULTS
                 */

                Spacer(
                    modifier = Modifier.height(searchResultsSpacing)
                )

                /*
                 * RESULTS
                 */

                AnimatedContent(
                    targetState = viewModel.mode,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    modifier = Modifier.weight(1f),
                    label = "searchModeSwitch"
                ) { currentMode ->

                    when (currentMode) {

                        SearchDisplayMode.PEOPLE -> {

                            val filtered =
                                if (viewModel.searchQuery.isBlank()) {
                                    viewModel.people
                                } else {
                                    viewModel.people.filter {
                                        it.name.startsWith(
                                            viewModel.searchQuery,
                                            ignoreCase = true
                                        )
                                    }
                                }

                            PeopleResultsGrid(
                                people = filtered,
                                bottomPadding = bottomNavHeight,
                                onPersonClick = onPersonClick
                            )
                        }

                        SearchDisplayMode.LOCATION -> {

                            LocationResultsList(
                                selectedLocation =
                                    viewModel.selectedLocation,

                                bottomPadding =
                                    bottomNavHeight,

                                onLocationChange = {
                                    viewModel.onSelectedLocationChange(it)
                                },

                                onAddLocationClick =
                                    onMoreInfoLocationClick
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun PeopleResultsGrid(
    people: List<PersonItem>,
    bottomPadding: Dp,
    onPersonClick: () -> Unit
) {

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {

        /*
         * RESPONSIVE DIMENSIONS
         */

        // Gap between the two columns
        val horizontalGridSpacing = (maxWidth * 0.025f)
            .coerceIn(8.dp, 14.dp)

        // Vertical gap between cards
        val verticalGridSpacing = (maxHeight * 0.025f)
            .coerceIn(18.dp, 28.dp)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),

            horizontalArrangement =
                Arrangement.spacedBy(horizontalGridSpacing),

            verticalArrangement =
                Arrangement.spacedBy(verticalGridSpacing),

            contentPadding = PaddingValues(
                bottom = bottomPadding
            ),

            modifier = Modifier.fillMaxSize()
        ) {

            items(people) { person ->

                Box(
                    modifier = Modifier.wrapContentSize(),
                    contentAlignment = Alignment.Center
                ) {

                    PersonCard(
                        name = stringResource(R.string.name),
                        image = person.image,
                        modifier = Modifier,
                        onClick = onPersonClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationResultsList(
    selectedLocation: String,
    bottomPadding: Dp,
    onLocationChange: (String) -> Unit,
    onAddLocationClick: () -> Unit
) {

    val appColors = AppTheme.colors

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {

        // Capture BoxWithConstraints dimensions
        // before entering LazyColumn's scope.
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        /*
         * RESPONSIVE DIMENSIONS
         */

        // Distance text size
        val distanceTextSize =
            (screenWidth.value * 0.052f)
                .coerceIn(17f, 21f)
                .sp

        // Location text size
        val locationTextSize =
            (screenWidth.value * 0.046f)
                .coerceIn(15f, 19f)
                .sp

        // Distance text → location box
        val distanceBottomSpacing =
            (screenHeight * 0.007f)
                .coerceIn(4.dp, 8.dp)

        // Height of location result box
        val locationBoxHeight =
            (screenHeight * 0.075f)
                .coerceIn(56.dp, 66.dp)

        // Location picker width
        val locationPickerWidth =
            (screenWidth * 0.70f)
                .coerceIn(220.dp, 320.dp)

        // Location picker vertical offset
        val locationPickerOffsetY =
            (screenHeight * 0.027f)
                .coerceIn(18.dp, 24.dp)

        // Location picker horizontal offset
        val locationPickerOffsetX =
            (screenWidth * 0.04f)
                .coerceIn(12.dp, 20.dp)

        /*
         * ADD BUTTON POSITION
         */

        val addButtonX =
            (screenWidth * 0.84f)
                .coerceIn(250.dp, 320.dp)

        val addButtonPlusX =
            (screenWidth * 0.83f)
                .coerceIn(247.dp, 317.dp)

        /*
         * ADD BUTTON VERTICAL POSITIONS
         */

        val addButtonY =
            (screenHeight * 0.052f)
                .coerceIn(34.dp, 42.dp)

        val addButtonPlusY =
            (screenHeight * 0.035f)
                .coerceIn(22.dp, 30.dp)


        /*
         * LOCATION RESULTS
         */

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),

            modifier = Modifier.fillMaxSize(),

            contentPadding = PaddingValues(
                bottom = bottomPadding
            )
        ) {

            items(5) {

                Column {

                    /*
                     * DISTANCE
                     */

                    Text(
                        text = stringResource(
                            R.string.distance_format,
                            104
                        ),

                        color = Color.Black,

                        fontSize = distanceTextSize,

                        fontWeight = FontWeight.Bold,

                        fontFamily = MartelFont,

                        modifier = Modifier.padding(
                            start = (screenWidth * 0.01f)
                                .coerceIn(3.dp, 5.dp),

                            bottom = distanceBottomSpacing
                        )
                    )


                    /*
                     * LOCATION RESULT BOX
                     */

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(locationBoxHeight)
                    ) {

                        /*
                         * MAIN LOCATION BOX
                         */

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(
                                        bottomStart = 16.dp
                                    ),
                                    clip = false
                                )
                                .clip(
                                    RoundedCornerShape(
                                        bottomStart = 16.dp
                                    )
                                )
                                .background(
                                    appColors.boxOuter
                                )
                        ) {

                            Text(
                                text = stringResource(
                                    R.string.location
                                ),

                                color = Color.Black,

                                fontSize = locationTextSize,

                                fontFamily = OutfitFont,

                                fontWeight = FontWeight.Normal,

                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(
                                        start = (screenWidth * 0.02f)
                                            .coerceIn(7.dp, 10.dp),

                                        top = (screenHeight * 0.007f)
                                            .coerceIn(5.dp, 8.dp)
                                    )
                            )
                        }


                        /*
                         * ADD LOCATION BUTTON
                         */

                        AddShapeButton(
                            onClick = onAddLocationClick,

                            shapeColor =
                                appColors.pagesText,

                            plusColor =
                                appColors.popupText,

                            boxOffset = DpOffset(
                                x = addButtonX,
                                y = -addButtonY
                            ),

                            plusOffset = DpOffset(
                                x = addButtonPlusX,
                                y = -addButtonPlusY
                            )
                        )
                    }


                    /*
                     * LOCATION PICKER
                     */

                    LocationPickerField(
                        value = selectedLocation,

                        placeholder = stringResource(
                            R.string.open_location_in_map
                        ),

                        onClick = {
                            // open map or location picker here
                        },

                        modifier = Modifier
                            .width(locationPickerWidth)
                            .offset(
                                x = locationPickerOffsetX,
                                y = -locationPickerOffsetY
                            ),

                        backgroundColor =
                            appColors.boxInner,

                        showShadow = true
                    )
                }
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