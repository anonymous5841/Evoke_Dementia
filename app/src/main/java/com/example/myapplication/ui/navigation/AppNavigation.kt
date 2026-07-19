package com.example.myapplication.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.GreenTheme
import com.example.myapplication.ui.AddLocationContent
import com.example.myapplication.ui.CameraPreviewContent
import com.example.myapplication.ui.DemoScreen
import com.example.myapplication.ui.DetailedSummaryScreen
import com.example.myapplication.ui.FigmaMotionApp
import com.example.myapplication.ui.HelpScreen
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.LocationSearchContent
import com.example.myapplication.ui.PersonFormContent
import com.example.myapplication.ui.PersonFormMode
import com.example.myapplication.ui.NotRecognisedContent
import com.example.myapplication.ui.PersonListContent
import com.example.myapplication.ui.RecognisedContent
import com.example.myapplication.ui.RecordScreen
import com.example.myapplication.ui.SearchLocationScreen
import com.example.myapplication.ui.SearchResultsContent
import com.example.myapplication.ui.SelectLanguageContent
import com.example.myapplication.ui.SettingsContent
import com.example.myapplication.ui.ViewMoreScreen
import com.example.myapplication.ui.components.BottomNavBar
import com.example.myapplication.ui.components.DiscussionSummaryBox
import com.example.myapplication.ui.components.NavTab
import androidx.navigation.compose.dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.ui.components.PopupCard
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween



