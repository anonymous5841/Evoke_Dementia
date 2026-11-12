package com.example.evoke.ui
import android.annotation.SuppressLint
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evoke.ui.components.HeaderSection
import com.example.evoke.ui.theme.GreenTheme
import com.example.evoke.ui.theme.OutfitFont
import com.example.evoke.ui.components.MenuItemData
import com.example.evoke.ui.components.RowContent
import com.example.evoke.ui.components.RotatingRowGrid
import com.example.evoke.ui.components.TextDark
import com.example.evoke.ui.components.rememberBottomNavBarHeight
import com.example.evoke.ui.theme.AppTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import com.example.evoke.utils.LanguageManager
import com.example.evoke.utils.LocalAppLanguage
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import com.example.evoke.R
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.evoke.data.EmergencyContactState
import com.example.evoke.services.EmergencyAlertService
import kotlinx.coroutines.launch

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
     * EMERGENCY CALL
     * -------------------------------------------------------------
     *
     * Ported from the backend HomeScreen. The header's
     * "Emergency Call" button drives EmergencyAlertService.
     */

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // CALL_PHONE, READ_CALL_LOG, and READ_PHONE_STATE are all "dangerous"
    // permissions — being in the manifest isn't enough, the user has to grant
    // them at runtime or EmergencyAlertService silently no-ops (its permission
    // guards just set lastEvent and return, they don't crash or retry).
    val emergencyPermissions = remember {
        arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    fun hasEmergencyPermissions(): Boolean = emergencyPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    var showEmergencyChoiceDialog by remember { mutableStateOf(false) }

    fun triggerEmergencyAlert(action: String = EmergencyAlertService.ACTION_TRIGGER) {
        val intent = Intent(context, EmergencyAlertService::class.java).apply {
            this.action = action
        }
        ContextCompat.startForegroundService(context, intent)
    }

    // Reads the saved state before deciding what a press should do: if the
    // active contact already answered (and we're not past the 30-min reset
    // window), show the "call again / next contact" dialog instead of
    // re-running the normal attempt flow.
    fun checkStateThenAct() {
        coroutineScope.launch {
            val state = EmergencyContactState.read(context)
            val now = System.currentTimeMillis()
            val stale = state.lastCallAt == 0L || now - state.lastCallAt > EmergencyContactState.RESET_AFTER_MS
            if (state.answered && !stale) {
                showEmergencyChoiceDialog = true
            } else {
                triggerEmergencyAlert(EmergencyAlertService.ACTION_TRIGGER)
            }
        }
    }

    val emergencyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            checkStateThenAct()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.emergency_permissions_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Surfaces what actually happened inside the service — without this,
    // permission failures, "no answer, escalating", etc. are invisible.
    val emergencyEvent by EmergencyAlertService.lastEvent.collectAsState()
    LaunchedEffect(emergencyEvent) {
        val event = emergencyEvent ?: return@LaunchedEffect
        val message = when {
            event.startsWith("sms_sent:") -> context.getString(R.string.emergency_event_sms_sent, event.removePrefix("sms_sent:"))
            event.startsWith("sms_failed:") -> context.getString(R.string.emergency_event_sms_failed, event.removePrefix("sms_failed:"))
            event.startsWith("call_failed:") -> context.getString(R.string.emergency_event_call_failed, event.removePrefix("call_failed:"))
            event.startsWith("invalid_contact_number:") -> context.getString(R.string.emergency_event_invalid_number, event.removePrefix("invalid_contact_number:"))
            event.startsWith("answered:") -> context.getString(R.string.emergency_event_answered, event.removePrefix("answered:"))
            event.startsWith("no_answer_retry_same_contact:") -> context.getString(R.string.emergency_event_no_answer_retry, event.removePrefix("no_answer_retry_same_contact:"))
            event.startsWith("no_answer_moving_to_next:") -> context.getString(R.string.emergency_event_no_answer_next, event.removePrefix("no_answer_moving_to_next:"))
            event == "moved_to_next_contact" -> context.getString(R.string.emergency_event_moved_to_next)
            event == "all_contacts_exhausted" -> context.getString(R.string.emergency_event_all_exhausted)
            event == "no_contacts_configured" -> context.getString(R.string.emergency_event_no_contacts)
            event.startsWith("internal_error:") -> context.getString(R.string.emergency_event_internal_error, event.removePrefix("internal_error:"))
            event == "already_in_progress" -> context.getString(R.string.emergency_event_in_progress)
            event == "no_call_permission" -> context.getString(R.string.emergency_event_no_call_permission)
            event == "no_sms_permission" -> context.getString(R.string.emergency_event_no_sms_permission)
            event == "no_call_log_permission_assuming_no_answer" -> context.getString(R.string.emergency_event_no_call_log_permission)
            event == "no_phone_state_permission_assuming_no_answer" -> context.getString(R.string.emergency_event_no_phone_state_permission)
            else -> null
        }
        message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    // Resolved here, not inside the dialog: a dialog opens in its own window
    // whose context still has the language the app was started with
    // (see utils/ProvideAppLocale.kt).
    val answeredTitle = stringResource(R.string.emergency_answered_title)
    val answeredMessage = stringResource(R.string.emergency_answered_message)
    val callAgainText = stringResource(R.string.emergency_call_again)
    val nextContactText = stringResource(R.string.emergency_next_contact)


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
                        // Checks saved state first — if the active contact
                        // already answered, this shows the choice dialog instead
                        // of re-running the normal attempt flow.
                        if (hasEmergencyPermissions()) {
                            checkStateThenAct()
                        } else {
                            emergencyPermissionLauncher.launch(emergencyPermissions)
                        }
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

    if (showEmergencyChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyChoiceDialog = false },
            containerColor = appColors.background,
            titleContentColor = appColors.pagesText,
            textContentColor = appColors.pagesText,
            title = {
                Text(
                    text = answeredTitle,
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = answeredMessage,
                    fontFamily = OutfitFont,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmergencyChoiceDialog = false
                        triggerEmergencyAlert(EmergencyAlertService.ACTION_CALL_EXISTING)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = appColors.pagesText
                    )
                ) {
                    Text(
                        text = callAgainText,
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEmergencyChoiceDialog = false
                        triggerEmergencyAlert(EmergencyAlertService.ACTION_MOVE_TO_NEXT)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = appColors.pagesText
                    )
                ) {
                    Text(
                        text = nextContactText,
                        fontFamily = OutfitFont
                    )
                }
            }
        )
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
