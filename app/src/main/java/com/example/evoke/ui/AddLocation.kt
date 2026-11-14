package com.example.evoke.ui

import android.Manifest
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evoke.location.LocationHelper
import com.example.evoke.location.SharedLocationInbox
import com.example.evoke.ui.components.FieldLabel
import com.example.evoke.ui.components.HeaderSection
import com.example.evoke.ui.components.LocationPickerField
import com.example.evoke.ui.components.ShadowButton
import com.example.evoke.ui.components.ShadowTextField
import com.example.evoke.ui.components.rememberBottomContentPadding
import com.example.evoke.ui.theme.AppTheme
import com.example.evoke.ui.theme.BlueTheme
import com.example.evoke.ui.theme.OutfitFont
import com.example.evoke.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * [ADD LOCATION FLOW]
 *  1. "Get Location" (yellow)  -> GPS current location, works OFFLINE
 *  2. "Confirm?" dialog -> Save  (current location)
 *                         -> Customise (ONLINE): opens the Google Maps app; the user
 *                            long-presses a place, taps Share -> Evoke; Evoke comes back
 *                            here, reads the shared link and shows "Confirm?" -> Save
 *  3. Saved in VARIABLES (no database yet); the place name (online) or the
 *     coordinates (offline) is shown in the Location box
 *  4. Location box (green pin) -> opens the saved location in the Google Maps app,
 *     or in the browser if the app is not installed (no navigation)
 */
private sealed class LocationStep {
    object Idle : LocationStep()
    object Finding : LocationStep()
    data class ShowCurrent(val location: Location) : LocationStep()
    object Reading : LocationStep()                                   // reading a shared link
    data class ShowShared(val place: LocationHelper.SharedPlace) : LocationStep()
}

