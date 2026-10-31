package com.example.evoke.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["latitude"]),
        Index(value = ["longitude"])
    ]
)data class Location(
    @PrimaryKey(autoGenerate = true)
    val locationId: Long = 0,

    val title: String,

    val description: String,

    val latitude: Double,

    val longitude: Double
)
data class NearbyResult(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val source: String
)