package com.example.evoke.location

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [SHARED LOCATION INBOX]
 * Holds text that another app (Google Maps) shared to Evoke with "Share -> Evoke",
 * until the Add Location screen reads it.
 *
 * MainActivity puts the text here (onCreate / onNewIntent); AddLocationContent
 * takes it with [consume] and turns it into a location.
 */
object SharedLocationInbox {

    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    /** Stores the shared text if [intent] is a "share text" intent. Returns true if it was. */
    fun offer(intent: Intent?): Boolean {
        if (intent?.action != Intent.ACTION_SEND) return false
        if (intent.type?.startsWith("text/") != true) return false
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (text.isNullOrEmpty()) return false
        _pending.value = text
        return true
    }

    /** Returns the waiting shared text (once) and clears it. */
    fun consume(): String? {
        val text = _pending.value
        _pending.value = null
        return text
    }
}
