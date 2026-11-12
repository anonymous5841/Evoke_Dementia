package com.example.evoke.database.repository

import androidx.room.withTransaction
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.EmergencyContact

class EmergencyContactRepository(
    private val db: AppDatabase
) {

    private val dao = db.emergencyContactDao()

    /**
     * Replaces every saved emergency contact with [phoneNumbers], in order.
     *
     * Blank entries are skipped; the remaining numbers get priority 1..n in
     * the order given (first field = priority 1 = called / texted first).
     * At most 5 are kept. Used by the "Let us know you" / Profile form, whose
     * numbers EmergencyAlertService and LocationShareService read.
     */
    suspend fun replaceContacts(
        phoneNumbers: List<String>
    ) {
        val cleaned = phoneNumbers
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(5)

        db.withTransaction {
            dao.deleteAll()

            cleaned.forEachIndexed { index, phoneNo ->
                dao.insert(
                    EmergencyContact(
                        phoneNo = phoneNo,
                        priority = index + 1
                    )
                )
            }
        }
    }

    suspend fun addContact(
        contact: EmergencyContact
    ): Long {

        val count = dao.getContactCount()

        if (count >= 5) {
            throw IllegalStateException(
                "Maximum of 5 emergency contacts allowed."
            )
        }

        if (contact.priority !in 1..5) {
            throw IllegalArgumentException(
                "Emergency contact priority must be between 1 and 5."
            )
        }

        if (dao.getByPriority(contact.priority) != null) {
            throw IllegalArgumentException(
                "This priority is already assigned."
            )
        }

        return dao.insert(contact)
    }

    suspend fun getContacts(): List<EmergencyContact> {
        return dao.getAllOrderedByPriority()
    }

    suspend fun deleteContact(
        contact: EmergencyContact
    ) {
        val count = dao.getContactCount()

        if (count <= 1) {
            throw IllegalStateException(
                "At least 1 emergency contact is required."
            )
        }

        dao.delete(contact)
    }

    suspend fun getContactCount(): Int {
        return dao.getContactCount()
    }
}