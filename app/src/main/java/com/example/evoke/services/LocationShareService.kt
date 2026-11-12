package com.example.evoke.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.evoke.R
import com.example.evoke.data.ContactRepository
import com.example.evoke.data.DatabaseContactRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

/**
 * Offline "live tracking" via SMS with contact escalation.
 *
 * Behavior:
 *  - Movement of MIN_DISTANCE_METERS+ triggers a send to the current active contact.
 *  - Every send — including the very first send for a given movement trigger —
 *    fetches a genuinely LIVE location fix (getCurrentLocation, same approach
 *    as EmergencyAlertService) right before texting it. The Location object
 *    handed to onMovementDetected() by the passive LocationCallback is only
 *    used to confirm that MIN_DISTANCE_METERS was crossed; it is never sent
 *    as-is. If the live fetch fails or times out, that triggering fix is used
 *    as a fallback so a slow GPS fix doesn't silently drop the message.
 *  - Each send gets up to 2 attempts. "Failed" here means the OS/carrier couldn't
 *    even hand the SMS off (no service, radio off, generic failure) — NOT the
 *    carrier delivery report, which is unreliable in many regions and isn't used
 *    to gate retries. Tries every active SIM before declaring a real failure.
 *  - Every attempt (including retries) fetches a BRAND NEW location fix — never
 *    reuses a stale one.
 *  - The reply-wait clock (REPLY_WAIT_MS, 10 min) is independent of movement:
 *    it starts once when a contact first becomes active, and is NOT reset by
 *    routine movement-triggered sends to that same contact. It only resets when
 *    an actual reply SMS arrives (restarting a fresh window — they need to keep
 *    replying roughly every REPLY_WAIT_MS to stay active), or moves forward when
 *    it expires with no reply at all, escalating to the next contact.
 *  - If a message fails both attempts, we escalate to the next contact
 *    immediately (no reply-wait for a contact we couldn't even reach).
 *  - Escalation only moves forward through the list — it never resets to
 *    contact 0 once movement/time has advanced things, EXCEPT via the
 *    stop/restart rule below.
 *  - If every contact is exhausted, sharing stops and emits "all_contacts_exhausted".
 *
 *  - STOP/RESTART RESUME RULE: stopping sharing remembers which contact was
 *    active and the timestamp of that stop. The next time sharing is started:
 *      - If it's started again within RESTART_RESUME_WINDOW_MS (10 min) of
 *        that stop, it resumes at the NEXT contact after the one that was
 *        active — the same treatment as a no-reply timeout, since going
 *        silent and quickly restarting looks the same from the active
 *        contact's perspective.
 *      - If it's started later than that (or this is the first run ever),
 *        it starts over at contact 0.
 *    If the resulting resume index would be past the end of the contact
 *    list, it falls back to contact 0 rather than immediately reporting
 *    exhaustion.
 *
 * IMPORTANT — Play Store note: RECEIVE_SMS is tightly restricted by Google Play
 * policy (generally only default-SMS-handler apps get approved for it). If this
 * app isn't going through Play (sideloaded/enterprise/family-only distribution),
 * you're fine. If you do plan to publish on Play, verify this against their
 * current SMS/Call Log permissions policy before shipping.
 */
class LocationShareService : Service() {

    // Emergency contacts saved in the Room database (emergency_contacts table).
    // Lazy so the Service context is attached before it is used.
    private val contactRepository: ContactRepository by lazy { DatabaseContactRepository(applicationContext) }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sendMutex = Mutex()

    private var contacts: List<String> = emptyList()
    private var activeContactIndex = 0
    private var replyWaitJob: Job? = null
    // Which contact index the currently-running replyWaitJob belongs to. Movement
    // sends don't touch the timer at all — only escalation (new index) or an
    // actual reply (restarts fresh for the same index) does.
    private var replyWaitOwnerIndex = -1
    private var smsReplyReceiver: BroadcastReceiver? = null

