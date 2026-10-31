package com.example.evoke.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emergency_contacts")
data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val emergencyContactId: Long = 0,

    val phoneNo: String,

    val priority: Int
)