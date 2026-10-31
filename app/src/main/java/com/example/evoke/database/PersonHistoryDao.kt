package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PersonHistoryDao {

    @Insert
    suspend fun insert(history: PersonHistory): Long

    @Update
    suspend fun update(history: PersonHistory)

    @Query("""
        SELECT *
        FROM person_history
        WHERE recognitionId = :recognitionId
        LIMIT 1
    """)
    suspend fun getByRecognitionId(recognitionId: Long): PersonHistory?

    @Query("""
        SELECT ph.*
        FROM person_history ph
        INNER JOIN recognition_events re
            ON ph.recognitionId = re.recognitionId
        WHERE re.personId = :personId
        ORDER BY re.dateTime DESC
    """)
    suspend fun getHistoryForPerson(personId: Long): List<PersonHistory>

}