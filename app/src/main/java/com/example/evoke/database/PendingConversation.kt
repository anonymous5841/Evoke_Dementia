package com.example.evoke.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_conversations",
    foreignKeys = [
        ForeignKey(
            entity = Conversation::class,
            parentColumns = ["conversationId"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("conversationId")
    ]
)
data class PendingConversation(
    @PrimaryKey(autoGenerate = true)
    val pendingId: Long = 0,

    val conversationId: Long
)