@Composable
fun AddLocationContent(
    onAddClick: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val appColors = AppTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // rememberSaveable: kept even if Android closes the app while Google Maps is open
    var titleText by rememberSaveable { mutableStateOf("") }
    var descriptionText by rememberSaveable { mutableStateOf("") }

    /*
     * [SELECTED LOCATION VARIABLES]
     * TODO: save these to the database (locations table) once the
     * database is connected to this screen.
     */
    var selectedLocation by rememberSaveable { mutableStateOf("") }          // name shown in the Location box
    var selectedLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var selectedLongitude by rememberSaveable { mutableStateOf<Double?>(null) }

    var step by remember { mutableStateOf<LocationStep>(LocationStep.Idle) }
    var findingJob by remember { mutableStateOf<Job?>(null) }

    fun toast(resId: Int) = Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG).show()

    // [GET CURRENT LOCATION] GPS — works offline
    fun findCurrentLocation() {
        if (!LocationHelper.isLocationEnabled(context)) {
            toast(R.string.addloc_turn_on_location)
            return
        }
        step = LocationStep.Finding
        findingJob = scope.launch {
            val location = LocationHelper.getCurrentLocation(context)
            if (location != null) {
                step = LocationStep.ShowCurrent(location)
            } else {
                step = LocationStep.Idle
                toast(R.string.addloc_location_not_found)
            }
        }
    }

    // [LOCATION PERMISSION]
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            findCurrentLocation()
        } else {
            toast(R.string.addloc_permission_needed)
        }
    }

    fun onGetLocationClick() {
        if (LocationHelper.hasLocationPermission(context)) {
            findCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    // [CONFIRMED LOCATION] store in variables, then show the name in the Location box
    fun onLocationConfirmed(latitude: Double, longitude: Double, knownName: String? = null) {
        selectedLatitude = latitude
        selectedLongitude = longitude
        step = LocationStep.Idle
        if (!knownName.isNullOrBlank()) {
            selectedLocation = knownName              // e.g. "Liberty Market" shared by Google Maps
            return
        }
        selectedLocation = LocationHelper.formatCoordinates(latitude, longitude)   // shown at once
        scope.launch {
            // Online: real place name. Offline: stays as coordinates.
            val name = LocationHelper.placeName(context, latitude, longitude)
            if (selectedLatitude == latitude && selectedLongitude == longitude) {
                selectedLocation = name
            }
        }
    }

    // [CUSTOMISE] open Google Maps so the user can pick a place and share it back
    fun onCustomiseClick(latitude: Double, longitude: Double) {
        if (!LocationHelper.isOnline(context)) {
            toast(R.string.addloc_internet_needed)
            return
        }
        step = LocationStep.Idle
        toast(R.string.addloc_customise_hint)
        if (!LocationHelper.openGoogleMapsForPicking(context, latitude, longitude)) {
            toast(R.string.addloc_no_maps_app)
        }
    }

    // [SHARED FROM GOOGLE MAPS] text arrives via MainActivity -> SharedLocationInbox
    // Collected once (LaunchedEffect(Unit)); the slow link reading runs in its own job,
    // so clearing the inbox can never cancel it (that left "Reading location…" stuck).
    LaunchedEffect(Unit) {
        SharedLocationInbox.pending.collect { pending ->
            if (pending == null) return@collect
            val text = SharedLocationInbox.consume() ?: return@collect
            step = LocationStep.Reading
            scope.launch {
                val place = LocationHelper.parseSharedLocation(text)
                if (place != null) {
                    step = LocationStep.ShowShared(place)
                } else {
                    step = LocationStep.Idle
                    toast(if (LocationHelper.isOnline(context)) R.string.addloc_read_failed else R.string.addloc_internet_needed)
                }
            }
        }
    }

    // [OPEN IN GOOGLE MAPS] Location box / green pin
    fun onLocationBoxClick() {
        val lat = selectedLatitude
        val lng = selectedLongitude
        if (lat == null || lng == null) {
            toast(R.string.addloc_select_first)
            return
        }
        val label = titleText.trim().ifEmpty { selectedLocation }
        if (!LocationHelper.openInGoogleMaps(context, lat, lng, label)) {
            toast(R.string.addloc_no_maps_app)
        }
    }

    Scaffold(
        containerColor = appColors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)  // insets already handled at root
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            /*
             * RESPONSIVE DIMENSIONS
             * (24.dp padding / 92.dp button were the design values)
             */

            // Same rule as the other screens; 24.dp on a ~400.dp wide phone
            val horizontalPadding = (maxWidth * 0.06f)
                .coerceIn(20.dp, 32.dp)

            val contentWidth = maxWidth - horizontalPadding * 2

            // Right column (Get Location label + button) gets a fixed share of the
            // width. Before it grew to fit the label on one line and squeezed the
            // location field on small screens. The label wraps to 2 lines if needed.
            val getLocationColumnWidth = (contentWidth * 0.32f)
                .coerceIn(110.dp, 130.dp)

            val getLocationButtonWidth = (contentWidth * 0.26f)
                .coerceIn(80.dp, 92.dp)
                .coerceAtMost(getLocationColumnWidth)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Spacer pushes content down below header height
                Spacer(modifier = Modifier.height(260.dp))  // ← match your headerHeight

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding)
                        .offset(y = (-40).dp)
                ) {

                    // Bottom-aligned: the location field and the button always line
                    // up, even when the right label wraps to 2 lines.
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // ── Left: Label + location display ────────────────────────
                        Column(modifier = Modifier.weight(1f)) {
                            FieldLabel(stringResource(R.string.location_required))
                            Spacer(modifier = Modifier.height(8.dp))
                            // [LOCATION BOX] shows the place name; tap -> Google Maps
                            LocationPickerField(
                                value = selectedLocation,
                                placeholder = stringResource(R.string.open_location_in_map),
                                onClick = { onLocationBoxClick() }
                            )
                        }

                        // ── Right: Label + button ─────────────────────────────────
                        // Fixed width, so the full-width FieldLabel is safe here.
                        Column(
                            modifier = Modifier.width(getLocationColumnWidth),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // One line: shrinks to fit the narrow column
                            // (the Urdu label used to wrap onto 2 lines).
                            FieldLabel(
                                stringResource(R.string.get_location),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // [GET LOCATION BUTTON]
                            ShadowButton(
                                width = getLocationButtonWidth,
                                // Same height as LocationPickerField (52.dp) so the
                                // edges match (51.dp was resized by ShadowButton).
                                height = 52.dp,
                                color = appColors.popupText,
                                cornerRadius = 15.dp,
                                onClick = { onGetLocationClick() }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.location_icon),
                                    contentDescription = stringResource(R.string.map_pin),
                                    tint = appColors.pagesText,
                                    modifier = Modifier.size(35.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Title ─────────────────────────────────────────────────────
                    FieldLabel(stringResource(R.string.title_required))
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        placeholder = stringResource(R.string.title_placeholder),
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Description ───────────────────────────────────────────────
                    FieldLabel(stringResource(R.string.description_required))
                    Spacer(modifier = Modifier.height(8.dp))
                    ShadowTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        placeholder = stringResource(R.string.description_placeholder),
                        height = 160.dp,
                        singleLine = false,
                        maxLines = 6,
                    )

                    Spacer(modifier = Modifier.height(55.dp))

                    // ── Green Add Button ───────────────────────────────────────────
                    // (not saved to the database yet — see [SELECTED LOCATION VARIABLES])
                    ShadowButton(
                        height = 56.dp,
                        color = appColors.pagesText,
                        cornerRadius = 30.dp,
                        onClick = onAddClick
                    ) {
                        Text(
                            text = stringResource(R.string.add),
                            color = appColors.popupText,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = OutfitFont
                        )
                    }
                }

                Spacer(modifier = Modifier.height(rememberBottomContentPadding()))
            }

            HeaderSection(
                stringResource(R.string.add_location_header),
                spacing = 57.dp,
                onBack = { onBack() }
            )
        }
    }

    // ── [LOCATION DIALOGS] ──────────────────────────────────────────────────
    when (val s = step) {
        LocationStep.Idle -> Unit

        // [FINDING DIALOG] waiting for the GPS fix
        LocationStep.Finding -> LocationDialog(
            title = stringResource(R.string.addloc_finding),
            onDismiss = {
                findingJob?.cancel()
                step = LocationStep.Idle
            },
            dismissText = stringResource(R.string.addloc_cancel),
            onDismissClick = {
                findingJob?.cancel()
                step = LocationStep.Idle
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = appColors.pagesText,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                DialogText(stringResource(R.string.addloc_finding_hint))
            }
        }

        // [CURRENT LOCATION DIALOG] offline-capable result -> Confirm saves it
        is LocationStep.ShowCurrent -> LocationDialog(
            title = stringResource(R.string.addloc_current_title),
            onDismiss = { step = LocationStep.Idle },
            confirmText = stringResource(R.string.addloc_confirm),
            onConfirmClick = { onLocationConfirmed(s.location.latitude, s.location.longitude) },
            dismissText = stringResource(R.string.addloc_customise),
            onDismissClick = { onCustomiseClick(s.location.latitude, s.location.longitude) }
        ) {
            Column {
                DialogText(LocationHelper.formatCoordinates(s.location.latitude, s.location.longitude))
                if (s.location.hasAccuracy()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DialogText(
                        stringResource(R.string.addloc_accuracy, s.location.accuracy.roundToInt()),
                        small = true
                    )
                }
            }
        }

        // [READING DIALOG] resolving the link shared by Google Maps
        LocationStep.Reading -> LocationDialog(
            title = stringResource(R.string.addloc_reading),
            onDismiss = { step = LocationStep.Idle }
        ) {
            CircularProgressIndicator(
                color = appColors.pagesText,
                modifier = Modifier.size(28.dp)
            )
        }

        // [SHARED LOCATION DIALOG] place chosen in Google Maps -> Save
        is LocationStep.ShowShared -> LocationDialog(
            title = stringResource(R.string.addloc_shared_title),
            onDismiss = { step = LocationStep.Idle },
            confirmText = stringResource(R.string.addloc_confirm),
            onConfirmClick = { onLocationConfirmed(s.place.latitude, s.place.longitude, s.place.name) },
            dismissText = stringResource(R.string.addloc_customise),
            onDismissClick = { onCustomiseClick(s.place.latitude, s.place.longitude) }
        ) {
            Column {
                s.place.name?.let {
                    DialogText(it)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                DialogText(LocationHelper.formatCoordinates(s.place.latitude, s.place.longitude), small = s.place.name != null)
            }
        }
    }
}

/** [LOCATION DIALOG STYLE] app colours + Outfit font for all Add Location dialogs. */
@Composable
private fun LocationDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    onConfirmClick: () -> Unit = {},
    dismissText: String? = null,
    onDismissClick: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val appColors = AppTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.background,
        title = {
            Text(
                text = title,
                fontFamily = OutfitFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = appColors.pagesText
            )
        },
        text = content,
        confirmButton = {
            if (confirmText != null) {
                TextButton(onClick = onConfirmClick) {
                    Text(
                        text = confirmText,
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = appColors.pagesText
                    )
                }
            }
        },
        dismissButton = {
            if (dismissText != null) {
                TextButton(onClick = onDismissClick) {
                    Text(
                        text = dismissText,
                        fontFamily = OutfitFont,
                        fontSize = 17.sp,
                        color = appColors.pagesText
                    )
                }
            }
        }
    )
}

@Composable
private fun DialogText(text: String, small: Boolean = false) {
    Text(
        text = text,
        fontFamily = OutfitFont,
        fontSize = if (small) 15.sp else 18.sp,
        color = AppTheme.colors.pagesText.copy(alpha = if (small) 0.75f else 1f)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddLocationPreview() {
    BlueTheme {
        AddLocationContent()
    }
}
