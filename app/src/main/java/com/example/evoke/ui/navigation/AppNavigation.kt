package com.example.evoke.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.evoke.ui.AddLocationContent
import com.example.evoke.location.SharedLocationInbox
import com.example.evoke.transcription.ConversationSummaryState
import com.example.evoke.ui.CameraPreviewContent
import com.example.evoke.ui.CapturedScreen
import com.example.evoke.ui.DemoScreen
import com.example.evoke.ui.DetailedSummaryScreen
import com.example.evoke.ui.FigmaMotionApp
import com.example.evoke.ui.HelpScreen
import com.example.evoke.ui.HomeScreen
import com.example.evoke.ui.LocationSearchContent
import com.example.evoke.ui.PersonFormContent
import com.example.evoke.ui.PersonFormMode
import com.example.evoke.ui.NotRecognisedContent
import com.example.evoke.ui.RecognisedContent
import com.example.evoke.ui.RecordScreen
import com.example.evoke.ui.SearchResultsContent
import com.example.evoke.ui.SelectLanguageContent
import com.example.evoke.ui.SettingsContent
import com.example.evoke.ui.ViewMoreScreen
import com.example.evoke.ui.components.BottomNavBar
import com.example.evoke.ui.components.NavTab
import com.example.evoke.ui.CloudBackupScreen
import androidx.navigation.compose.dialog
import androidx.compose.ui.window.DialogProperties
import com.example.evoke.ui.components.PopupCard
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.stringResource
import com.example.evoke.ui.SearchScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evoke.R
import androidx.navigation.NavGraphBuilder
import com.example.evoke.utils.ProvideAppLocale
import androidx.compose.runtime.LaunchedEffect

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.repository.EmergencyContactRepository
import com.example.evoke.database.viewmodel.AppViewModelFactory
import com.example.evoke.database.viewmodel.EmergencyContactViewModel
import android.util.Log
import com.example.evoke.mobilefacenet.FaceConfig
import com.example.evoke.mobilefacenet.FaceMatcher
import com.example.evoke.mobilefacenet.FaceRecognitionViewModel
import com.example.evoke.mobilefacenet.logFullEmbedding
import com.example.evoke.voice.RecorderMode
import com.example.evoke.voice.RecorderPhase
import com.example.evoke.voice.VoiceSessionViewModel
import com.example.evoke.registration.RegisterPersonViewModel
import com.example.evoke.location.LocationHelper
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


