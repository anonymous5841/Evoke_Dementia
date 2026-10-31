package com.example.evoke.database.repository

import androidx.room.withTransaction
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.Conversation
import com.example.evoke.database.PendingConversation
import com.example.evoke.database.PersonHistory
import com.example.evoke.database.RecognitionEvent

class RecognitionRepository(
    private val db: AppDatabase
) {

    private val recognitionDao = db.recognitionEventDao()
    private val historyDao = db.personHistoryDao()
    private val conversationDao = db.conversationDao()
    private val pendingDao = db.pendingConversationDao()

    private val personDao = db.personDao()

    suspend fun createRecognition(
        personId: Long,
        dateTime: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        rawConversation: String? = null
    ): Long {

        return db.withTransaction {
            createRecognitionInternal(
                personId = personId,
                dateTime = dateTime,
                latitude = latitude,
                longitude = longitude,
                rawConversation = rawConversation
            )
        }
    }

    internal suspend fun createRecognitionInternal(
        personId: Long,
        dateTime: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        rawConversation: String? = null
    ): Long {

        require(
            (latitude != null && longitude != null) ||
                    !rawConversation.isNullOrBlank()
        ) {
            "Recognition must contain either location or conversation"
        }

        // Conversation requires voice to be enabled
        if (!rawConversation.isNullOrBlank()) {

            val voiceEnabled =
                personDao.isVoiceEnabled(personId)
                    ?: throw IllegalArgumentException(
                        "Person does not exist."
                    )

            require(voiceEnabled) {
                "Conversation cannot be recorded because voice is not enabled for this person."
            }
        }

        val recognitionId = recognitionDao.insert(
            RecognitionEvent(
                personId = personId,
                dateTime = dateTime
            )
        )

        if (latitude != null && longitude != null) {
            historyDao.insert(
                PersonHistory(
                    recognitionId = recognitionId,
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }

        if (!rawConversation.isNullOrBlank()) {

            val conversationId = conversationDao.insert(
                Conversation(
                    recognitionId = recognitionId,
                    summary = rawConversation
                )
            )

            pendingDao.insert(
                PendingConversation(
                    conversationId = conversationId
                )
            )
        }

        return recognitionId
    }


    suspend fun markConversationSummarized(
        conversationId: Long,
        summary: String
    ) {

        db.withTransaction {

            conversationDao.updateSummary(
                conversationId,
                summary
            )

            pendingDao.deleteByConversationId(
                conversationId
            )
        }
    }

    suspend fun getPendingConversationCount(): Int {
        return pendingDao.getPendingCount()
    }

    suspend fun deleteRecognition(
        recognitionId: Long
    ) {
        recognitionDao.deleteById(recognitionId)
    }
}