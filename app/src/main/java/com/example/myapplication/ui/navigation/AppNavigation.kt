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
import com.example.myapplication.ui.RecognisedContent
import com.example.myapplication.ui.RecordScreen
import com.example.myapplication.ui.SearchResultsContent
import com.example.myapplication.ui.SelectLanguageContent
import com.example.myapplication.ui.SettingsContent
import com.example.myapplication.ui.ViewMoreScreen
import com.example.myapplication.ui.components.BottomNavBar
import com.example.myapplication.ui.components.NavTab
import androidx.navigation.compose.dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.ui.components.PopupCard
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.stringResource
import com.example.myapplication.ui.SearchScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
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

    val deleteRecord = stringResource(R.string.delete_record)
    val editRecord = stringResource(R.string.edit_record)
    val deleteSuccess = stringResource(R.string.record_deleted_successfully)
    val editSuccess = stringResource(R.string.record_edited_successfully)
    val locationSuccess = stringResource(R.string.location_added_successfully)
    val recordSaved = stringResource(R.string.record_saved_successfully)
    val saveRecording = stringResource(R.string.save_recording)

    val deleteText = stringResource(R.string.delete_popup)
    val editText = stringResource(R.string.edit_popup)
    val saveText = stringResource(R.string.save_popup)

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
                        messageText = deleteRecord,
                        buttonText = deleteText,
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
                    "delete_viewmore_popup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = deleteRecord,
                        buttonText = deleteText,
                        showButton = true,
                        navController = navController,
                        onDismiss = { navController.popBackStack() },
                        onButtonClick = {
                            navController.popBackStack()
                            navController.navigate("delete_success_popup")

                        }
                    )
                }

                dialog(
                    "save_success_popup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = recordSaved,
                        height = 0.25f,
                        upperPadding = 10.dp,
                        showButton = false,
                        navController = navController,
                        onDismiss = {
                            navController.popBackStack()
                            navController.popBackStack()                        }
                    )
                }

                dialog(
                    "editpopup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = editRecord,
                        buttonText = editText,
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
                        messageText = deleteSuccess,
                        height = 0.25f,
                        upperPadding = 10.dp,
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
                        messageText = editSuccess,
                        height = 0.25f,
                        upperPadding = 10.dp,
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
                        messageText = locationSuccess,
                        height = 0.25f,
                        upperPadding = 10.dp,
                        showButton = false,
                        navController = navController,
                        onDismiss = {
                            navController.popBackStack()
                            navController.popBackStack()
                        }
                    )
                }


                dialog(
                    "confirmationpopup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = saveRecording,
                        buttonText = saveText,
                        showButton = true,
                        navController = navController,
                        onDismiss = { navController.popBackStack() },
                        onButtonClick = {
                            navController.popBackStack()
                            navController.popBackStack()
                        }
                    )
                }
                dialog(
                    "record_back_popup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = stringResource(R.string.dialog_record_back_title),
                        buttonText = stringResource(R.string.back),
                        showButton = true,
                        navController = navController,
                        onDismiss = { navController.popBackStack() },
                        onButtonClick = {
                            navController.popBackStack()
                            navController.popBackStack()
                        }
                    )
                }

                dialog(
                    "delete_data_popup",
                    dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    PopupCard(
                        messageText = stringResource(R.string.dialog_delete_data_title),
                        buttonText = stringResource(R.string.dialog_delete_data_button),
                        showButton = true,
                        navController = navController,
                        onDismiss = { navController.popBackStack() },
                        onButtonClick = {}
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

                composable("search") { backStackEntry ->
                    val searchViewModel: SearchViewModel = viewModel(backStackEntry)
                    SearchScreen(
                        viewModel = searchViewModel,
                        onBack = { navController.popBackStack() },
                        onPersonClick = { navController.navigate("searchresult") },
                        onMoreInfoLocationClick = { navController.navigate("locationSearch") }
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

                composable("viewmore") {
                    ViewMoreScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onEllipseClick = {
                            navController.navigate("detailpopup")
                        },
                        onDeleteClick = {
                            navController.navigate("delete_viewmore_popup")
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
                        onEraseClick = {
                            navController.navigate("delete_data_popup")
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
                            navController.navigate("record_back_popup")
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