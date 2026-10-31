package com.example.evoke.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "face_embeddings",
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
data class FaceEmbedding(
    @PrimaryKey(autoGenerate = true)
    val embeddingId: Long = 0,

    val personId: Long,

    val embedding: ByteArray,

    val type: String,

    val quality: Float
)