package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ConversationDao {

    @Insert
    suspend fun insert(conversation: Conversation): Long

    @Update
    suspend fun update(conversation: Conversation)

    @Query("""
        SELECT *
        FROM conversations
        WHERE conversationId = :conversationId
        LIMIT 1
    """)
    suspend fun getById(conversationId: Long): Conversation?

    @Query("""
        SELECT *
        FROM conversations
        WHERE recognitionId = :recognitionId
        LIMIT 1
    """)
    suspend fun getByRecognitionId(recognitionId: Long): Conversation?

    @Query("""
        SELECT *
        FROM conversations
        INNER JOIN recognition_events
            ON conversations.recognitionId = recognition_events.recognitionId
        WHERE recognition_events.dateTime = :dateTime
        LIMIT 1
    """)
    suspend fun getByDateTime(
        dateTime: Long
    ): Conversation?

    @Query("""
        UPDATE conversations
        SET summary = :summary
        WHERE conversationId = :conversationId
    """)
    suspend fun updateSummary(
        conversationId: Long,
        summary: String
    )
}