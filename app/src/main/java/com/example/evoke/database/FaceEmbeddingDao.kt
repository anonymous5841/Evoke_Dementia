package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface FaceEmbeddingDao {

    @Insert
    suspend fun insert(embedding: FaceEmbedding): Long

    @Delete
    suspend fun delete(embedding: FaceEmbedding)

    @Query("""
        SELECT * 
        FROM face_embeddings 
        WHERE personId = :personId
        ORDER BY quality DESC
    """)
    suspend fun getEmbeddingsForPerson(personId: Long): List<FaceEmbedding>

    @Query("""
        SELECT * 
        FROM face_embeddings 
        WHERE personId = :personId
        ORDER BY quality DESC
        LIMIT 3
    """)
    suspend fun getTopThreeEmbeddings(personId: Long): List<FaceEmbedding>

    @Query("SELECT COUNT(*) FROM face_embeddings WHERE personId = :personId")
    suspend fun getEmbeddingCount(personId: Long): Int

    @Query("DELETE FROM face_embeddings WHERE personId = :personId")
    suspend fun deleteForPerson(personId: Long)

    @Query("""
        SELECT *
        FROM face_embeddings
    """)
    suspend fun getAllEmbeddings(): List<FaceEmbedding>
    @Query("""
    SELECT *
    FROM face_embeddings
    WHERE personId = :personId
    ORDER BY quality ASC
    LIMIT 1
""")
    suspend fun getLowestQualityEmbedding(
        personId: Long
    ): FaceEmbedding?
}