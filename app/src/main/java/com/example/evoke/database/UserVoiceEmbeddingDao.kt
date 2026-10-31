package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UserVoiceEmbeddingDao {

    @Insert
    suspend fun insert(embedding: UserVoiceEmbedding): Long

    @Delete
    suspend fun delete(embedding: UserVoiceEmbedding)

    @Query("""
        SELECT *
        FROM user_voice_embeddings
        ORDER BY quality DESC
    """)
    suspend fun getAllEmbeddings(): List<UserVoiceEmbedding>

    @Query("""
        SELECT *
        FROM user_voice_embeddings
        ORDER BY quality DESC
        LIMIT 3
    """)
    suspend fun getTopThreeEmbeddings(): List<UserVoiceEmbedding>

    @Query("SELECT COUNT(*) FROM user_voice_embeddings")
    suspend fun getEmbeddingCount(): Int

    @Query("DELETE FROM user_voice_embeddings")
    suspend fun deleteAll()

    @Query("""
    SELECT *
    FROM user_voice_embeddings
    ORDER BY quality ASC
    LIMIT 1
""")
    suspend fun getLowestQualityEmbedding(): UserVoiceEmbedding?
}