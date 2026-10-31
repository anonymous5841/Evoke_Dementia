package com.example.evoke.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.evoke.database.AppDatabase
import com.example.evoke.database.repository.EmergencyContactRepository
import com.example.evoke.database.repository.LocationRepository
import com.example.evoke.database.repository.PersonRepository
import com.example.evoke.database.repository.RecognitionRepository
import com.example.evoke.database.repository.UserRepository

class AppViewModelFactory(
    private val db: AppDatabase
) : ViewModelProvider.Factory {

    private val recognitionRepository =
        RecognitionRepository(db)

    private val personRepository =
        PersonRepository(
            db = db,
            recognitionRepository = recognitionRepository
        )

    private val emergencyContactRepository =
        EmergencyContactRepository(db)

    private val locationRepository =
        LocationRepository(db)

    private val userRepository =
        UserRepository(db)

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