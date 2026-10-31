package com.example.evoke.database.repository

import com.example.evoke.database.AppDatabase
import com.example.evoke.database.EmergencyContact

class EmergencyContactRepository(
    private val db: AppDatabase
) {

    private val dao = db.emergencyContactDao()

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