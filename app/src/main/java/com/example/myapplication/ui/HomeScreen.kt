package com.example.myapplication.ui
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.myapplication.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.components.HeaderSection
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.theme.OutfitFont
import com.example.myapplication.ui.components.MenuItemData
import com.example.myapplication.ui.components.RowContent
import com.example.myapplication.ui.components.RotatingRowGrid
import com.example.myapplication.ui.components.TextDark
import com.example.myapplication.ui.components.rememberBottomNavBarHeight
import com.example.myapplication.ui.theme.AppTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.myapplication.utils.LanguageManager
import com.example.myapplication.utils.LocalAppLanguage
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(
    onSettingsClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onRecognizeClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onDemoClick: () -> Unit = {},
    onAddLocationClick: () -> Unit = {},
    initialPage: Int = 0,
    startExpanded: Boolean = false
) {
    val appColors = AppTheme.colors
    val bottomNavHeight = rememberBottomNavBarHeight()

    val language = LocalAppLanguage.current
    val isUrdu = language == LanguageManager.URDU

    val recognizeRegister =
        stringResource(R.string.recognize_register)

    val addLocation =
        stringResource(R.string.add_location_menu)

    val help =
        stringResource(R.string.help)

    val search =
        stringResource(R.string.search_name_location)

    val demo =
        stringResource(R.string.demo)

    val settings =
        stringResource(R.string.settings)


    /*
     * -------------------------------------------------------------
     * MENU DATA
     * -------------------------------------------------------------
     *
     * These are now the BASE sizes.
     *
     * RotatingRowGrid will scale them according to the available
     * screen width.
     */

    val rowsData = remember(
        recognizeRegister,
        addLocation,
        help,
        search,
        demo,
        settings
    ) {

        listOf(

            // ------------------------------------------------------
            // ROW 1
            // ------------------------------------------------------

            RowContent(
                left = MenuItemData(
                    label = recognizeRegister,
                    iconRes = appColors.recognizeIcon,

                    iconSizeSmall = 50.dp,
                    iconSizeBig = 50.dp,

                    fontSizeSmall = 20.sp,
                    fontSizeBig = 20.sp,

                    tintIcon = false
                ) {
                    onRecognizeClick()
                },

                right = MenuItemData(
                    label = addLocation,
                    iconRes = R.drawable.ic_location,

                    iconSizeSmall = 44.dp,
                    iconSizeBig = 44.dp,

                    fontSizeSmall = 20.sp,
                    fontSizeBig = 20.sp
                ) {
                    onAddLocationClick()
                }
            ),


            // ------------------------------------------------------
            // ROW 2
            // ------------------------------------------------------

            RowContent(
                left = MenuItemData(
                    label = help,
                    iconRes = R.drawable.help_icon,

                    iconSizeSmall = 41.dp,
                    iconSizeBig = 60.dp,

                    fontSizeSmall = 18.sp,
                    fontSizeBig = 23.sp
                ) {
                    onHelpClick()
                },

                right = MenuItemData(
                    label = search,
                    iconRes = R.drawable.ic_search,

                    iconSizeSmall = 18.dp,
                    iconSizeBig = 50.dp,

                    fontSizeSmall = 10.sp,
                    fontSizeBig = 18.sp
                ) {
                    onSearchClick()
                }
            ),


            // ------------------------------------------------------
            // ROW 3
            // ------------------------------------------------------

            RowContent(
                left = MenuItemData(
                    label = demo,
                    iconRes = R.drawable.ic_demo,

                    iconSizeSmall = 48.dp,
                    iconSizeBig = 48.dp,

                    fontSizeSmall = 18.sp,
                    fontSizeBig = 18.sp
                ) {
                    onDemoClick()
                },

                right = MenuItemData(
                    label = settings,
                    iconRes = R.drawable.ic_settings,

                    iconSizeSmall = 58.dp,
                    iconSizeBig = 58.dp,

                    fontSizeSmall = 22.sp,
                    fontSizeBig = 22.sp
                ) {
                    onSettingsClick()
                }
            )
        )
    }


    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        /*
         * BoxWithConstraints gives us the actual available
         * width/height of the screen.
         */
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * -----------------------------------------------------
             * RESPONSIVE DIMENSIONS
             * -----------------------------------------------------
             */

            val horizontalPadding =
                (maxWidth * 0.06f)
                    .coerceIn(20.dp, 32.dp)

            /*
             * Space between Header and greeting.
             */
            val headerToGreetingSpacing =
                (maxHeight * 0.012f)
                    .coerceIn(8.dp, 14.dp)

            /*
             * Greeting → menu grid.
             *
             * This is now the real visible gap. Before, the greeting's
             * offset(y = -20.dp) left 20.dp of hidden empty space, so the
             * visible gap was 42–54.dp.
             */
            val greetingToGridSpacing =
                (maxHeight * 0.025f)
                    .coerceIn(14.dp, 24.dp)

            /*
             * How far the greeting is pulled up towards the header
             * (same as the old offset).
             */
            val greetingLift = 20.dp


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = bottomNavHeight
                    )
            ) {

                /*
                 * -------------------------------------------------
                 * HEADER
                 * -------------------------------------------------
                 *
                 * Left unchanged.
                 */

                HeaderSection(
                    title = stringResource(
                        R.string.home
                    ),
                    centerButton = stringResource(
                        R.string.emergency_call
                    ),
                    onCenterButton = {
                        // emergency action
                    }
                )


                Spacer(
                    modifier = Modifier.height(
                        headerToGreetingSpacing
                    )
                )


                /*
                 * -------------------------------------------------
                 * GREETING
                 * -------------------------------------------------
                 */

                CompositionLocalProvider(
                    LocalLayoutDirection provides
                            if (isUrdu)
                                LayoutDirection.Rtl
                            else
                                LayoutDirection.Ltr
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = horizontalPadding
                            )
                            // Same position as the old offset(y = -20.dp), but
                            // the space it leaves below is removed too.
                            .liftUp(greetingLift)
                    ) {

                        Text(
                            text = stringResource(
                                R.string.hi_user
                            ),

                            fontFamily = OutfitFont,

                            /*
                             * Keep the greeting visually stable.
                             * Don't excessively scale text.
                             */
                            fontSize = 28.sp,

                            fontWeight = FontWeight.Bold,

                            color = TextDark,

                            modifier = Modifier.align(
                                Alignment.CenterStart
                            )
                        )
                    }
                }


                Spacer(
                    modifier = Modifier.height(
                        greetingToGridSpacing
                    )
                )


                /*
                 * -------------------------------------------------
                 * RESPONSIVE MENU GRID
                 * -------------------------------------------------
                 *
                 * The grid gets exactly the height left between the
                 * greeting and the bottom nav bar and shrinks to fit it.
                 * On very short screens it reaches its minimum size and
                 * this area scrolls, so the bottom buttons are never
                 * hidden under the nav bar.
                 */

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {

                    // Room for the cards' shadows inside the scroll area
                    val gridShadowPaddingTop = 4.dp
                    val gridShadowPaddingBottom = 12.dp

                    val gridAvailableHeight =
                        (maxHeight - gridShadowPaddingTop - gridShadowPaddingBottom)
                            .coerceAtLeast(0.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                top = gridShadowPaddingTop,
                                bottom = gridShadowPaddingBottom
                            )
                    ) {

                        RotatingRowGrid(
                            rowsData = rowsData,

                            startExpanded = startExpanded,

                            availableHeight = gridAvailableHeight,

                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = horizontalPadding
                                )
                        )
                    }
                }
            }
        }
    }
}


@Preview(name = "Home", showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    GreenTheme {
        HomeScreen(
            onSettingsClick = {}
        )
    }
}

@Preview(name = "Home - Expanded", showSystemUi = true)
@Composable
fun HomeScreenExpandedPreview() {
    GreenTheme {
        HomeScreen(
            onSettingsClick = {},
            startExpanded   = true
        )
    }
}

/*
 * Draws the element [amount] higher AND removes that much space below it,
 * so the content underneath moves up with it. (offset() only moves the
 * drawing and leaves an empty gap.)
 */
private fun Modifier.liftUp(amount: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val lift = amount.roundToPx().coerceAtMost(placeable.height)
        layout(placeable.width, placeable.height - lift) {
            placeable.place(0, -lift)
        }
    }
