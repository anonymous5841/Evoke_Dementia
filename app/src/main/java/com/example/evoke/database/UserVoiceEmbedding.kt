package com.example.evoke.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_voice_embeddings")
data class UserVoiceEmbedding(
    @PrimaryKey(autoGenerate = true)
    val userVoiceEmbeddingId: Long = 0,

    val embedding: ByteArray,

    val type: String,

    val quality: Float
)