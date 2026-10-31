package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PersonDao {

    @Insert
    suspend fun insert(person: Person): Long

    @Update
    suspend fun update(person: Person)

    @Delete
    suspend fun delete(person: Person)

    @Query("SELECT * FROM persons ORDER BY name ASC")
    suspend fun getAllPersons(): List<Person>

    @Query("SELECT * FROM persons WHERE personId = :personId")
    suspend fun getPersonById(personId: Long): Person?

    @Query("SELECT * FROM persons WHERE name = :name AND relation = :relation LIMIT 1")
    suspend fun findByNameAndRelation(
        name: String,
        relation: String
    ): Person?

    @Query("SELECT COUNT(*) FROM persons")
    suspend fun getPersonCount(): Int

    @Query("""
    SELECT *
    FROM persons
    WHERE name LIKE '%' || :name || '%'
    ORDER BY name ASC
""")
    suspend fun searchPersonsByName(name: String): List<Person>

    @Query("""
    UPDATE persons
    SET name = :name,
        relation = :relation,
        profileImagePath = :profileImagePath
    WHERE personId = :personId
""")
    suspend fun updatePersonDetails(
        personId: Long,
        name: String,
        relation: String,
        profileImagePath: String
    )

    @Query("""
    SELECT voiceEnabled
    FROM persons
    WHERE personId = :personId
    LIMIT 1
""")
    suspend fun isVoiceEnabled(
        personId: Long
    ): Boolean?
}