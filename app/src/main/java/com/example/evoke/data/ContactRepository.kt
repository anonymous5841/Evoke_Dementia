package com.example.evoke.data

import android.content.Context
import com.example.evoke.database.AppDatabase

/**
 * Abstraction over "where the emergency contacts come from" so the escalation
 * logic in EmergencyAlertService / LocationShareService never needs to know
 * it's SQLite (or anything else) underneath.
 *
 * Contacts are just phone numbers, ordered by importance — index 0 is the
 * most important / tried first, up to 5 total.
 */
interface ContactRepository {
    /** Returns 1–5 phone numbers, ordered most-important-first (index 0 tried first). */
    suspend fun getContactsOrderedByPriority(): List<String>
}

/**
 * Reads the emergency contacts saved in the encrypted Room database
 * (`emergency_contacts` table, ordered by `priority` ascending).
 *
 * The numbers are written there by the "Let us know you" / Profile form via
 * EmergencyContactRepository.replaceContacts().
 */
class DatabaseContactRepository(
    private val context: Context
) : ContactRepository {

    override suspend fun getContactsOrderedByPriority(): List<String> =
        AppDatabase.getDatabase(context.applicationContext)
            .emergencyContactDao()
            .getAllOrderedByPriority()
            .map { it.phoneNo.trim() }
            .filter { it.isNotEmpty() }
            .take(5)
}
