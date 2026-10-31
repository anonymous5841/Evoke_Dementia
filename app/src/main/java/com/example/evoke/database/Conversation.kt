package com.example.evoke.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
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
data class Conversation(
    @PrimaryKey(autoGenerate = true)
    val conversationId: Long = 0,

    val recognitionId: Long,

    val summary: String
)