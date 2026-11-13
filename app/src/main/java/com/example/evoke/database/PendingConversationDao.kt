package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingConversationDao {

    // Creates a pending entry for a newly recorded conversation.
    @Insert
    suspend fun insert(
        pendingConversation: PendingConversation
    ): Long

    // Fetches ONLY the oldest pending conversation.
    // The worker will process this one before moving to the next.
    @Query("""
        SELECT *
        FROM pending_conversations
        ORDER BY pendingId ASC
        LIMIT 1
    """)
    suspend fun getOldestPending(): PendingConversation?

    // Counts all outstanding conversations.
    // The repository will use this to enforce the maximum of five.
    @Query("""
        SELECT COUNT(*)
        FROM pending_conversations
    """)
    suspend fun getPendingCount(): Int

    // Moves a pending conversation to the summarization stage.
    @Query("""
    UPDATE pending_conversations
    SET status = 'WAITING_SUMMARY'
    WHERE pendingId = :pendingId
""")
    suspend fun markWaitingForSummary(pendingId: Long): Int

    // Moves a pending conversation to the summarization stage by conversation id.
    @Query("""
    UPDATE pending_conversations
    SET status = 'WAITING_SUMMARY'
    WHERE conversationId = :conversationId
""")
    suspend fun markWaitingForSummaryByConversationId(
        conversationId: Long
    ): Int

    // Moves a pending conversation back to the transcription stage, if needed.
    @Query("""
    UPDATE pending_conversations
    SET status = 'WAITING_TRANSCRIPTION'
    WHERE pendingId = :pendingId
""")
    suspend fun markWaitingForTranscription(pendingId: Long): Int

    // Clears the audio path after the audio file has been deleted.
    @Query("""
        UPDATE pending_conversations
        SET audioPath = NULL
        WHERE pendingId = :pendingId
    """)
    suspend fun clearAudioPath(
        pendingId: Long
    ): Int

    // Deletes a completed pending entry.
    @Query("""
        DELETE FROM pending_conversations
        WHERE pendingId = :pendingId
    """)
    suspend fun deleteById(
        pendingId: Long
    ): Int

    // Deletes pending entries associated with a conversation.
    @Query("""
        DELETE FROM pending_conversations
        WHERE conversationId = :conversationId
    """)
    suspend fun deleteByConversationId(
        conversationId: Long
    )

    // Checks whether a conversation is still waiting for processing.
    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM pending_conversations
            WHERE conversationId = :conversationId
        )
    """)
    suspend fun isPending(
        conversationId: Long
    ): Boolean

    @Query("SELECT audioPath FROM pending_conversations WHERE audioPath IS NOT NULL")
    suspend fun getAllAudioPaths(): List<String>

    @Query("""
    SELECT pc.audioPath FROM pending_conversations pc
    INNER JOIN conversations c ON pc.conversationId = c.conversationId
    WHERE c.recognitionId = :recognitionId AND pc.audioPath IS NOT NULL
""")
    suspend fun getAudioPathsForRecognition(recognitionId: Long): List<String>

    @Query("""
    SELECT pc.audioPath FROM pending_conversations pc
    INNER JOIN conversations c ON pc.conversationId = c.conversationId
    INNER JOIN recognition_events re ON c.recognitionId = re.recognitionId
    WHERE re.personId = :personId AND pc.audioPath IS NOT NULL
""")
    suspend fun getAudioPathsForPerson(personId: Long): List<String>
}