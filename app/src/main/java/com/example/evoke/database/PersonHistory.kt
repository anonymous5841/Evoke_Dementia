package com.example.evoke.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "person_history",
    foreignKeys = [
        ForeignKey(
            entity = RecognitionEvent::class,
            parentColumns = ["recognitionId"],
            childColumns = ["recognitionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("recognitionId")
    ]
)
data class PersonHistory(
    @PrimaryKey(autoGenerate = true)
    val historyId: Long = 0,

    val recognitionId: Long,

    val latitude: Double? = null,

    val longitude: Double? = null
)