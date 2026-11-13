package com.example.evoke.database.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.repository.EmergencyContactRepository
import com.example.evoke.database.repository.LocationRepository
import com.example.evoke.database.repository.PersonRepository
import com.example.evoke.database.repository.RecognitionRepository
import com.example.evoke.database.repository.UserRepository
import com.example.evoke.storage.AudioStorage
import com.example.evoke.storage.PersonImageStorage

class AppViewModelFactory(
    private val db: AppDatabase,
    context: Context
) : ViewModelProvider.Factory {

    // The repositories own the files that belong to the rows they delete.
    private val audioStorage =
        AudioStorage(context.applicationContext)

    private val imageStorage =
        PersonImageStorage(context.applicationContext)

    private val recognitionRepository =
        RecognitionRepository(db, audioStorage)

    private val personRepository =
        PersonRepository(
            db = db,
            recognitionRepository = recognitionRepository,
            audioStorage = audioStorage,
            imageStorage = imageStorage
        )

    private val emergencyContactRepository =
        EmergencyContactRepository(db)

    private val locationRepository =
        LocationRepository(db)

    private val userRepository =
        UserRepository(db, audioStorage, imageStorage)

    /**
     * Call once at app start, before any recording can be saved.
     * Removes half-copied and unreferenced audio files.
     */
    suspend fun cleanupOrphanedAudio() {
        recognitionRepository.cleanupOrphanedAudio()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        return when {

            modelClass.isAssignableFrom(PersonViewModel::class.java) ->
                PersonViewModel(personRepository)

            modelClass.isAssignableFrom(UserViewModel::class.java) ->
                UserViewModel(userRepository)

            modelClass.isAssignableFrom(
                EmergencyContactViewModel::class.java
            ) ->
                EmergencyContactViewModel(
                    emergencyContactRepository
                )

            modelClass.isAssignableFrom(
                LocationViewModel::class.java
            ) ->
                LocationViewModel(
                    locationRepository
                )

            modelClass.isAssignableFrom(
                RecognitionViewModel::class.java
            ) ->
                RecognitionViewModel(
                    recognitionRepository
                )

            else ->
                throw IllegalArgumentException(
                    "Unknown ViewModel class: ${modelClass.name}"
                )
        } as T
    }
}