    // Correlates SmsManager's sentIntent broadcast back to the coroutine awaiting it
    private val pendingSendResults = mutableMapOf<Int, CompletableDeferred<Boolean>>()
    private val requestCodeCounter = AtomicInteger(0)
    private var sentResultReceiver: BroadcastReceiver? = null

    private var messagesSentTotal = 0

    companion object {
        const val CHANNEL_ID = "location_share_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.example.evoke.action.STOP_SHARING"
        private const val ACTION_SMS_SENT = "com.example.evoke.action.SMS_SENT_RESULT"
        private const val EXTRA_REQUEST_CODE = "request_code"

        const val MIN_DISTANCE_METERS = 150f
        const val MIN_CHECK_INTERVAL_MS = 15_000L
        const val MAX_ATTEMPTS_PER_CONTACT = 2
        const val REPLY_WAIT_MS = 10 * 60 * 1000L // 10 minutes
        const val MAX_MESSAGES = 100 // overall safety cap across the whole session

        // How long to wait for a genuinely live fix before falling back to
        // whatever location triggered the movement check. Mirrors
        // EmergencyAlertService's LOCATION_FETCH_TIMEOUT_MS.
        private const val LOCATION_FETCH_TIMEOUT_MS = 20_000L

        // If sharing is restarted within this long of the last stop, resume
        // at the NEXT contact instead of starting over at contact 0. Same
        // 10-minute value as the reply-wait window by design — a restart
        // inside that window is treated like the previously-active contact
        // having timed out with no reply.
        const val RESTART_RESUME_WINDOW_MS = REPLY_WAIT_MS

        private const val PREFS_NAME = "location_share_service_state"
        private const val KEY_LAST_ACTIVE_INDEX = "last_active_contact_index"
        private const val KEY_LAST_STOPPED_AT = "last_stopped_at"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        // e.g. "sent:<n>:<contactName>", "escalated:<contactName>",
        // "no_reply_escalating:<contactName>", "all_contacts_exhausted", etc.
        private val _lastEvent = MutableStateFlow<String?>(null)
        val lastEvent: StateFlow<String?> = _lastEvent

        private const val TAG = "LocationShareService"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        registerSentResultReceiver()
        registerSmsReplyReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSharing("stopped_by_user")
            return START_NOT_STICKY
        }

        _isRunning.value = true
        _lastEvent.value = null
        replyWaitOwnerIndex = -1
        messagesSentTotal = 0
        // Computed now (before contacts load) from persisted stop state; the
        // in-memory activeContactIndex only becomes authoritative once we
        // know how many contacts there actually are, below.
        val resumeIndex = resolveStartingContactIndex()
        startForeground(NOTIFICATION_ID, buildNotification())

        serviceScope.launch {
            contacts = contactRepository.getContactsOrderedByPriority().take(5)
            if (contacts.isEmpty()) {
                Log.e(TAG, "No contacts configured — stopping")
                _lastEvent.value = "no_contact"
                stopSharing("no_contact")
                return@launch
            }
            activeContactIndex = if (resumeIndex in contacts.indices) resumeIndex else 0
            Log.i(TAG, "Starting sharing at contact index $activeContactIndex")
            withContext(Dispatchers.Main) { startLocationUpdates() }
        }

