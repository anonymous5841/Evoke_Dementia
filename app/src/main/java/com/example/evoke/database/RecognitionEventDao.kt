package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecognitionEventDao {

    @Insert
    suspend fun insert(event: RecognitionEvent): Long

    @Query("""
        SELECT *
        FROM recognition_events
        WHERE personId = :personId
        ORDER BY dateTime DESC
    """)
    suspend fun getEventsForPerson(personId: Long): List<RecognitionEvent>

    @Query("""
        SELECT *
        FROM recognition_events
        WHERE recognitionId = :recognitionId
        LIMIT 1
    """)
    suspend fun getEventById(recognitionId: Long): RecognitionEvent?

    @Query("""
        SELECT *
        FROM recognition_events
        ORDER BY dateTime DESC
    """)
    suspend fun getAllEvents(): List<RecognitionEvent>

    @Query("""
    DELETE FROM recognition_events
    WHERE recognitionId = :recognitionId
""")
    suspend fun deleteById(recognitionId: Long)

    @Query("""
    SELECT personId
    FROM recognition_events
    WHERE recognitionId = :recognitionId
    LIMIT 1
""")
    suspend fun getPersonIdByRecognitionId(
        recognitionId: Long
    ): Long?

    @Query("""
    SELECT *
    FROM recognition_events
    WHERE personId = :personId
    ORDER BY dateTime DESC
    LIMIT 1
""")
    suspend fun getLatestRecognition(
        personId: Long
    ): RecognitionEvent?
}