//newly one
@SuppressLint("SuspiciousIndentation")
@Composable
fun AppNavigation(
    isBlueTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val currentDestination = currentRoute?.destination?.route

    val selectedTab = when (currentDestination) {
        "home"    -> NavTab.HOME
        "profile" -> NavTab.PROFILE
        else      -> NavTab.NONE
    }

    val showNavBar = currentDestination != "recorder"
            && currentDestination != "splash"
            && currentDestination != "addperson"
            && currentDestination != "detailpopup"


    Box(Modifier.fillMaxSize()) {
            NavHost(
                navController    = navController,
                startDestination = "splash",
                modifier         = Modifier.fillMaxSize()
            ) {
                composable(
                    "splash",
                    exitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(900, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(900, easing = FastOutSlowInEasing))
                    }
                ) {
                    FigmaMotionApp(
                        onContinueClick = {
                            navController.navigate("addperson")
                        }
                    )
                }
                composable("addlocation") {
                    AddLocationContent(
                        onBack = { navController.popBackStack() },
                        onAddClick = { navController.navigate("add_success_popup") }
                    )
                }

                composable("notrecognised") { NotRecognisedContent(
                    onBack = { navController.popBackStack() },
                    onVoiceSampleClick = { navController.navigate("recorder") },
                    onSave = {navController.navigate("save_success_popup") }
                )
                }

                composable("profile") {
                    PersonFormContent(
                        mode = PersonFormMode.EDIT,
                        onVoiceSampleClick = { navController.navigate("recorder") },
                        onBack = { navController.popBackStack() },
                    )
                }

                dialog(
                    "detailpopup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    DetailedSummaryScreen(
                        onCloseClick = { navController.popBackStack() },
                    )
                }

                dialog(
                    "deletepopup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = "Delete Record?",
                        buttonText = "Delete",
                        showButton = true,
                        navController = navController,
                        onDismiss = { navController.popBackStack() },
                        onButtonClick = {
                            navController.popBackStack()
                            navController.popBackStack()
                            navController.navigate("delete_success_popup")

                        }
                    )
                }

                dialog(
                    "editpopup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = "Edit Record?",
                        buttonText = "Edit",
                        showButton = true,
                        navController = navController,
                        onDismiss = { navController.popBackStack() },
                        onButtonClick = {
                            navController.popBackStack()
                            navController.navigate("edit_success_popup")

                        }
                    )
                }

                dialog(
                    "delete_success_popup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = "Record Deleted Successfully!",
                        height = 0.25f,
                        upperPadding = 10.dp,
                        textOffset = DpOffset(x = (-36).dp, y = 312.dp),
                        shapeOffset = DpOffset(x = (-8).dp, y = 304.dp),
                        showButton = false,
                        navController = navController,
                        onDismiss = { navController.popBackStack() }
                    )
                }

                dialog(
                    "edit_success_popup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = "Record Edited Successfully!",
                        height = 0.25f,
                        upperPadding = 10.dp,
                        textOffset = DpOffset(x = (-36).dp, y = 312.dp),
                        shapeOffset = DpOffset(x = (-8).dp, y = 304.dp),
                        showButton = false,
                        navController = navController,
                        onDismiss = { navController.popBackStack() }
                    )
                }

                dialog(
                    "add_success_popup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = "Location Added Successfully!",
                        height = 0.25f,
                        upperPadding = 10.dp,
                        textOffset = DpOffset(x = (-36).dp, y = 312.dp),
                        shapeOffset = DpOffset(x = (-8).dp, y = 304.dp),
                        showButton = false,
                        navController = navController,
                        onDismiss = {
                            navController.popBackStack()
                            navController.popBackStack()
                        }
                    )
                }

                dialog(
                    "save_success_popup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = "Record Saved Successfully!",
                        height = 0.25f,
                        upperPadding = 10.dp,
                        textOffset = DpOffset(x = (-36).dp, y = 312.dp),
                        shapeOffset = DpOffset(x = (-8).dp, y = 304.dp),
                        showButton = false,
                        navController = navController,
                        onDismiss = {
                            navController.navigate("search")
                        }
                    )
                }

                dialog(
                    "confirmationpopup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = "Save recording?",
                        buttonText = "Save",
                        showButton = true,
                        navController = navController,
                        onDismiss = { navController.popBackStack() },
                        onButtonClick = {
                            navController.popBackStack()
                            navController.popBackStack()
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        onSettingsClick = {
                            navController.navigate("settings") { launchSingleTop = true }
                                          },

                        onSearchClick = {
                            navController.navigate("search")
                        },

                        onRecognizeClick = {
                            navController.navigate("camera")
                        },

                        onHelpClick = {
                            navController.navigate("help")
                        },

                        onDemoClick = {
                            navController.navigate("demo")
                        },

                        onAddLocationClick = {
                            navController.navigate("addlocation")
                        }
                    )
                }

                composable(
                    "addperson",
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(900, easing = FastOutSlowInEasing))
                    },
                    popExitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(900, easing = FastOutSlowInEasing))
                    }
                ) {
                    PersonFormContent(
                        mode = PersonFormMode.ADD,
                        onAdd = { _, _, _, _ ->
                            navController.navigate("home") {
                                launchSingleTop = true
                                popUpTo("splash") { inclusive = true }   // clears both splash + addperson
                            }
                        },
                        onBack = { navController.popBackStack() },        // ← pops back to splash, triggers slide-down
                        onVoiceSampleClick = { navController.navigate("recorder") }
                    )
                }
                composable("recognised") {
                    RecognisedContent(
                        onBack = {
                            navController.popBackStack()
                        },
                        onViewmore = {
                            navController.navigate("viewmore")
                        },
                        onVoiceSampleClick = { navController.navigate("recorder")
                        },
                        onSave = {navController.navigate("save_success_popup") }

                    )

                }
                composable("camera") {
                    CameraPreviewContent(
                        onBack = {
                            navController.popBackStack()
                        },
                        onCapture = {
                            navController.navigate("recognised")
                        },
                        onGallery = {
                            navController.navigate("notrecognised")
                        }
                    )
                }

                composable("search") {
                    PersonListContent(
                        onBack = {
                            navController.popBackStack()
                        },

//                        onViewMoreClick = {
//                            navController.navigate(Screen.ViewMore.route)
//                        },
                        onPersonClick = {
                            navController.navigate("searchresult")
                        },
//
                        onSearch = {
                            navController.navigate("locationQuery")
                        }
                    )
                }
                composable("searchresult") {
                    SearchResultsContent(
                        onBack = {
                            navController.popBackStack()
                        },
                        onViewmore = {
                            navController.navigate("viewmore")
                        },
                        onEdit = {
                            navController.navigate("editpopup")
                        },
                        onDelete = {
                            navController.navigate("deletepopup")
                        }
                    )
                }


                composable("locationQuery") {
                    SearchLocationScreen(
                        onBack = {
                            navController.popBackStack()
                        },
                        onEllipseClick = {
                            navController.navigate("locationSearch")
                        }
                    )
                }


                composable("viewmore") {
                    ViewMoreScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onEllipseClick = {
                            navController.navigate("detailpopup")
                        }
                    )
                }
                composable("locationSearch") {
                    LocationSearchContent(
                        onBack = {
                            navController.popBackStack()
                        },
                        onEdit = {
                            navController.navigate("editpopup")
                        },
                        onDelete = {
                            navController.navigate("deletepopup")
                        }
                    )
                }
                composable("help") {
                    HelpScreen(
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("settings") {
                    SettingsContent(
                        onBack = {
                            navController.popBackStack()
                        },
                        onSelectLanguage = {
                            navController.navigate("language")
                        },
                        isBlueTheme = isBlueTheme,
                        onThemeToggle = onThemeToggle
                    )
                }
                composable("language") {
                    SelectLanguageContent(
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("demo") {
                    DemoScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("recorder") {
                    RecordScreen(
                        onBack      = {
                            navController.navigate("confirmationpopup")
                             },
                        onDoneClick = {
                            navController.navigate("confirmationpopup")
                        }
                    )
                }
            }


            if (showNavBar) {
                BottomNavBar(
                    selectedTab    = selectedTab,
                    onHomeClick    = { navController.navigate("home") { launchSingleTop = true } },
                    onProfileClick = { navController.navigate("profile") { launchSingleTop = true } },
                    modifier       = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

}