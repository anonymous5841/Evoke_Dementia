package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EmergencyContactDao {

    @Insert
    suspend fun insert(contact: EmergencyContact): Long

    @Update
    suspend fun update(contact: EmergencyContact)

    @Delete
    suspend fun delete(contact: EmergencyContact)

    @Query("""
        SELECT *
        FROM emergency_contacts
        ORDER BY priority ASC
    """)
    suspend fun getAllOrderedByPriority(): List<EmergencyContact>

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun getContactCount(): Int

    @Query("""
        SELECT *
        FROM emergency_contacts
        WHERE priority = :priority
        LIMIT 1
    """)
    suspend fun getByPriority(priority: Int): EmergencyContact?

    @Query("DELETE FROM emergency_contacts")
    suspend fun deleteAll()
}