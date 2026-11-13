package com.example.evoke.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "emergency_contacts",
    indices = [ Index(value = ["priority"], unique = true) ]
)data class EmergencyContact(
    @PrimaryKey(autoGenerate = true)
    val emergencyContactId: Long = 0,

    val phoneNo: String,

    val priority: Int
)