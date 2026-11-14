package com.example.evoke.ui
import android.content.ActivityNotFoundException
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evoke.ui.components.FitText
import com.example.evoke.ui.components.HeaderSection
import com.example.evoke.ui.theme.AppTheme
import com.example.evoke.ui.theme.GreenTheme
import com.example.evoke.ui.theme.MartelFont
import com.example.evoke.ui.theme.OutfitFont
import com.example.evoke.R
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.evoke.services.LocationShareService


@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun HelpScreen(
    onBack: () -> Unit = {},
    onMapToHome: () -> Unit = {}
) {

    val appColors = AppTheme.colors


    /*
     * -------------------------------------------------------------
     * SEND MESSAGE (live location sharing by SMS)
     * -------------------------------------------------------------
     *
     * Ported from the backend HelpScreen. The "Send Message" card
     * starts / stops LocationShareService.
     */

    val context = LocalContext.current

    // Reflects the service's actual state, so the button stays correct even
    // if the screen was recreated while sharing was already running.
    val isSharing by LocationShareService.isRunning.collectAsState()

    // Contacts come from the database (SQLite), not from user input here —
    // see ContactRepository. SEND_SMS + ACCESS_FINE_LOCATION + RECEIVE_SMS are
    // required for the core feature. READ_PHONE_STATE is optional — it lets the
    // service try every active SIM (not just the system default) when sending,
    // which matters if e.g. the default SIM has no balance. Declining it just
    // means multi-SIM fallback is unavailable; it never blocks starting.
    val coreRequiredPermissions = remember {
        arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECEIVE_SMS
        )
    }
    val allRequestedPermissions = remember {
        coreRequiredPermissions + Manifest.permission.READ_PHONE_STATE
    }

    fun hasCoreRequiredPermissions(): Boolean = coreRequiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationServiceEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    fun startSharing() {
        if (!isLocationServiceEnabled()) {
            Toast.makeText(
                context,
                context.getString(R.string.sharing_location_off),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        ContextCompat.startForegroundService(context, Intent(context, LocationShareService::class.java))
        Toast.makeText(context, context.getString(R.string.sharing_starting), Toast.LENGTH_SHORT).show()
    }

    fun stopSharing() {
        val stopIntent = Intent(context, LocationShareService::class.java).apply {
            action = LocationShareService.ACTION_STOP
        }
        context.startService(stopIntent)
        Toast.makeText(context, context.getString(R.string.sharing_stopped), Toast.LENGTH_SHORT).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val coreGranted = coreRequiredPermissions.all { results[it] == true }
        if (coreGranted) {
            startSharing()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.sharing_permissions_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val lastEvent by LocationShareService.lastEvent.collectAsState()
    LaunchedEffect(lastEvent) {
        val event = lastEvent ?: return@LaunchedEffect
        val message = when {
            event.startsWith("sent:") -> {
                val parts = event.removePrefix("sent:").split(":")
                context.getString(
                    R.string.sharing_event_sent,
                    parts.getOrNull(1) ?: context.getString(R.string.sharing_event_contact_fallback),
                    parts.getOrNull(0)
                )
            }
            event.startsWith("escalated_send_failed:") ->
                context.getString(R.string.sharing_event_escalated_send_failed, event.removePrefix("escalated_send_failed:"))
            event.startsWith("no_reply_escalating:") ->
                context.getString(R.string.sharing_event_no_reply_escalating, event.removePrefix("no_reply_escalating:"))
            event.startsWith("reply_received:") ->
                context.getString(R.string.sharing_event_reply_received, event.removePrefix("reply_received:"))
            event == "no_contact" -> context.getString(R.string.sharing_event_no_contact)
            event == "no_location_permission" -> context.getString(R.string.sharing_event_no_location_permission)
            event == "no_sms_permission" -> context.getString(R.string.sharing_event_no_sms_permission)
            event == "no_telephony_hardware" -> context.getString(R.string.sharing_no_telephony)
            event == "no_sim" -> context.getString(R.string.sharing_event_no_sim)
            event == "max_messages_reached" -> context.getString(R.string.sharing_event_max_messages)
            event == "waiting_for_gps" -> context.getString(R.string.sharing_event_waiting_for_gps)
            event == "all_contacts_exhausted" -> context.getString(R.string.sharing_event_all_exhausted)
            event == "location_unavailable_for_escalation" -> context.getString(R.string.sharing_event_location_unavailable)
            else -> null
        }
        message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    fun onSendMessageClicked() {
        if (isSharing) {
            stopSharing()
            return
        }
        val hasTelephony = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        if (!hasTelephony) {
            Toast.makeText(
                context,
                context.getString(R.string.sharing_no_telephony),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (hasCoreRequiredPermissions()) {
            startSharing()
        } else {
            permissionLauncher.launch(allRequestedPermissions)
        }
    }
    //val context = LocalContext.current

    /*
     * [HOME LOCATION - TEMP VARIABLE]
     * TODO: replace with the home location fetched from the Database
     * once the Database is connected.
     */
    val homeLatitude: Double = 31.5204
    val homeLongitude: Double = 74.3587

    /*
     * [OPEN GOOGLE MAPS NAVIGATION]
     * Opens Google Maps turn-by-turn navigation (walking) to home.
     * If the Google Maps app isn't installed, opens Google Maps
     * directions in the browser instead.
     */
    fun openGoogleMapsNavigation(latitude: Double, longitude: Double) {
        Log.d("MapToHome", "Map to Home clicked -> $latitude,$longitude")

        val navigationIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=$latitude,$longitude&mode=w")
        ).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(navigationIntent)
        } catch (e: ActivityNotFoundException) {
            Log.w("MapToHome", "Google Maps app not found, opening browser", e)
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=walking")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(browserIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(context, "No app found to open the map", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            // [MAP TO HOME ERROR]
            Log.e("MapToHome", "Couldn't open Google Maps", e)
            Toast.makeText(context, "Couldn't open Google Maps: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

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
             * ORIGINAL SPACING
             * ----------------
             * These values are kept the same
             * as your original HelpScreen.
             */

            val headerHeight = 260.dp

            val horizontalPadding = 22.dp

            val cardSpacing = 20.dp

            val verticalSpacing = 24.dp


            /*
             * RESPONSIVE CARD WIDTH
             * --------------------
             *
             * Available width:
             *
             * screen width
             * - left padding
             * - right padding
             * - space between cards
             *
             * The remaining width is divided
             * equally between the two cards.
             */

            val availableCardWidth =
                (
                        maxWidth
                                - (horizontalPadding * 2)
                                - cardSpacing
                        ) / 2


            /*
             * Keep the original 175.dp width
             * whenever the screen is large enough.
             *
             * On smaller screens the cards
             * automatically shrink.
             */

            val cardWidth =
                availableCardWidth.coerceAtMost(175.dp)


            /*
             * ORIGINAL CARD HEIGHT
             *
             * Height does NOT depend on screen
             * height.
             */

            val cardHeight = 125.dp


            /*
             * SCROLLABLE CONTENT
             */

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                /*
                 * ORIGINAL HEADER SPACE
                 */

                Spacer(
                    modifier = Modifier.height(
                        headerHeight
                    )
                )


                /*
                 * CONTENT CONTAINER
                 */

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = horizontalPadding
                        )
                        .offset(
                            y = (-15).dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            verticalSpacing
                        )
                ) {


                    /*
                     * CARD ROW
                     */

                    Row(
                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.Center,

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {


                        /*
                         * SEND MESSAGE CARD
                         */

                        HelpCard(
                            iconRes =
                                R.drawable.message_icon,

                            title =
                                if (isSharing)
                                    stringResource(
                                        R.string.stop_sharing
                                    )
                                else
                                    stringResource(
                                        R.string.send_message
                                    ),

                            onClick = {
                                onSendMessageClicked()
                            },

                            cardWidth =
                                cardWidth,

                            cardHeight =
                                cardHeight
                        )


                        /*
                         * ORIGINAL 20dp GAP
                         */

                        Spacer(
                            modifier =
                                Modifier.width(
                                    cardSpacing
                                )
                        )


                        /*
                         * MAP TO HOME CARD
                         */

                        HelpCard(
                            iconRes =
                                R.drawable.map_to_home_icon,

                            title =
                                stringResource(
                                    R.string.map_to_home
                                ),

                            // [MAP TO HOME CLICK] opens Google Maps walking navigation to home
                            onClick = {
                                openGoogleMapsNavigation(homeLatitude, homeLongitude)
                            },

                            iconWidth = 66.dp,

                            iconHeight = 56.dp,

                            cardWidth =
                                cardWidth,

                            cardHeight =
                                cardHeight
                        )
                    }


                    /*
                     * LIVE TRACKING HINT (only while sharing)
                     */

                    if (isSharing) {
                        Text(
                            text = stringResource(
                                R.string.sharing_live_tracking_hint,
                                LocationShareService.MIN_DISTANCE_METERS.toInt()
                            ),
                            fontSize = 13.sp,
                            fontFamily = OutfitFont,
                            color = Color(0xFF5A5A5A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }


            /*
             * HEADER
             *
             * Your header is already responsive,
             * so nothing is changed here.
             */

            HeaderSection(
                stringResource(R.string.help),

                spacing = 74.dp,

                bottomspace = 44.dp,

                onBack = onBack
            )
        }
    }
}


/*
 * HELP CARD
 */

@Composable
fun HelpCard(
    iconRes: Int,
    title: String,
    onClick: () -> Unit,

    iconWidth: Dp = 55.dp,

    iconHeight: Dp = 47.dp,

    cardWidth: Dp = 175.dp,

    cardHeight: Dp = 125.dp
) {

    val appColors = AppTheme.colors

    Column(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)

            /*
             * ORIGINAL SHADOW
             */

            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp)
            )

            /*
             * ORIGINAL SHAPE
             */

            .clip(
                RoundedCornerShape(16.dp)
            )

            /*
             * ORIGINAL BACKGROUND
             */

            .background(
                appColors.textfield
            )

            /*
             * ORIGINAL CLICK BEHAVIOR
             */

            .clickable {
                onClick()
            }

            /*
             * Small horizontal padding prevents
             * text from touching the card edges
             * when the card becomes narrow.
             */

            .padding(
                horizontal = 8.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {


        /*
         * ICON
         */

        Icon(
            painter =
                painterResource(
                    id = iconRes
                ),

            contentDescription =
                title,

            tint =
                AppTheme.colors.backButton,

            modifier =
                Modifier.size(
                    iconWidth,
                    iconHeight
                )
        )


        /*
         * ORIGINAL 12dp SPACING
         */

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )


        /*
         * [HELP CARD TEXT] — one line, shrinks to fit
         *
         * The card height is fixed (125.dp). In English, "Send Message"
         * and "Map to Home" did not fit on one line at 19.sp in the
         * Martel font, so they wrapped to 2 lines and the second line
         * was cut off by the bottom of the card. FitText keeps 19.sp
         * when the text fits (Urdu, wide phones) and shrinks it only as
         * much as needed (down to 13.sp) to stay on one line.
         */

        FitText(
            text = title,

            maxFontSize = 19.sp,

            minFontSize = 13.sp,

            fontWeight =
                FontWeight.Medium,

            fontFamily =
                MartelFont,

            color =
                Color.Black,

            maxLines = 1,

            textAlign =
                TextAlign.Center,

            modifier =
                Modifier.fillMaxWidth()
        )
    }
}


/*
 * PREVIEW
 */

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun HelpScreenPreview() {

    GreenTheme {

        HelpScreen()
    }
}