        return START_STICKY
    }

    /**
     * Decides where to pick up based on when sharing was last stopped:
     *  - within RESTART_RESUME_WINDOW_MS of that stop -> next contact after
     *    the one that was active (treated like a no-reply timeout)
     *  - otherwise (or no prior stop recorded) -> contact 0
     * Clamped against the real contact list once it's loaded, in onStartCommand.
     */
    private fun resolveStartingContactIndex(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastStoppedAt = prefs.getLong(KEY_LAST_STOPPED_AT, 0L)
        if (lastStoppedAt == 0L) return 0

        val lastIndex = prefs.getInt(KEY_LAST_ACTIVE_INDEX, 0)
        val elapsed = System.currentTimeMillis() - lastStoppedAt
        return if (elapsed in 0..RESTART_RESUME_WINDOW_MS) {
            Log.i(TAG, "Restarted ${elapsed}ms after last stop (<= ${RESTART_RESUME_WINDOW_MS}ms) — resuming after contact $lastIndex")
            lastIndex + 1
        } else {
            Log.i(TAG, "Restarted ${elapsed}ms after last stop (> ${RESTART_RESUME_WINDOW_MS}ms) — starting over at contact 0")
            0
        }
    }

    private fun saveStopState(index: Int) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_LAST_ACTIVE_INDEX, index)
            .putLong(KEY_LAST_STOPPED_AT, System.currentTimeMillis())
            .apply()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "ACCESS_FINE_LOCATION not granted at service runtime")
            _lastEvent.value = "no_location_permission"
            stopSharing("no_location_permission")
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, MIN_CHECK_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_CHECK_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISTANCE_METERS)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                serviceScope.launch { onMovementDetected(location) }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback as LocationCallback,
            mainLooper
        )

        serviceScope.launch {
            delay(30_000L)
            if (messagesSentTotal == 0 && _isRunning.value) {
                Log.w(TAG, "No location fix after 30s")
                _lastEvent.value = "waiting_for_gps"
            }
        }
    }

    /**
     * Movement crossed the distance threshold — send to whoever is currently
     * active. `triggeringLocation` only proves the threshold was crossed; a
     * fresh, genuinely live fix is fetched right before sending (see
     * dispatchToActiveContact / fetchFreshLocationOrNull), falling back to
     * `triggeringLocation` only if that live fetch fails or times out.
     *
     * Deliberately does NOT touch replyWaitJob: the reply-wait clock
     * runs independently of how many location updates go out in the meantime.
     * It only resets on an actual reply, or moves forward on escalation.
     */
    private suspend fun onMovementDetected(triggeringLocation: Location) {
        val liveLocation = fetchFreshLocationOrNull() ?: triggeringLocation
        dispatchToActiveContact(liveLocation)
    }

    /**
     * Sends to contacts[activeContactIndex], retrying with a FRESH location fetch
     * on failure, and escalating forward through the list as needed.
     * `firstLocation` is used for the very first attempt only — every retry after
     * that fetches a brand new fix, per requirement.
     */
    private suspend fun dispatchToActiveContact(firstLocation: Location) = sendMutex.withLock {
        if (messagesSentTotal >= MAX_MESSAGES) {
            Log.i(TAG, "Hit overall MAX_MESSAGES cap — stopping")
            _lastEvent.value = "max_messages_reached"
            stopSharing("max_messages_reached")
            return@withLock
        }

        var location = firstLocation
        var attempt = 1

        while (activeContactIndex < contacts.size) {
            val contact = contacts[activeContactIndex]

            val sendOk = trySendSms(contact, location, attempt)
            if (sendOk) {
                messagesSentTotal++
                Log.i(TAG, "Sent to $contact (attempt $attempt)")
                _lastEvent.value = "sent:$messagesSentTotal:$contact"
                // Only (re)start the clock if this contact doesn't already own a
                // running one — i.e. this is a freshly-activated contact (session
                // start or just escalated to). A routine movement-triggered send
                // to a contact who's already being timed doesn't touch the timer.
                if (replyWaitOwnerIndex != activeContactIndex) {
                    startReplyWaitWindow(contact)
                    replyWaitOwnerIndex = activeContactIndex
                }
                return@withLock
            }

            Log.w(TAG, "Send to $contact failed (attempt $attempt)")
            if (attempt < MAX_ATTEMPTS_PER_CONTACT) {
                attempt++
                // Fresh fix for the retry — never reuse the old one.
                location = fetchFreshLocationOrNull() ?: location
                continue
            }

            // Both attempts failed — escalate to next contact, resetting attempt count.
            _lastEvent.value = "escalated_send_failed:$contact"
            activeContactIndex++
            attempt = 1
            location = fetchFreshLocationOrNull() ?: location
        }

        Log.e(TAG, "All contacts exhausted")
        _lastEvent.value = "all_contacts_exhausted"
        stopSharing("all_contacts_exhausted")
    }

    private fun startReplyWaitWindow(contact: String) {
        replyWaitJob?.cancel()
        replyWaitJob = serviceScope.launch {
            delay(REPLY_WAIT_MS)
            // Timed out with no reply -> escalate to the next contact.
            Log.i(TAG, "No reply from $contact within window — escalating")
            _lastEvent.value = "no_reply_escalating:$contact"
            sendMutex.withLock {
                activeContactIndex++
            }
            val fresh = fetchFreshLocationOrNull()
            if (fresh != null) {
                dispatchToActiveContact(fresh)
            } else {
                _lastEvent.value = "location_unavailable_for_escalation"
            }
        }
    }

    /**
     * Called by the reply receiver when an incoming SMS matches the active
     * contact. Per spec: a reply *resets* the clock rather than turning
     * monitoring off — they need to keep responding roughly every
     * REPLY_WAIT_MS to remain the active contact.
     */
    private fun onReplyReceivedFromActiveContact(contactPhone: String) {
        Log.i(TAG, "Reply received from $contactPhone — resetting the clock")
        _lastEvent.value = "reply_received:$contactPhone"
        startReplyWaitWindow(contactPhone)
        replyWaitOwnerIndex = activeContactIndex
    }

    /**
     * Fetches a brand-new live fix, capped at LOCATION_FETCH_TIMEOUT_MS so a
     * cold/slow GPS lock can never hang a send indefinitely. Same approach as
     * EmergencyAlertService.fetchFreshLocationOrNull.
     */
    private suspend fun fetchFreshLocationOrNull(): Location? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        return try {
            withTimeoutOrNull(LOCATION_FETCH_TIMEOUT_MS) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fresh location fetch failed: ${e.message}")
            null
        }
    }

    /**
     * Sends one SMS and suspends until the OS reports send success/failure via
     * the sentIntent broadcast (not the delivery report — see class doc).
     */
    private suspend fun trySendSms(contact: String, location: Location, attempt: Int): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _lastEvent.value = "no_sms_permission"
            return false
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            _lastEvent.value = "no_telephony_hardware"
            return false
        }

        val lat = location.latitude
        val lng = location.longitude
        val mapsLink = "https://maps.google.com/?q=$lat,$lng"
        val message = if (messagesSentTotal == 0) {
            "Live location sharing started.\nCoords: $lat,$lng\n$mapsLink"
        } else {
            "Location update (moved ~${MIN_DISTANCE_METERS.toInt()}m+):\nCoords: $lat,$lng\n$mapsLink"
        }

        val smsManagers = getCandidateSmsManagers()
        if (smsManagers.isEmpty()) {
            _lastEvent.value = "no_sim"
            return false
        }

        // Try every active SIM in turn — a SIM with no balance/no service will
        // fail here even though another SIM in the same phone works fine.
        // Only report failure if ALL of them fail.
        for ((index, manager) in smsManagers.withIndex()) {
            val ok = sendViaManager(manager, contact, message)
            if (ok) {
                if (smsManagers.size > 1) {
                    Log.i(TAG, "Sent via SIM slot #$index (of ${smsManagers.size} tried)")
                }
                return true
            }
        }
        return false
    }

    /**
     * Enumerates active SIMs so we're not locked to whichever one happens to be
     * the system "default SMS" SIM. Falls back to the plain default SmsManager
     * if READ_PHONE_STATE isn't granted or subscription info isn't available —
     * this permission is optional, not required, to keep the core feature
     * working even if the user declines it.
     */
    @SuppressLint("MissingPermission") // guarded by the checkSelfPermission call directly below
    private fun getCandidateSmsManagers(): List<SmsManager> {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                        as? SubscriptionManager
                val subIds = subscriptionManager?.activeSubscriptionInfoList?.map { it.subscriptionId }
                if (!subIds.isNullOrEmpty()) {
                    val managers = subIds.mapNotNull { subId ->
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                getSystemService(SmsManager::class.java)?.createForSubscriptionId(subId)
                            } else {
                                @Suppress("DEPRECATION")
                                SmsManager.getSmsManagerForSubscriptionId(subId)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Couldn't get SmsManager for subId $subId: ${e.message}")
                            null
                        }
                    }
                    if (managers.isNotEmpty()) return managers
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "No permission to enumerate SIMs: ${e.message}")
            }
        }

        // Fall back: just the system default SIM.
        val default = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        return listOfNotNull(default)
    }

    /** Sends via one specific SIM's SmsManager and suspends for its send-result. */
    private suspend fun sendViaManager(manager: SmsManager, phone: String, message: String): Boolean {
        val requestCode = requestCodeCounter.incrementAndGet()
        val deferred = CompletableDeferred<Boolean>()
        pendingSendResults[requestCode] = deferred

        val sentPendingIntent = PendingIntent.getBroadcast(
            this, requestCode,
            Intent(ACTION_SMS_SENT).apply {
                setPackage(packageName)
                putExtra(EXTRA_REQUEST_CODE, requestCode)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Single-part send: keeps sent-result correlation simple, and the
            // message above is short enough to fit one SMS segment normally.
            manager.sendTextMessage(phone, null, message, sentPendingIntent, null)
        } catch (e: Exception) {
            Log.e(TAG, "sendTextMessage threw: ${e.message}", e)
            pendingSendResults.remove(requestCode)
            return false
        }

        // Wait for the sent-result broadcast, with a safety timeout in case it
        // never arrives (some OEMs are inconsistent about firing it).
        return try {
            withContext(Dispatchers.Default) {
                withTimeoutOrNull(15_000L) { deferred.await() } ?: run {
                    Log.w(TAG, "No send-result broadcast within 15s — treating as failure")
                    false
                }
            }
        } finally {
            pendingSendResults.remove(requestCode)
        }
    }

    private fun registerSentResultReceiver() {
        sentResultReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)
                val deferred = pendingSendResults[requestCode] ?: return
                val success = resultCode == android.app.Activity.RESULT_OK
                if (!deferred.isCompleted) deferred.complete(success)
            }
        }
        val filter = IntentFilter(ACTION_SMS_SENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(sentResultReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(sentResultReceiver, filter)
        }
    }

    /**
     * Listens for incoming SMS while sharing is active, to detect a reply from
     * the currently active contact. Requires RECEIVE_SMS — see class doc for
     * the Play Store caveat.
     */
    private fun registerSmsReplyReceiver() {
        smsReplyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
                if (replyWaitJob == null || contacts.isEmpty()) return
                val activeContact = contacts.getOrNull(activeContactIndex) ?: return

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val senderMatches = messages.any { msg ->
                    normalizePhone(msg.originatingAddress ?: "") == normalizePhone(activeContact)
                }
                if (senderMatches) {
                    onReplyReceivedFromActiveContact(activeContact)
                }
            }
        }
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsReplyReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(smsReplyReceiver, filter)
        }
    }

    /** Compares only the last 10 digits so +92xxxxxxxxxx and 0xxxxxxxxxx style numbers still match. */
    private fun normalizePhone(raw: String): String {
        val digitsOnly = raw.filter { it.isDigit() }
        return digitsOnly.takeLast(10)
    }

    private fun stopSharing(reason: String) {
        Log.i(TAG, "Stopping sharing: $reason")
        saveStopState(activeContactIndex)
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        replyWaitJob?.cancel()
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, LocationShareService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sharing live location")
            .setContentText("Texts on movement, escalates if a contact doesn't respond")
            .setSmallIcon(R.drawable.map_to_home_icon)
            .setOngoing(true)
            .addAction(0, "Stop Sharing", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Covers the case where the service is killed without going through
        // ACTION_STOP (e.g. the system reclaiming it) — the resume window
        // should still be measured from whenever sharing actually stopped.
        saveStopState(activeContactIndex)
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        replyWaitJob?.cancel()
        sentResultReceiver?.let { runCatching { unregisterReceiver(it) } }
        smsReplyReceiver?.let { runCatching { unregisterReceiver(it) } }
        _isRunning.value = false
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}