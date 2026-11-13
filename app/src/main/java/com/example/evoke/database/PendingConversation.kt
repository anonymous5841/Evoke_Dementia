package com.example.evoke.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Represents the current stage of a pending conversation.
object PendingConversationStatus {

    // Audio has been recorded and is waiting for transcription.
    const val WAITING_TRANSCRIPTION = "WAITING_TRANSCRIPTION"

    // Transcription has been saved and is waiting for summarization.
    const val WAITING_SUMMARY = "WAITING_SUMMARY"
}

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
    indices = [ Index(value = ["conversationId"], unique = true) ]

)
data class PendingConversation(

    @PrimaryKey(autoGenerate = true)
    val pendingId: Long = 0,

    // Identifies the conversation this pending entry belongs to.
    val conversationId: Long,

    // Location of the temporarily stored filtered audio file.
    // Becomes null after the transcription has been safely saved
    // and the audio file has been deleted.
    val audioPath: String? = null,

    // Tracks which API needs to process this conversation next.
    @ColumnInfo(defaultValue = "'WAITING_TRANSCRIPTION'")
    val status: String = PendingConversationStatus.WAITING_TRANSCRIPTION
)
