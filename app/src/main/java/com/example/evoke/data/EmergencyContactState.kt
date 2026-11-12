package com.example.evoke.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.emergencyStateDataStore by preferencesDataStore(name = "emergency_alert_state")

/**
 * Tracks escalation progress across emergency-button presses. Each press
 * performs exactly ONE action (one call, or one dialog choice) — there is no
 * automatic multi-contact cascade within a single press.
 *
 *  - lastCallAt              : when a call was last actually placed. If more
 *                              than RESET_AFTER_MS has passed since this, the
 *                              NEXT press starts the whole cycle over from
 *                              contact 0, attempt 1.
 *  - activeContactIndex      : which contact (0-based) is currently being
 *                              tried.
 *  - attemptForActiveContact : 1 or 2. Each contact gets up to two call
 *                              attempts. The one-time location SMS is only
 *                              sent on attempt 1 for that contact, never on
 *                              attempt 2.
 *  - answered                : true once the active contact has picked up
 *                              and hasn't been moved on from yet. While true,
 *                              the next button press shows a choice dialog
 *                              ("call them again" / "move to next contact")
 *                              instead of the normal attempt flow.
 */
data class EmergencyState(
    val lastCallAt: Long = 0L,
    val activeContactIndex: Int = 0,
    val attemptForActiveContact: Int = 1,
    val answered: Boolean = false
)

object EmergencyContactState {

    private val LAST_CALL_AT = longPreferencesKey("last_call_at")
    private val ACTIVE_CONTACT_INDEX = intPreferencesKey("active_contact_index")
    private val ATTEMPT_FOR_ACTIVE_CONTACT = intPreferencesKey("attempt_for_active_contact")
    private val ANSWERED = booleanPreferencesKey("answered")

    const val RESET_AFTER_MS = 30 * 60 * 1000L // 30 min since the last call -> start over from contact 0

    fun flow(context: Context): Flow<EmergencyState> =
        context.emergencyStateDataStore.data.map { prefs ->
            EmergencyState(
                lastCallAt = prefs[LAST_CALL_AT] ?: 0L,
                activeContactIndex = prefs[ACTIVE_CONTACT_INDEX] ?: 0,
                attemptForActiveContact = prefs[ATTEMPT_FOR_ACTIVE_CONTACT] ?: 1,
                answered = prefs[ANSWERED] ?: false
            )
        }

    suspend fun read(context: Context): EmergencyState = flow(context).first()

    suspend fun write(context: Context, state: EmergencyState) {
        context.emergencyStateDataStore.edit { prefs ->
            prefs[LAST_CALL_AT] = state.lastCallAt
            prefs[ACTIVE_CONTACT_INDEX] = state.activeContactIndex
            prefs[ATTEMPT_FOR_ACTIVE_CONTACT] = state.attemptForActiveContact
            prefs[ANSWERED] = state.answered
        }
    }
}
