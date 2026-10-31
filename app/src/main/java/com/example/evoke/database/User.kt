package com.example.evoke.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val userId: Long = 0,

    val name: String,

    val phoneNo: String,

    val latitude: Double,

    val longitude: Double
)

data class UserWithDetails(
    val user: User,
    val emergencyContacts: List<EmergencyContact>
)