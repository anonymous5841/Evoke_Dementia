package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface VoiceEmbeddingDao {

    @Insert
    suspend fun insert(embedding: VoiceEmbedding): Long

    @Delete
    suspend fun delete(embedding: VoiceEmbedding)

    @Query("""
        SELECT * 
        FROM voice_embeddings 
        WHERE personId = :personId
        ORDER BY quality DESC
    """)
    suspend fun getEmbeddingsForPerson(personId: Long): List<VoiceEmbedding>

    @Query("""
        SELECT * 
        FROM voice_embeddings 
        WHERE personId = :personId
        ORDER BY quality DESC
        LIMIT 3
    """)
    suspend fun getTopThreeEmbeddings(personId: Long): List<VoiceEmbedding>

    @Query("SELECT COUNT(*) FROM voice_embeddings WHERE personId = :personId")
    suspend fun getEmbeddingCount(personId: Long): Int

    @Query("DELETE FROM voice_embeddings WHERE personId = :personId")
    suspend fun deleteForPerson(personId: Long)

    @Query("""
        SELECT *
        FROM voice_embeddings
    """)
    suspend fun getAllEmbeddings(): List<VoiceEmbedding>

    @Query("""
    SELECT *
    FROM voice_embeddings
    WHERE personId = :personId
    ORDER BY quality ASC
    LIMIT 1
""")
    suspend fun getLowestQualityEmbedding(
        personId: Long
    ): VoiceEmbedding?
}