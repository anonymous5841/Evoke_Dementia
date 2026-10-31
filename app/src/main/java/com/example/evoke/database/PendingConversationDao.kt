package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingConversationDao {

    @Insert
    suspend fun insert(pendingConversation: PendingConversation): Long

    @Query("""
        SELECT *
        FROM pending_conversations
        ORDER BY pendingId ASC
        LIMIT 5
    """)
    suspend fun getPending(): List<PendingConversation>

    @Query("SELECT COUNT(*) FROM pending_conversations")
    suspend fun getPendingCount(): Int

    @Query("""
        DELETE FROM pending_conversations
        WHERE pendingId = :pendingId
    """)
    suspend fun deleteById(pendingId: Long)

    @Query("""
        DELETE FROM pending_conversations
        WHERE conversationId = :conversationId
    """)
    suspend fun deleteByConversationId(conversationId: Long)

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM pending_conversations
            WHERE conversationId = :conversationId
        )
    """)
    suspend fun isPending(conversationId: Long): Boolean
}