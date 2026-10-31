package com.example.evoke.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evoke.database.EmergencyContact
import com.example.evoke.database.User
import com.example.evoke.database.UserVoiceEmbedding
import com.example.evoke.database.UserWithDetails
import com.example.evoke.database.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val repository: UserRepository
) : ViewModel() {

    private val _user = MutableStateFlow<UserWithDetails?>(null)
    val user: StateFlow<UserWithDetails?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()


    fun loadUser() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _user.value = repository.getUser()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load user."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun createUser(
        user: User,
        emergencyContacts: List<EmergencyContact>,
        voiceEmbeddings: List<UserVoiceEmbedding>,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val userId = repository.createUser(
                    user = user,
                    emergencyContacts = emergencyContacts,
                    voiceEmbeddings = voiceEmbeddings
                )

                _user.value = repository.getUser()

                onSuccess(userId)

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to create user."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun updateUser(
        user: User,
        emergencyContacts: List<EmergencyContact>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.updateUser(
                    user = user,
                    emergencyContacts = emergencyContacts
                )

                _user.value = repository.getUser()

                onSuccess()

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update user."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun addVoiceEmbedding(
        embedding: UserVoiceEmbedding,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _error.value = null

            try {
                val embeddingId =
                    repository.addVoiceEmbedding(embedding)

                onSuccess(embeddingId)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to add voice embedding."
            }
        }
    }


    fun replaceVoiceEmbeddings(
        embeddings: List<UserVoiceEmbedding>,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.replaceVoiceEmbeddings(embeddings)

                _user.value = repository.getUser()

                onSuccess()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to replace voice embeddings."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun resetDatabase(
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.deleteUserAndResetDatabase()

                _user.value = null

                onSuccess()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to reset database."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun clearError() {
        _error.value = null
    }
}