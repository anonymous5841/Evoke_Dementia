package com.example.evoke.services

import android.Manifest
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
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.evoke.R
import com.example.evoke.data.ContactRepository
import com.example.evoke.data.EmergencyContactState
import com.example.evoke.data.EmergencyState
import com.example.evoke.data.DatabaseContactRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
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
 * Handles the "Emergency Call" header button.
 *
 * Each button press performs exactly ONE action — there is no automatic
 * cascading through multiple contacts within a single press:
 *
 *  - ACTION_TRIGGER (normal press): acts on whichever contact/attempt the
 *    saved state points at. Attempt 1 sends the one-time location SMS then
 *    calls; attempt 2 just calls (no SMS). If not answered: attempt 1 ->
 *    advances to attempt 2 for the SAME contact (next press retries them).
 *    Attempt 2 unanswered -> advances to the NEXT contact, attempt 1 (next
 *    press calls them fresh, with SMS). If answered, state just remembers
 *    that — the NEXT press won't re-trigger this action at all; the UI
 *    should show the "call again / next contact" dialog instead and send
 *    ACTION_CALL_EXISTING or ACTION_MOVE_TO_NEXT.
 *  - ACTION_CALL_EXISTING: re-calls the already-answered active contact, no
 *    SMS. If they don't pick up this time, treated as done with this
 *    contact -> auto-advances to the next one (attempt 1, ready for the next
 *    ACTION_TRIGGER press).
 *  - ACTION_MOVE_TO_NEXT: pure state advance to the next contact (attempt 1,
 *    not answered) — places no call itself. The next ACTION_TRIGGER press
 *    will call that contact fresh.
 *
 * 30+ minutes since the last call placed (any of the three actions) resets
 * everything back to contact 0, attempt 1 on the next ACTION_TRIGGER press.
 *
 * LOCATION/CALL ARE PARALLEL, NOT SEQUENTIAL: the call is the priority, so
 * dialing never waits on a GPS fix. On attempt 1, the location fetch and the
 * "send location SMS" step run concurrently with placing the call — the SMS
 * goes out whenever the fix resolves (or times out), independent of the call.
 */
class EmergencyAlertService : Service() {

    // Emergency contacts saved in the Room database (emergency_contacts table).
    // Lazy so the Service context is attached before it is used.
    private val contactRepository: ContactRepository by lazy { DatabaseContactRepository(applicationContext) }
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val triggerMutex = Mutex()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var telephonyManager: TelephonyManager

    // Correlates SmsManager's sentIntent broadcast back to the coroutine
    // awaiting it, same pattern as LocationShareService.
    private val pendingSmsResults = mutableMapOf<Int, CompletableDeferred<Boolean>>()
    private val smsRequestCodeCounter = AtomicInteger(0)
    private var smsSentResultReceiver: BroadcastReceiver? = null