import androidx.compose.runtime.rememberCoroutineScope
import com.example.evoke.nllb.NLLBTranslator
import com.example.evoke.tts.AudioPlayer
import com.example.evoke.tts.TtsApi
import com.example.evoke.tts.TtsLanguage
import com.example.evoke.ui.components.isUrduSelected
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@SuppressLint("SuspiciousIndentation")
@Composable
fun AppNavigation(
    isBlueTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onSplashScreenChanged: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val faceVm: FaceRecognitionViewModel = viewModel()
    // [VOICE] recorder screen state + voice pipeline (patient / visitor samples, conversations)
    val voiceVm: VoiceSessionViewModel = viewModel()
    // [REGISTER] Not-recognised screen: saves the new person (face, voice, location, conversation)
    val registerVm: RegisterPersonViewModel = viewModel()
    val currentRoute by navController.currentBackStackEntryAsState()
    val currentDestination = currentRoute?.destination?.route

    /*
     * [ENGLISH SUMMARY VARIABLE]
     * Final English summary of the last recorded conversation:
     * filtered audio -> Groq transcription -> Groq summary (transcription/ConversationSummaryState).
     * null until a conversation has been filtered and summarised.
     * TODO: pass it to the summary screens / save it in the database.
     */
    val englishSummary by ConversationSummaryState.englishSummary.collectAsState()
    LaunchedEffect(englishSummary) {
        englishSummary?.let { android.util.Log.i("AppNavigation", "englishSummary = $it") }
    }

    // ── [SUMMARY] language + NLLB translation + Piper speech ─────────────────
    val appContext = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val isUrdu = isUrduSelected()          // follows the app language, replaces summaryLanguage

// TEMP fallback so Play / Translate work before a real conversation exists. REMOVE.
    val summaryText = englishSummary ?: "Daniel discussed the upcoming family gathering with his " +
    "uncle. He agreed to bring groceries for twelve people and added a shopping list " +
    "and a reminder to his phone."

// Urdu cache; resets when the English summary changes.
    var urduSummary by remember(summaryText) { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }

    val translationMutex = remember { Mutex() }
    val ttsApi = remember { TtsApi(appContext) }
    val audioPlayer = remember { AudioPlayer() }

    suspend fun ensureUrduSummary(): String =
        translationMutex.withLock {
            if (urduSummary.isNotBlank()) {
                urduSummary
            } else {
                val source = summaryText
                val translated = withContext(Dispatchers.IO) {
                    val translator = NLLBTranslator(appContext)
                    try {
                        translator.translate(source)
                    } finally {
                        translator.close()
                    }
                }
                check(translated.isNotBlank()) { "NLLB returned an empty Urdu translation." }
                urduSummary = translated
                translated
            }
        }

    val translateSummaryToUrdu: () -> Unit = {
        if (urduSummary.isBlank() && !isTranslating) {
            isTranslating = true
            translationError = null
            scope.launch {
                try {
                    ensureUrduSummary()
                } catch (e: CancellationException) {
                    throw e
                } catch (t: Throwable) {          // includes UnsatisfiedLinkError / OOM
                    Log.e("AppNavigation", "Translation failed", t)
                    translationError = t.localizedMessage ?: "Translation failed."
                } finally {
                    isTranslating = false
                }
            }
        }
    }

    val speakSummary: () -> Unit = {
        scope.launch {
            try {
                val text = if (isUrdu) ensureUrduSummary() else summaryText
                audioPlayer.speak(
                    ttsApi = ttsApi,
                    text = text,
                    language = if (isUrdu) TtsLanguage.URDU else TtsLanguage.ENGLISH
                )
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e("AppNavigation", "Speech failed", t)
                Toast.makeText(appContext, t.localizedMessage ?: "Speech playback failed.",
                    Toast.LENGTH_LONG).show()
                audioPlayer.onPlaybackCompleted?.invoke()   // resets the play button
            }
        }
    }

    LaunchedEffect(currentDestination) {
        onSplashScreenChanged(currentDestination == "splash")
    }

    // [SHARED LOCATION] a place shared from Google Maps while on Home -> open Add Location,
    // which reads it (normally the user is already on Add Location after "Customise").
    val pendingSharedLocation by SharedLocationInbox.pending.collectAsState()
    LaunchedEffect(pendingSharedLocation, currentDestination) {
        if (pendingSharedLocation != null && currentDestination == "home") {
            navController.navigate("addlocation") { launchSingleTop = true }
        }
    }
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
                    onAddClick = { navController.openPopup("add_success_popup") }
                )
            }

            composable("notrecognised") {
                val context = LocalContext.current
                val registerState by registerVm.state.collectAsState()
                val pendingPerson by voiceVm.pendingInfo.collectAsState()

                // [REGISTER] messages (missing name, need location or conversation, saved failed...)
                LaunchedEffect(registerState.messageRes) {
                    val res = registerState.messageRes ?: return@LaunchedEffect
                    val arg = registerState.messageArg
                    val text = if (arg != null) context.getString(res, arg) else context.getString(res)
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                    registerVm.messageShown()
                }

                // [REGISTER] location field -> current location (asks for permission first)
                val locationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    if (results.values.any { it }) {
                        registerVm.fetchCurrentLocation()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.addloc_permission_needed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                NotRecognisedContent(
                    imageBitmap = faceVm.profileBitmap,
                    onBack = {
                        faceVm.clear()
                        voiceVm.clearPendingPerson()
                        registerVm.reset()
                        navController.popBackStack()
                    },
                    locationText = registerState.locationText,
                    onLocationClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            registerVm.fetchCurrentLocation()
                        } else {
                            locationPermission.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    voiceSamplesRecorded = pendingPerson.voiceSamples,
                    conversationRecorded = pendingPerson.hasConversation,
                    isSaving = registerState.isSaving,
                    // [VOICE] visitor's voice sample (script shown); kept until the visitor is saved
                    onVoiceSampleClick = {
                        voiceVm.prepare(RecorderMode.PARTNER_SAMPLE)
                        navController.navigate("recorder")
                    },
                    // [VOICE] patient + visitor conversation (no script)
                    onRecordConversationClick = {
                        voiceVm.prepare(RecorderMode.CONVERSATION)
                        navController.navigate("recorder")
                    },
                    // [REGISTER] new person -> persons, face_embeddings, voice_embeddings,
                    // recognition (+ location / queued conversation); face added to the face store
                    onSave = { name, relation ->
                        registerVm.register(
                            name = name,
                            relation = relation,
                            face = faceVm.lastResult,
                            faceStore = faceVm.store,
                            voiceSession = voiceVm
                        ) {
                            navController.openPopup("save_success_popup")
                        }
                    }
                )
            }

            composable("profile") {
                val emergencyContactViewModel = rememberEmergencyContactViewModel()

                // Pre-fill the 5 emergency contact fields with what is saved.
                // The form copies its initial values only once, so it is keyed
                // on the loaded list (null -> list happens exactly once).
                val context = LocalContext.current
                var savedEmergencyContacts by remember { mutableStateOf<List<String>?>(null) }
                LaunchedEffect(Unit) {
                    val saved = EmergencyContactRepository(
                        AppDatabase.getDatabase(context.applicationContext)
                    ).getContacts().map { it.phoneNo }
                    savedEmergencyContacts = List(5) { saved.getOrNull(it) ?: "" }
                }

                key(savedEmergencyContacts) {
                    PersonFormContent(
                        mode = PersonFormMode.EDIT,
                        initialEmergencyContacts = savedEmergencyContacts ?: List(5) { "" },
                        onEdit = { _, _, emergencyContacts, _ ->
                            // Emergency contacts used by Home > Emergency Call
                            // and Help > Send Message.
                            emergencyContactViewModel.replaceContacts(emergencyContacts) {
                                navController.openPopup("edit_success_popup")
                            }
                        },
                        // [VOICE] patient's voice sample (script shown)
                        onVoiceSampleClick = {
                            voiceVm.prepare(RecorderMode.PATIENT_SAMPLE)
                            navController.navigate("recorder")
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            localizedDialog("detailpopup") {
                DetailedSummaryScreen(
                    onCloseClick = { navController.popBackStack() },
                    summaryText = if (isUrdu) urduSummary else summaryText,
                    isTranslating = isUrdu && isTranslating,
                    errorText = if (isUrdu) translationError else null,
                    onRetry = translateSummaryToUrdu
                )
            }

            localizedDialog("deletepopup") {
                PopupCard(
                    messageText = deleteRecord,
                    buttonText = deleteText,
                    showButton = true,
                    navController = navController,
                    onDismiss = { navController.popBackStack() },
                    onButtonClick = {
                        navController.popBackStack()
                        navController.popBackStack()
                        navController.openPopup("delete_success_popup")

                    }
                )
            }
            localizedDialog("delete_viewmore_popup") {
                PopupCard(
                    messageText = deleteRecord,
                    buttonText = deleteText,
                    showButton = true,
                    navController = navController,
                    onDismiss = { navController.popBackStack() },
                    onButtonClick = {
                        navController.popBackStack()
                        navController.openPopup("delete_success_popup")

                    }
                )
            }

            localizedDialog("save_success_popup") {
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

            localizedDialog("editpopup") {
                PopupCard(
                    messageText = editRecord,
                    buttonText = editText,
                    showButton = true,
                    navController = navController,
                    onDismiss = { navController.popBackStack() },
                    onButtonClick = {
                        navController.popBackStack()
                        navController.openPopup("edit_success_popup")

                    }
                )
            }

            localizedDialog("delete_success_popup") {
                PopupCard(
                    messageText = deleteSuccess,
                    height = 0.25f,
                    upperPadding = 10.dp,
                    showButton = false,
                    navController = navController,
                    onDismiss = { navController.popBackStack() }
                )
            }

            localizedDialog("edit_success_popup") {
                PopupCard(
                    messageText = editSuccess,
                    height = 0.25f,
                    upperPadding = 10.dp,
                    showButton = false,
                    navController = navController,
                    onDismiss = { navController.popBackStack() }
                )
            }

            localizedDialog("add_success_popup") {
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


            localizedDialog("confirmationpopup") {
                PopupCard(
                    messageText = saveRecording,
                    buttonText = saveText,
                    showButton = true,
                    navController = navController,
                    onDismiss = { navController.popBackStack() },
                    onButtonClick = {
                        navController.popBackStack()      // close the popup only
                        voiceVm.saveRecording()           // recorder shows progress + result
                    }
                )
            }
            localizedDialog("record_back_popup") {
                PopupCard(
                    messageText = stringResource(R.string.dialog_record_back_title),
                    buttonText = stringResource(R.string.back),
                    showButton = true,
                    navController = navController,
                    onDismiss = { navController.popBackStack() },
                    onButtonClick = {
                        voiceVm.discardRecording()
                        navController.popBackStack()
                        navController.popBackStack()
                    }
                )
            }

            localizedDialog("delete_data_popup") {
                PopupCard(
                    messageText = stringResource(R.string.dialog_delete_data_title),
                    buttonText = stringResource(R.string.dialog_delete_data_button),
                    showButton = true,
                    navController = navController,
                    onDismiss = { navController.popBackStack() },
                    // [ERASE DATA BUTTON] was an empty {} — the button did nothing.
                    // "Erase Data & Restart?" → clear the whole back stack and
                    // start again from the splash screen.
                    onButtonClick = {
                        navController.navigate("splash") {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
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
                val emergencyContactViewModel = rememberEmergencyContactViewModel()

                PersonFormContent(
                    mode = PersonFormMode.ADD,
                    onAdd = { _, _, emergencyContacts, _ ->
                        // Save the emergency contacts (used by Home > Emergency
                        // Call and Help > Send Message), then go Home.
                        emergencyContactViewModel.replaceContacts(emergencyContacts) {
                            navController.navigate("home") {
                                launchSingleTop = true
                                popUpTo("splash") { inclusive = true }   // clears both splash + addperson
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },        // ← pops back to splash, triggers slide-down
                    // [VOICE] patient's voice sample (script shown)
                    onVoiceSampleClick = {
                        voiceVm.prepare(RecorderMode.PATIENT_SAMPLE)
                        navController.navigate("recorder")
                    }
                )
            }
            composable("recognised") {
                RecognisedContent(
                    imageBitmap = faceVm.profileBitmap,
                    onBack = {
                        faceVm.clear()
                        navController.popBackStack()
                    },
                    onViewmore = {
                        navController.navigate("viewmore")
                    },
                    // [VOICE] patient + recognised person conversation (no script).
                    // The match id is a Room personId only once faces are enrolled
                    // with it; otherwise the recorder explains that no voice sample exists.
                    onRecordConversationClick = {
                        voiceVm.prepare(
                            RecorderMode.CONVERSATION,
                            personId = faceVm.matchResult?.personId?.toLongOrNull()
                        )
                        navController.navigate("recorder")
                    },
                    onSave = {navController.openPopup("save_success_popup") }

                )

            }

            composable("camera") {
                val store = faceVm.store
                var capturedImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

                BackHandler(enabled = capturedImageUri != null) { capturedImageUri = null }

                if (capturedImageUri != null) {
                    CapturedScreen(
                        imageUri = capturedImageUri,
                        onBack = { capturedImageUri = null },
                        onProcessingComplete = { result ->
                            voiceVm.clearPendingPerson()
                            registerVm.reset()
                            faceVm.lastResult = result
                            logFullEmbedding(result)

                            // TEMP: hardcoded test people while nobody is enrolled. REMOVE BEFORE RELEASE.
                            val recognition = FaceMatcher.identify(
                                queryEmbedding = result.embedding,
                                references = store.loadReferences().ifEmpty { FaceMatcher.testReferences },
                                threshold = FaceConfig.RECOGNITION_THRESHOLD
                            )
                            faceVm.matchResult = recognition

                            Log.d(
                                "FaceRecognition",
                                """
                    Recognized: ${recognition.isRecognized}
                    Person: ${recognition.personId}
                    Similarity: ${recognition.similarity}
                    Threshold: ${recognition.threshold}
                    """.trimIndent()
                            )

                            navController.navigate(
                                if (recognition.isRecognized) "recognised" else "notrecognised"
                            )
                        }
                    )
                } else {
                    CameraPreviewContent(
                        onBack = { navController.popBackStack() },
                        onImageReady = { uri -> capturedImageUri = uri }
                    )
                }
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
                        navController.openPopup("editpopup")
                    },
                    onDelete = {
                        navController.openPopup("deletepopup")
                    }
                )
            }

            composable("viewmore") {
                ViewMoreScreen(
                    onBackClick = { navController.popBackStack() },
                    onEllipseClick = {
                        if (isUrdu) translateSummaryToUrdu()
                        navController.openPopup("detailpopup")
                    },
                    onDeleteClick = { navController.openPopup("delete_viewmore_popup") },
                    onPlayRecording = speakSummary,
                    onPauseRecording = { audioPlayer.pause() },
                    onResumeRecording = speakSummary,      // also replays after playback finished
                    onRegisterPlaybackCompleted = { callback -> audioPlayer.onPlaybackCompleted = callback },
                    onSpeedChange = { speed -> audioPlayer.setPlaybackSpeed(speed) }
                )
            }


            composable("locationSearch") {
                LocationSearchContent(
                    onBack = {
                        navController.popBackStack()
                    },
                    onEdit = {
                        navController.openPopup("editpopup")
                    },
                    onDelete = {
                        navController.openPopup("deletepopup")
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

            composable("CloudBackup") {
                CloudBackupScreen(
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
                        navController.openPopup("delete_data_popup")
                    },
                    isBlueTheme = isBlueTheme,
                    onThemeToggle = onThemeToggle,
                    onCloudBackup = {
                        navController.navigate("CloudBackup")                    }
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
                val recorderState by voiceVm.state.collectAsState()
                val elapsedMs by voiceVm.recorder.elapsedMs.collectAsState()

                RecordScreen(
                    state = recorderState,
                    elapsedMs = elapsedMs,
                    onBack = {
                        when (recorderState.phase) {
                            // a recording exists: ask "Don't save recording?" first
                            RecorderPhase.RECORDING, RecorderPhase.STOPPED ->
                                navController.openPopup("record_back_popup")
                            // processing keeps running in the ViewModel
                            RecorderPhase.PROCESSING -> navController.popBackStack()
                            else -> {
                                voiceVm.discardRecording()
                                navController.popBackStack()
                            }
                        }
                    },
                    onStartClick = { voiceVm.startRecording() },
                    onDoneClick = {
                        voiceVm.stopRecording()
                        navController.openPopup("confirmationpopup")
                    },
                    onSaveClick = { voiceVm.saveRecording() },
                    onRetryClick = { voiceVm.retry() },
                    onNextSampleClick = { voiceVm.nextSample() },
                    onKeepAnywayClick = { voiceVm.keepSampleAnyway() },
                    onFinishClick = { navController.popBackStack() }
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


/*
 * EmergencyContactViewModel for the current screen, backed by the
 * encrypted Room database. Any save error is shown as a Toast.
 */
@Composable
private fun rememberEmergencyContactViewModel(): EmergencyContactViewModel {
    val context = LocalContext.current
    val factory = remember {
        AppViewModelFactory(AppDatabase.getDatabase(context.applicationContext),
            context = context.applicationContext)
    }
    val emergencyContactViewModel: EmergencyContactViewModel = viewModel(factory = factory)

    val error by emergencyContactViewModel.error.collectAsState()
    LaunchedEffect(error) {
        val message = error ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        emergencyContactViewModel.clearError()
    }

    return emergencyContactViewModel
}


/*
 * [POPUP NAVIGATION] Opens a popup route only once.
 * launchSingleTop stops a double tap from stacking a second, identical
 * popup underneath. That duplicate was why X / the button seemed to do
 * nothing — they closed the top copy and revealed its twin.
 */
private fun NavController.openPopup(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}


/**
 * A popup route — same as
 *   dialog(route, dialogProperties = DialogProperties(usePlatformDefaultWidth = false))
 * — but its content always uses the app's CURRENT language.
 *
 * Why: a dialog opens in its own window, and that window uses the Activity's
 * original context, i.e. the language the app was STARTED with. Without
 * ProvideAppLocale, popups stayed in the old language after switching
 * (see utils/ProvideAppLocale.kt).
 */
private fun NavGraphBuilder.localizedDialog(
    route: String,
    content: @Composable () -> Unit
) {
    dialog(
        route,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ProvideAppLocale {
            content()
        }
    }
}
