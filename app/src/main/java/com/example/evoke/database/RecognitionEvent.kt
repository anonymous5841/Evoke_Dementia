package com.example.evoke.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recognition_events",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["personId"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("personId")
    ]
)
data class RecognitionEvent(
    @PrimaryKey(autoGenerate = true)
    val recognitionId: Long = 0,

    val personId: Long,

    val dateTime: Long
)

data class RecognitionDetails(
    val recognition: RecognitionEvent,
    val history: PersonHistory?,
    val conversation: Conversation?
)