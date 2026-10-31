package com.example.evoke.database.repository

import androidx.room.withTransaction
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.EmergencyContact
import com.example.evoke.database.User
import com.example.evoke.database.UserVoiceEmbedding
import com.example.evoke.database.UserWithDetails

class UserRepository(
    private val db: AppDatabase
) {

    private val userDao = db.userDao()
    private val emergencyContactDao = db.emergencyContactDao()
    private val userVoiceEmbeddingDao = db.userVoiceEmbeddingDao()

    private fun validateEmergencyContacts(
        emergencyContacts: List<EmergencyContact>
    ) {
        if (emergencyContacts.size !in 1..5) {
            throw IllegalArgumentException(
                "Emergency contacts must contain between 1 and 5 contacts."
            )
        }

        val priorities = emergencyContacts.map { it.priority }

        if (priorities.any { it !in 1..5 }) {
            throw IllegalArgumentException(
                "Emergency contact priority must be between 1 and 5."
            )
        }

        if (priorities.size != priorities.distinct().size) {
            throw IllegalArgumentException(
                "Emergency contact priorities must be unique."
            )
        }
    }

    suspend fun createUser(
        user: User,
        emergencyContacts: List<EmergencyContact>,
        voiceEmbeddings: List<UserVoiceEmbedding>
    ): Long {

        if (voiceEmbeddings.isEmpty()) {
            throw IllegalArgumentException(
                "At least one voice sample is required."
            )
        }

        if (voiceEmbeddings.size > 3) {
            throw IllegalArgumentException(
                "A maximum of 3 voice embeddings are allowed."
            )
        }

        validateEmergencyContacts(emergencyContacts)

        return db.withTransaction {

            val userId = userDao.insert(user)

            emergencyContacts.forEach { contact ->
                emergencyContactDao.insert(contact)
            }

            voiceEmbeddings.forEach { embedding ->
                userVoiceEmbeddingDao.insert(embedding)
            }

            userId
        }
    }
    suspend fun getUser(): UserWithDetails? {

        val user = userDao.getUser() ?: return null

        val emergencyContacts =
            emergencyContactDao.getAllOrderedByPriority()

        return UserWithDetails(
            user = user,
            emergencyContacts = emergencyContacts
        )
    }

    suspend fun updateUser(
        user: User,
        emergencyContacts: List<EmergencyContact>
    ) {
        validateEmergencyContacts(emergencyContacts)

        db.withTransaction {

            // Update user details
            userDao.update(user)

            // Replace emergency contacts
            emergencyContactDao.deleteAll()

            emergencyContacts.forEach { contact ->
                emergencyContactDao.insert(contact)
            }
        }
    }
    suspend fun deleteUserAndResetDatabase() {
        db.withTransaction {
            db.clearAllTables()
        }
    }
    suspend fun addVoiceEmbedding(
        embedding: UserVoiceEmbedding
    ): Long {

        val embeddings =
            userVoiceEmbeddingDao.getAllEmbeddings()

        // Less than 3 → simply add the new embedding
        if (embeddings.size < 3) {
            return userVoiceEmbeddingDao.insert(embedding)
        }

        // Already have 3 → find the weakest
        val lowestQuality =
            userVoiceEmbeddingDao.getLowestQualityEmbedding()
                ?: return userVoiceEmbeddingDao.insert(embedding)

        // New embedding isn't better → don't store it
        if (embedding.quality <= lowestQuality.quality) {
            return lowestQuality.userVoiceEmbeddingId
        }

        // New embedding is better → replace weakest
        return db.withTransaction {

            userVoiceEmbeddingDao.delete(lowestQuality)

            userVoiceEmbeddingDao.insert(embedding)
        }
    }


//    I'd add a function to explicitly replace all user voice embeddings
//
//    This is useful if the user goes into settings and says something like "Re-register my voice":

    suspend fun replaceVoiceEmbeddings(
        embeddings: List<UserVoiceEmbedding>
    ) {
        if (embeddings.isEmpty()) {
            throw IllegalArgumentException(
                "At least one voice embedding is required."
            )
        }

        if (embeddings.size > 3) {
            throw IllegalArgumentException(
                "A maximum of 3 voice embeddings are allowed."
            )
        }

        db.withTransaction {

            userVoiceEmbeddingDao.deleteAll()

            embeddings.forEach { embedding ->
                userVoiceEmbeddingDao.insert(embedding)
            }
        }
    }
}