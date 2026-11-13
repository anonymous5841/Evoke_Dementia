package com.example.evoke.debug

import android.content.Context
import android.util.Log
import com.example.evoke.database.*
import com.example.evoke.database.repository.*
import com.example.evoke.storage.AudioStorage
import com.example.evoke.storage.PersonImageStorage
import kotlinx.coroutines.runBlocking
import java.io.File

object DebugSeed {
    private const val DELETE_BOB_ONCE = false
    fun seedIfEmpty(context: Context) {
        val ctx = context.applicationContext
        Thread {
            try {
                runBlocking {
                    val db = AppDatabase.getDatabase(ctx)
                    val audio = AudioStorage(ctx)
                    val images = PersonImageStorage(ctx)
                    val recognition = RecognitionRepository(db, audio)
                    val persons = PersonRepository(db, recognition, audio, images)
                    val users = UserRepository(db, audio, images)

                    if (users.getUser() != null) {
                        val names = persons.getAllPersons().map { it.name }
                        val pending = recognition.getPendingConversationCount()
                        Log.d("EVOKE_SEED", "Present: people=$names, pending=$pending")

                        if (DELETE_BOB_ONCE) {
                            persons.getAllPersons().firstOrNull { it.name == "Seed Bob" }
                                ?.let { persons.deletePerson(it) }
                            Log.d("EVOKE_SEED", "Deleted Seed Bob")
                        }

                        return@runBlocking
                    }

                    users.createUser(
                        User(name = "Seed User", phoneNo = "0300", latitude = 31.5, longitude = 74.3),
                        listOf(EmergencyContact(phoneNo = "111", priority = 1)),
                        listOf(UserVoiceEmbedding(embedding = ByteArray(4), type = "SEED", quality = 0.5f))
                    )

                    val peopleDir = File(ctx.filesDir, "people").apply { mkdirs() }
                    fun provider(): (Long) -> String = { id ->
                        File(peopleDir, "person_$id.jpg").writeText("img")
                        "people/person_$id.jpg"
                    }
                    fun face() = FaceEmbedding(personId = 0, embedding = ByteArray(8), type = "SEED", quality = 0.9f)
                    fun voice() = VoiceEmbedding(personId = 0, embedding = ByteArray(8), type = "SEED", quality = 0.9f)

                    // Person with location only
                    persons.createPerson(
                        person = Person(name = "Seed Alice", relation = "Friend", profileImagePath = "", voiceEnabled = false),
                        firstFaceEmbedding = face(),
                        dateTime = System.currentTimeMillis(),
                        latitude = 31.5, longitude = 74.3,
                        profileImageProvider = provider()
                    )

                    // Person with a queued conversation
                    val src = File(ctx.cacheDir, "seed.m4a").apply { writeBytes(ByteArray(2048) { 1 }) }
                    val audioPath = audio.saveFilteredAudio(src)
                    src.delete()
                    persons.createPerson(
                        person = Person(name = "Seed Bob", relation = "Son", profileImagePath = "", voiceEnabled = false),
                        firstFaceEmbedding = face(),
                        dateTime = System.currentTimeMillis(),
                        audioPath = audioPath,
                        voiceEmbedding = voice(),
                        profileImageProvider = provider()
                    )

                    Log.d("EVOKE_SEED", "Seeded: 1 user, 2 people, 1 pending conversation")

                }
            } catch (e: Throwable) {
                Log.e("EVOKE_SEED", "Seeding failed", e)
            }
        }.start()
    }
}



//Set DELETE_BOB_ONCE = true and run. You should see Present: people=[Seed Alice, Seed Bob] and then Deleted Seed Bob.
//Set it back to false immediately and run again. Expect people=[Seed Alice], pending=0.
//Open Cloud Backup, reconnect Google Drive (the connection isn’t kept across app restarts), tap Restore My Data, accept the warning, enter the password, and tap Restart.
//Expect Present: people=[Seed Alice, Seed Bob], pending=1. If Bob is back, the restore is fully proven.

//filter:tag:EVOKE_SEED | tag:CloudBackup