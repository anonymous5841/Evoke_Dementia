package com.example.evoke.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true)
    val personId: Long = 0,

    val name: String,

    val relation: String,

    val profileImagePath: String,

    val voiceEnabled: Boolean
)

data class PersonDetails(
    val person: Person,
    val recognition: RecognitionEvent,
    val history: PersonHistory?,
    val conversation: Conversation?
)