    companion object {
        const val ACTION_TRIGGER = "com.example.evoke.action.EMERGENCY_TRIGGER"
        const val ACTION_CALL_EXISTING = "com.example.evoke.action.EMERGENCY_CALL_EXISTING"
        const val ACTION_MOVE_TO_NEXT = "com.example.evoke.action.EMERGENCY_MOVE_TO_NEXT"

        const val CHANNEL_ID = "emergency_alert_channel"
        const val NOTIFICATION_ID = 2001
        private const val TAG = "EmergencyAlertService"
        private const val ACTION_EMERGENCY_SMS_SENT = "com.example.evoke.action.EMERGENCY_SMS_SENT_RESULT"
        private const val EXTRA_SMS_REQUEST_CODE = "sms_request_code"

        // Safety cap so a call that never ends (e.g. listener glitch) can't
        // wedge the flow forever.
        private const val CALL_END_TIMEOUT_MS = 90_000L
        private const val SMS_SEND_TIMEOUT_MS = 15_000L

        // A connected call under this length is treated as an automated
        // network response (e.g. "not reachable" announcement) rather than a
        // real answer. Tune if this is too strict/lenient in practice —
        // Android has no better public signal to go on than duration.
        private const val ANSWERED_MIN_DURATION_SECONDS = 5L
        // How long/often to poll the Call Log waiting for this specific
        // call's entry to be written before giving up and assuming no answer.
        private const val CALL_LOG_POLL_ATTEMPTS = 4
        private const val CALL_LOG_POLL_DELAY_MS = 1_500L

        // Location no longer blocks the call, so this can be generous — it
        // only bounds how long we keep trying in the background before
        // giving up on attaching a location to the SMS.
        private const val LOCATION_FETCH_TIMEOUT_MS = 25_000L

        // TEMP TESTING TOGGLE: flip back to true once the call flow is
        // confirmed working, so SMS + call get tested together again.
        private const val SMS_SENDING_ENABLED = true

        // e.g. "sms_sent:<number>", "sms_failed:<number>", "call_failed:<number>",
        // "invalid_contact_number:<raw>", "answered:<number>",
        // "no_answer_retry_same_contact:<number>", "no_answer_moving_to_next:<number>",
        // "moved_to_next_contact", "all_contacts_exhausted", etc.
        private val _lastEvent = MutableStateFlow<String?>(null)
        val lastEvent: StateFlow<String?> = _lastEvent

        private val _isActive = MutableStateFlow(false)
        val isActive: StateFlow<Boolean> = _isActive
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        createNotificationChannel()
        registerSmsSentResultReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_TRIGGER || action == ACTION_CALL_EXISTING || action == ACTION_MOVE_TO_NEXT) {
            startForeground(NOTIFICATION_ID, buildNotification("Emergency alert in progress"))
            serviceScope.launch { handleAction(action) }
        }
        return START_NOT_STICKY
    }

    private suspend fun handleAction(action: String) {
        if (triggerMutex.isLocked) {
            _lastEvent.value = "already_in_progress"
            return
        }
        triggerMutex.withLock {
            _isActive.value = true
            try {
                when (action) {
                    ACTION_TRIGGER -> handleNormalTrigger()
                    ACTION_CALL_EXISTING -> handleCallExisting()
                    ACTION_MOVE_TO_NEXT -> handleMoveToNext()
                }
            } catch (e: Exception) {
                // A crash is the worst possible outcome for an emergency
                // feature — fail loud but stay alive so it's visible as a
                // toast instead of silently killing the process.
                Log.e(TAG, "Emergency action crashed: ${e.javaClass.simpleName}: ${e.message}", e)
                _lastEvent.value = "internal_error:${e.javaClass.simpleName}:${e.message}"
            } finally {
                _isActive.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    /** Normal button press: acts on whatever contact/attempt the saved state points at. */
    private suspend fun handleNormalTrigger() {
        val previous = EmergencyContactState.read(this)
        val now = System.currentTimeMillis()
        val resetNeeded = previous.lastCallAt == 0L || now - previous.lastCallAt > EmergencyContactState.RESET_AFTER_MS
        val state = if (resetNeeded) {
            Log.i(TAG, "30+ min since last call (or first run) — starting at contact 0, attempt 1")
            EmergencyState()
        } else {
            previous
        }

        val contacts = contactRepository.getContactsOrderedByPriority()
        if (contacts.isEmpty()) {
            Log.e(TAG, "No contacts configured")
            _lastEvent.value = "no_contacts_configured"
            return
        }

        // Skip over any obviously-invalid entries (e.g. leftover placeholder
        // numbers) to find the next real contact to act on.
        var index = state.activeContactIndex
        while (index < contacts.size && !looksLikeDialableNumber(contacts[index])) {
            Log.e(TAG, "Contact at index $index (\"${contacts[index]}\") isn't a real number — skipping")
            _lastEvent.value = "invalid_contact_number:${contacts[index]}"
            index++
        }
        if (index >= contacts.size) {
            // Nothing left to try this cycle — the NEXT press should just
            // start fresh from contact 0 rather than sitting stuck reporting
            // "exhausted" forever until 30 minutes pass. The 30-min timer is
            // for resuming a stalled mid-cycle attempt, not for this case —
            // there's nothing to resume once every contact's been tried.
            EmergencyContactState.write(this, EmergencyState())
            Log.w(TAG, "All emergency contacts exhausted — reset to contact 0 for the next press")
            _lastEvent.value = "all_contacts_exhausted"
            return
        }

        val contact = contacts[index]
        val attempt = state.attemptForActiveContact.coerceIn(1, 2)

        // PARALLEL, NOT SEQUENTIAL: kick the location fetch + SMS send off in
        // the background and go straight to dialing. The call must never wait
        // on GPS. `async` starts eagerly, so the fetch is already running by
        // the time we call placeCallAndAwaitOutcome below.
        val smsJob: Deferred<Unit>? = if (attempt == 1) {
            serviceScope.async {
                val location = fetchFreshLocationOrNull()
                sendLocationSmsIfEnabled(contact, location)
            }
        } else {
            Log.i(TAG, "Second attempt for $contact — not resending the location SMS")
            null
        }

        val answeredNow = placeCallAndAwaitOutcome(contact)

        // Let the SMS finish (it's likely already done by the time the call
        // ends, but don't leave it dangling / let the service die mid-send).
        smsJob?.await()

        val now2 = System.currentTimeMillis()

        if (answeredNow) {
            EmergencyContactState.write(this, EmergencyState(lastCallAt = now2, activeContactIndex = index, attemptForActiveContact = attempt, answered = true))
            _lastEvent.value = "answered:$contact"
            return
        }

        if (attempt == 1) {
            EmergencyContactState.write(this, EmergencyState(lastCallAt = now2, activeContactIndex = index, attemptForActiveContact = 2, answered = false))
            _lastEvent.value = "no_answer_retry_same_contact:$contact"
        } else {
            val nextIndex = index + 1
            if (nextIndex >= contacts.size) {
                EmergencyContactState.write(this, EmergencyState())
                _lastEvent.value = "all_contacts_exhausted"
            } else {
                EmergencyContactState.write(this, EmergencyState(lastCallAt = now2, activeContactIndex = nextIndex, attemptForActiveContact = 1, answered = false))
                _lastEvent.value = "no_answer_moving_to_next:$contact"
            }
        }
    }

    /** "Call existing" choice from the answered-contact dialog: re-call the
     *  same contact, no SMS (they've already been texted). */
    private suspend fun handleCallExisting() {
        val state = EmergencyContactState.read(this)
        val contacts = contactRepository.getContactsOrderedByPriority()
        if (state.activeContactIndex >= contacts.size) {
            EmergencyContactState.write(this, EmergencyState())
            _lastEvent.value = "all_contacts_exhausted"
            return
        }
        val contact = contacts[state.activeContactIndex]
        if (!looksLikeDialableNumber(contact)) {
            _lastEvent.value = "invalid_contact_number:$contact"
            return
        }

        val answeredNow = placeCallAndAwaitOutcome(contact)
        val now2 = System.currentTimeMillis()

        if (answeredNow) {
            EmergencyContactState.write(this, state.copy(lastCallAt = now2, answered = true))
            _lastEvent.value = "answered:$contact"
        } else {
            // They answered before but not on this recall — treat this
            // contact as done and move on, ready for the next normal press.
            val nextIndex = state.activeContactIndex + 1
            if (nextIndex >= contacts.size) {
                EmergencyContactState.write(this, EmergencyState())
                _lastEvent.value = "all_contacts_exhausted"
            } else {
                EmergencyContactState.write(this, EmergencyState(lastCallAt = now2, activeContactIndex = nextIndex, attemptForActiveContact = 1, answered = false))
                _lastEvent.value = "no_answer_moving_to_next:$contact"
            }
        }
    }

    /** "Next contact" choice from the answered-contact dialog: pure state
     *  advance, places no call. The next normal press calls the new contact. */
    private suspend fun handleMoveToNext() {
        val state = EmergencyContactState.read(this)
        val nextIndex = state.activeContactIndex + 1
        val contacts = contactRepository.getContactsOrderedByPriority()
        if (nextIndex >= contacts.size) {
            EmergencyContactState.write(this, EmergencyState())
            _lastEvent.value = "all_contacts_exhausted"
        } else {
            // Not a call, so lastCallAt is intentionally left untouched — the
            // 30-min reset window is measured from the last actual call.
            EmergencyContactState.write(this, state.copy(activeContactIndex = nextIndex, attemptForActiveContact = 1, answered = false))
            _lastEvent.value = "moved_to_next_contact"
        }
    }

    /** Rejects obvious placeholders (e.g. "03xxxxxxxxx") and anything too
     *  short to be a real number. */
    private fun looksLikeDialableNumber(raw: String): Boolean {
        val digits = raw.filter { it.isDigit() }
        return digits.length >= 7 && raw.none { it.lowercaseChar() == 'x' }
    }

    private suspend fun sendLocationSmsIfEnabled(contact: String, location: Location?) {
        if (!SMS_SENDING_ENABLED) {
            Log.i(TAG, "SMS sending disabled for testing — skipping text to $contact")
            return
        }
        sendLocationSmsAndConfirm(contact, location)
    }

    /** Sends the SMS and suspends until the OS reports send success/failure
     *  via the sentIntent broadcast — mirrors LocationShareService's
     *  trySendSms/sendViaManager so a real failure (bad number, no signal,
     *  radio off, etc.) is caught instead of assumed to have worked. */
    private suspend fun sendLocationSmsAndConfirm(contact: String, location: Location?) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SEND_SMS not granted — cannot text $contact")
            _lastEvent.value = "no_sms_permission"
            return
        }
        val body = if (location != null) {
            "Emergency alert triggered.\nMy location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            "Emergency alert triggered. Location unavailable right now."
        }

        val manager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
        if (manager == null) {
            Log.e(TAG, "No SmsManager available on this device")
            _lastEvent.value = "sms_failed:$contact"
            return
        }

        val requestCode = smsRequestCodeCounter.incrementAndGet()
        val deferred = CompletableDeferred<Boolean>()
        pendingSmsResults[requestCode] = deferred

        val sentPendingIntent = PendingIntent.getBroadcast(
            this, requestCode,
            Intent(ACTION_EMERGENCY_SMS_SENT).apply {
                setPackage(packageName)
                putExtra(EXTRA_SMS_REQUEST_CODE, requestCode)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            manager.sendTextMessage(contact, null, body, sentPendingIntent, null)
        } catch (e: Exception) {
            Log.e(TAG, "sendTextMessage threw for $contact: ${e.message}", e)
            pendingSmsResults.remove(requestCode)
            _lastEvent.value = "sms_failed:$contact"
            return
        }

        val sent = try {
            withContext(Dispatchers.Default) {
                withTimeoutOrNull(SMS_SEND_TIMEOUT_MS) { deferred.await() } ?: run {
                    Log.w(TAG, "No SMS send-result broadcast within ${SMS_SEND_TIMEOUT_MS}ms for $contact — treating as failed")
                    false
                }
            }
        } finally {
            pendingSmsResults.remove(requestCode)
        }

        if (sent) {
            Log.i(TAG, "Confirmed: location SMS delivered to radio for $contact")
            _lastEvent.value = "sms_sent:$contact"
        } else {
            Log.e(TAG, "Confirmed FAILURE sending location SMS to $contact")
            _lastEvent.value = "sms_failed:$contact"
        }
    }

    private fun registerSmsSentResultReceiver() {
        smsSentResultReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val requestCode = intent.getIntExtra(EXTRA_SMS_REQUEST_CODE, -1)
                val deferred = pendingSmsResults[requestCode] ?: return
                val success = resultCode == android.app.Activity.RESULT_OK
                if (!deferred.isCompleted) deferred.complete(success)
            }
        }
        val filter = IntentFilter(ACTION_EMERGENCY_SMS_SENT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(smsSentResultReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(smsSentResultReceiver, filter)
        }
    }

    /**
     * Fetches a fresh fix, capped at [timeoutMs]. This no longer blocks the
     * call — it runs in a background coroutine started alongside dialing, so
     * a slow/cold GPS fix delays only the SMS, never the call itself.
     */
    private suspend fun fetchFreshLocationOrNull(timeoutMs: Long = LOCATION_FETCH_TIMEOUT_MS): Location? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "ACCESS_FINE_LOCATION not granted — skipping location, sending call/SMS without it")
            return null
        }
        return try {
            val location = withTimeoutOrNull(timeoutMs) {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            }
            when {
                location == null -> Log.w(TAG, "No location fix within ${timeoutMs}ms (timed out, or the Task completed with a null result — check that location is turned on for this device and that the service's foregroundServiceType includes \"location\")")
                else -> Log.i(TAG, "Got location fix: ${location.latitude}, ${location.longitude} (accuracy ${location.accuracy}m)")
            }
            location
        } catch (e: Exception) {
            Log.e(TAG, "Location fetch threw: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    /**
     * Places the call and waits for it to end, then checks the Call Log to
     * decide whether it was answered.
     *
     * IMPORTANT LIMITATION: Android does not give normal (non-default-dialer)
     * apps a live "the other person just picked up" signal for an outgoing
     * call. TelephonyManager only reports IDLE / OFFHOOK / RINGING(incoming),
     * and OFFHOOK covers dialing, ringing-on-their-end, AND connected, all as
     * one state — there is no public API to split those apart in real time.
     * So this waits for the call to fully end (state returns to IDLE), then
     * reads that call's duration from the Call Log: duration > 0 means they
     * picked up, duration == 0 means it rang out / was rejected / never
     * connected. That check needs READ_CALL_LOG.
     */
    private suspend fun placeCallAndAwaitOutcome(contact: String): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "CALL_PHONE not granted — cannot call $contact")
            _lastEvent.value = "no_call_permission"
            return false
        }

        val canReadState = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val callEnded = CompletableDeferred<Unit>()
        var reachedOffHook = false

        // PhoneStateListener's constructor grabs Looper.myLooper() internally
        // to build its callback Handler. Dispatchers.IO threads have no
        // prepared Looper, so building it there throws a NullPointerException
        // reading Looper.mQueue. The main thread has a Looper, so build and
        // register it there.
        val listener = withContext(Dispatchers.Main) {
            object : PhoneStateListener() {
                @Deprecated("Deprecated in Java", ReplaceWith(""))
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    Log.i(TAG, "Telephony state for $contact -> $state (0=idle,1=ringing,2=offhook)")
                    when (state) {
                        TelephonyManager.CALL_STATE_OFFHOOK -> reachedOffHook = true
                        TelephonyManager.CALL_STATE_IDLE -> if (reachedOffHook && !callEnded.isCompleted) callEnded.complete(Unit)
                    }
                }
            }
        }

        // Tracks whether registration actually succeeded, so the finally
        // block only attempts to unregister a listener that's really
        // registered, and so a SecurityException thrown at listen()-time
        // (permission revoked between the check above and this call) is
        // treated the same as "no phone state permission" rather than
        // crashing the whole emergency flow.
        var listenerRegistered = false

        if (canReadState) {
            withContext(Dispatchers.Main) {
                try {
                    @Suppress("DEPRECATION")
                    telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                    listenerRegistered = true
                } catch (e: SecurityException) {
                    Log.e(TAG, "listen(LISTEN_CALL_STATE) threw SecurityException for $contact: ${e.message}", e)
                }
            }
        }

        val callStartAtMillis = System.currentTimeMillis()
        try {
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$contact")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(callIntent)
                Log.i(TAG, "Dial intent fired for $contact")
            } catch (e: Exception) {
                Log.e(TAG, "startActivity(ACTION_CALL) threw for $contact: ${e.message}", e)
                _lastEvent.value = "call_failed:$contact"
                return false
            }

            if (!listenerRegistered) {
                delay(30_000L)
                _lastEvent.value = "no_phone_state_permission_assuming_no_answer"
                return false
            }

            val completedNormally = withTimeoutOrNull(CALL_END_TIMEOUT_MS) { callEnded.await() }
            if (completedNormally == null) {
                Log.w(TAG, "Call to $contact never reported ending within ${CALL_END_TIMEOUT_MS}ms")
            }
        } finally {
            if (listenerRegistered) {
                withContext(Dispatchers.Main) {
                    try {
                        @Suppress("DEPRECATION")
                        telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "listen(LISTEN_NONE) threw SecurityException for $contact: ${e.message}", e)
                    }
                }
            }
        }

        return wasLastCallToNumberAnswered(contact, callStartAtMillis)
    }

    /**
     * Reads the Call Log entry for THIS specific call and treats it as
     * answered only if its duration clears a minimum threshold.
     *
     * Two things this guards against that a naive "most recent entry,
     * duration > 0" check does not:
     *
     *  1. Race condition: the Call Log provider doesn't always have the new
     *     entry written the instant the call ends. Querying immediately can
     *     return an OLDER entry for the same number (e.g. from a previous
     *     test call that really was answered), wrongly reporting "answered"
     *     for a call that never connected. Fixed by requiring the entry's
     *     DATE to be at/after this call's own start time, and polling briefly
     *     if nothing matching has shown up yet.
     *  2. Automated network responses: an unreachable/off number is often
     *     routed to voicemail or a short "not reachable" announcement by the
     *     carrier — from the phone's perspective that's a real, non-zero-
     *     duration "connection," indistinguishable from a human answering
     *     using only public Android APIs. Requiring a minimum duration
     *     filters out short announcements, but genuinely CANNOT distinguish
     *     "picked up and immediately hung up" from "went to voicemail" —
     *     that's a hard platform limitation, not something fixable here
     *     without being the default dialer / using call-screening APIs.
     *
     * On any ambiguity (permission missing, entry never appears, etc.) this
     * defaults to false (not answered) rather than true — a false "try
     * again" is far safer than a false "we're covered" for an emergency flow.
     */
    private suspend fun wasLastCallToNumberAnswered(contact: String, callStartAtMillis: Long): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "READ_CALL_LOG not granted — cannot verify whether $contact answered")
            _lastEvent.value = "no_call_log_permission_assuming_no_answer"
            return false
        }

        val targetDigits = contact.filter { it.isDigit() }.takeLast(10)
        // Small negative tolerance: the call log's DATE is when the call
        // started, which can be a second or two before/after our own
        // callStartAtMillis depending on OS/telephony timing.
        val earliestAcceptableDate = callStartAtMillis - 5_000L

        repeat(CALL_LOG_POLL_ATTEMPTS) { attemptNumber ->
            val matchedDuration = queryCallLogDurationForThisCall(targetDigits, earliestAcceptableDate)
            if (matchedDuration != null) {
                val answered = matchedDuration >= ANSWERED_MIN_DURATION_SECONDS
                Log.i(TAG, "Call log match for $contact: duration=${matchedDuration}s -> answered=$answered")
                return answered
            }
            Log.i(TAG, "No fresh call log entry for $contact yet (attempt ${attemptNumber + 1}/$CALL_LOG_POLL_ATTEMPTS) — waiting")
            delay(CALL_LOG_POLL_DELAY_MS)
        }

        Log.w(TAG, "No call log entry for $contact appeared after polling — defaulting to not answered")
        return false
    }

    /** Returns the duration (seconds) of the newest Call Log entry for these
     *  digits dated at/after [earliestAcceptableDate], or null if none exists yet. */
    private fun queryCallLogDurationForThisCall(targetDigits: String, earliestAcceptableDate: Long): Long? {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DURATION, CallLog.Calls.DATE)
        return try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI, projection, null, null, "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val date = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE))
                    if (date < earliestAcceptableDate) {
                        // Sorted DESC — once we hit an entry older than this
                        // call could possibly be, there's nothing newer left.
                        return null
                    }
                    val number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: continue
                    val numberDigits = number.filter { it.isDigit() }.takeLast(10)
                    if (numberDigits == targetDigits) {
                        return cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                    }
                }
                null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Call log read threw SecurityException: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Call log read failed: ${e.message}")
            null
        }
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emergency alert")
            .setContentText(text)
            .setSmallIcon(R.drawable.map_to_home_icon)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Emergency Alerts", NotificationManager.IMPORTANCE_HIGH)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        smsSentResultReceiver?.let { runCatching { unregisterReceiver(it) } }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}