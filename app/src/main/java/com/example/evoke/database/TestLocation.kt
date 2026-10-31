package com.example.evoke.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_locations")
data class TestLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String
)