package com.example.evoke.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Helpers for the Add Location screen.
 *
 *  - [getCurrentLocation]  GPS position — works OFFLINE (GPS satellites, no internet),
 *                          same approach as Help > Send Message (LocationShareService).
 *  - [isOnline]            true when the phone has a working internet connection.
 *  - [placeName]           coordinates -> short place name (Android Geocoder, free,
 *                          needs internet). Offline it returns the coordinates instead.
 *  - [openInGoogleMaps]    opens a location in the Google Maps app (pin only, no navigation).
 */
object LocationHelper {

    private const val TAG = "LocationHelper"

    /** Longest wait for a GPS fix. Offline (no A-GPS help) a cold start can be slow. */
    private const val LOCATION_TIMEOUT_MS = 60_000L

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Is the phone's Location (GPS) switch on? */
    fun isLocationEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    /**
     * Current position with high accuracy (GPS). Works without internet.
     * Falls back to the last known position if no fresh fix arrives in time.
     * Returns null when no position is available at all.
     */
    @SuppressLint("MissingPermission")   // checked by hasLocationPermission() first
    suspend fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val fresh = try {
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fresh location failed: ${e.message}")
            null
        }
        if (fresh != null) return fresh
        return try {
            client.lastLocation.await()
        } catch (e: Exception) {
            Log.w(TAG, "Last known location failed: ${e.message}")
            null
        }
    }

    /** True when the active network can reach the internet. */
    fun isOnline(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** "31.52040, 74.35870" */
    fun formatCoordinates(latitude: Double, longitude: Double): String =
        String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

    /**
     * Short place name such as "Liberty Market, Gulberg III, Lahore".
     * Uses Android's built-in Geocoder (free, needs internet).
     * Offline, or if nothing is found, returns the coordinates.
     */
    suspend fun placeName(context: Context, latitude: Double, longitude: Double): String {
        val fallback = formatCoordinates(latitude, longitude)
        if (!isOnline(context) || !Geocoder.isPresent()) return fallback
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val geocoder = Geocoder(context, locale)
        val address = try {
            withTimeoutOrNull(10_000L) { firstAddress(geocoder, latitude, longitude) }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder failed: ${e.message}")
            null
        } ?: return fallback
        return shortName(address) ?: fallback
    }

    @Suppress("DEPRECATION")
    private suspend fun firstAddress(geocoder: Geocoder, latitude: Double, longitude: Double): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
        }

    /** Builds a short name from the parts of an address (skips plain house numbers). */
    private fun shortName(address: Address): String? {
        val feature = address.featureName?.takeIf { name -> name.any { it.isLetter() } }
        val parts = listOfNotNull(
            feature,
            address.thoroughfare,
            address.subLocality,
            address.locality
        ).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return when {
            parts.isNotEmpty() -> parts.take(3).joinToString(", ")
            else -> address.getAddressLine(0)
        }
    }

    // ── [CUSTOMISE: GOOGLE MAPS SHARE] ──────────────────────────────────────

    /** A location read from a Google Maps share. [name] is null for a dropped pin. */
    data class SharedPlace(val latitude: Double, val longitude: Double, val name: String?)

    /**
     * Opens Google Maps at [latitude],[longitude] WITHOUT a pin, so the user can
     * long-press any place and then tap Share -> Evoke. Falls back to the browser.
     */
    fun openGoogleMapsForPicking(context: Context, latitude: Double, longitude: Double): Boolean {
        val lat = String.format(Locale.US, "%.6f", latitude)
        val lng = String.format(Locale.US, "%.6f", longitude)
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?z=17")).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(mapsIntent)
            true
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/@$lat,$lng,17z"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            } catch (e: ActivityNotFoundException) {
                false
            }
        }
    }

    /**
     * Reads the location from text shared by Google Maps, for example
     *   "Dropped pin https://maps.app.goo.gl/AbC123"
     *   "Liberty Market https://maps.app.goo.gl/XyZ"
     * The short link hides the coordinates, so it is opened (needs internet) and the
     * redirect address is searched for them. Returns null if no location is found.
     */
    suspend fun parseSharedLocation(text: String): SharedPlace? = withContext(Dispatchers.IO) {
        // Never wait forever: give up after 25 s in total
        withTimeoutOrNull(25_000L) { runInterruptible { parseSharedLocationInternal(text) } }
    }

    private fun parseSharedLocationInternal(text: String): SharedPlace? {
        val url = URL_REGEX.find(text)?.value
        val name = text.replace(URL_REGEX, " ")
            .lines().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            ?.takeUnless { it.equals("Dropped pin", ignoreCase = true) || it.any { c -> c.isDigit() } && coordinatesIn(it) != null }

        // 1) coordinates written directly in the text / link
        coordinatesIn(text)?.let { return SharedPlace(it.first, it.second, name) }
        if (url == null) return null

        // 2) follow the short link's redirects and look in each address
        var current: String = url
        for (hop in 0 until 6) {
            coordinatesIn(current)?.let { return SharedPlace(it.first, it.second, name) }
            val next = try {
                nextRedirect(current)
            } catch (e: Exception) {
                Log.w(TAG, "Resolving shared link failed: ${e.message}")
                return null
            }
            Log.i(TAG, "Shared link hop $hop: $current -> $next")
            if (next == null) break
            current = next
        }
        coordinatesIn(current)?.let { return SharedPlace(it.first, it.second, name) }

        // 3) last resort: look for coordinates inside the final page itself
        return try {
            pageCoordinates(current)?.let { SharedPlace(it.first, it.second, name) }
        } catch (e: Exception) {
            Log.w(TAG, "Reading shared page failed: ${e.message}")
            null
        }
    }

    /** Reads the start of the page and searches it for coordinates. */
    private fun pageCoordinates(address: String): Pair<Double, Double>? {
        val connection = (java.net.URL(address).openConnection() as java.net.HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Evoke")
        }
        return try {
            val body = connection.inputStream.bufferedReader().use { reader ->
                val buffer = CharArray(300_000)
                val count = reader.read(buffer)
                if (count > 0) String(buffer, 0, count) else ""
            }
            coordinatesIn(body)
                ?: Regex("""center=(-?\d{1,2}\.\d+)%2C(-?\d{1,3}\.\d+)""").find(body)?.let {
                    it.groupValues[1].toDouble() to it.groupValues[2].toDouble()
                }
                ?: Regex("""\[null,null,(-?\d{1,2}\.\d{4,}),(-?\d{1,3}\.\d{4,})\]""").find(body)?.let {
                    it.groupValues[1].toDouble() to it.groupValues[2].toDouble()
                }
        } finally {
            connection.disconnect()
        }
    }

    private val URL_REGEX = Regex("""https?://\S+""")

    /** Most precise first: the place's own point (!3d!4d), then q=/ll=, then search/place paths, then @view. */
    private val COORDINATE_PATTERNS = listOf(
        Regex("""!3d(-?\d{1,2}\.\d+)!4d(-?\d{1,3}\.\d+)"""),
        Regex("""[?&](?:q|query|ll|center|destination)=(-?\d{1,2}\.\d+)\s*,\s*\+?(-?\d{1,3}\.\d+)"""),
        Regex("""/(?:search|place|dir)/(-?\d{1,2}\.\d+)\s*,\s*\+?(-?\d{1,3}\.\d+)"""),
        Regex("""geo:(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)"""),
        Regex("""@(-?\d{1,2}\.\d+),(-?\d{1,3}\.\d+)"""),
        Regex("""^\s*(-?\d{1,2}\.\d{3,})\s*,\s*(-?\d{1,3}\.\d{3,})\s*$""", RegexOption.MULTILINE)
    )

    private fun coordinatesIn(raw: String): Pair<Double, Double>? {
        val text = try {
            java.net.URLDecoder.decode(raw, "UTF-8")
        } catch (e: Exception) {
            raw
        }
        for (pattern in COORDINATE_PATTERNS) {
            val match = pattern.find(text) ?: continue
            val lat = match.groupValues[1].toDoubleOrNull() ?: continue
            val lng = match.groupValues[2].toDoubleOrNull() ?: continue
            if (lat in -90.0..90.0 && lng in -180.0..180.0 && !(lat == 0.0 && lng == 0.0)) return lat to lng
        }
        return null
    }

    /** One redirect step: returns the "Location" address, or null when there is none. */
    private fun nextRedirect(address: String): String? {
        val connection = (java.net.URL(address).openConnection() as java.net.HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 8_000
            readTimeout = 8_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Evoke")
        }
        return try {
            val code = connection.responseCode
            if (code in 300..399) {
                connection.getHeaderField("Location")?.let { location ->
                    java.net.URL(java.net.URL(address), location).toString()
                }
            } else {
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Opens the location in the Google Maps app with a pin (no navigation).
     * If Google Maps is not installed, opens Google Maps in the browser.
     * Works offline too: Maps shows the pin; the map around it appears only if the
     * area was downloaded as an offline map or viewed recently.
     * Returns false if no app could open it.
     */
    fun openInGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String): Boolean {
        val lat = String.format(Locale.US, "%.6f", latitude)
        val lng = String.format(Locale.US, "%.6f", longitude)
        val mapsIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        ).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(mapsIntent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Google Maps app not found, opening browser")
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            } catch (e: ActivityNotFoundException) {
                false
            }
        }
    }
}
