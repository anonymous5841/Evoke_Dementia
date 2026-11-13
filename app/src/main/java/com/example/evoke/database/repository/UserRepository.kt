package com.example.evoke.database.repository

import androidx.room.withTransaction
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.EmergencyContact
import com.example.evoke.database.User
import com.example.evoke.database.UserVoiceEmbedding
import com.example.evoke.database.UserWithDetails
import com.example.evoke.storage.AudioStorage
import com.example.evoke.storage.PersonImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(
    private val db: AppDatabase,
    private val audioStorage: AudioStorage,
    private val imageStorage: PersonImageStorage
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

            check(userDao.getUser() == null) { "A user already exists." }

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
            check(userDao.update(user) == 1) { "User does not exist." }

            // Replace emergency contacts
            emergencyContactDao.deleteAll()

            emergencyContacts.forEach { contact ->
                emergencyContactDao.insert(contact)
            }
        }
    }
    /**
     * Wipes every table AND the files that belong to them (profile images
     * and conversation audio).
     *
     * clearAllTables() must not run inside withTransaction (it runs a
     * VACUUM) and must not run on the main thread, so it is called directly
     * on the IO dispatcher.
     *
     * The database key is NOT removed here. To also remove the key and the
     * database file, use AppDataManager.clearAllLocalData().
     */
    suspend fun deleteUserAndResetDatabase() {
        withContext(Dispatchers.IO) {
            db.clearAllTables()
            audioStorage.deleteAllAudio()
            imageStorage.deleteAllProfileImages()
        }
    }

    suspend fun addVoiceEmbedding(
        embedding: UserVoiceEmbedding
    ): Long {

        return db.withTransaction {

            // Retrieve the currently stored user voice embeddings.
            val embeddings =
                userVoiceEmbeddingDao.getAllEmbeddings()

            // If fewer than 3 exist, insert the new embedding.
            if (embeddings.size < 3) {
                return@withTransaction userVoiceEmbeddingDao.insert(embedding)
            }

            // Three already exist, so find the weakest one.
            val lowestQuality =
                userVoiceEmbeddingDao.getLowestQualityEmbedding()
                    ?: throw IllegalStateException(
                        "Could not find the lowest-quality user voice embedding."
                    )

            // Keep the existing embedding if the new one isn't better.
            if (embedding.quality <= lowestQuality.quality) {
                return@withTransaction lowestQuality.userVoiceEmbeddingId
            }

            // Replace the weakest embedding with the better one.
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
