package com.example.evoke.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evoke.database.repository.RecognitionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecognitionViewModel(
    private val repository: RecognitionRepository
) : ViewModel() {

    private val _pendingConversationCount =
        MutableStateFlow(0)

    val pendingConversationCount:
            StateFlow<Int> =
        _pendingConversationCount.asStateFlow()


    private val _lastRecognitionId =
        MutableStateFlow<Long?>(null)

    val lastRecognitionId: StateFlow<Long?> =
        _lastRecognitionId.asStateFlow()


    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()


    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()


    fun createRecognition(
        personId: Long,
        dateTime: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        rawConversation: String? = null,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val recognitionId =
                    repository.createRecognition(
                        personId = personId,
                        dateTime = dateTime,
                        latitude = latitude,
                        longitude = longitude,
                        rawConversation = rawConversation
                    )

                _lastRecognitionId.value =
                    recognitionId

                loadPendingConversationCount()

                onSuccess(recognitionId)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to create recognition."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun markConversationSummarized(
        conversationId: Long,
        summary: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.markConversationSummarized(
                    conversationId = conversationId,
                    summary = summary
                )

                loadPendingConversationCount()

                onSuccess()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to save conversation summary."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun loadPendingConversationCount() {
        viewModelScope.launch {
            _error.value = null

            try {
                _pendingConversationCount.value =
                    repository.getPendingConversationCount()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load pending conversations."
            }
        }
    }


    fun deleteRecognition(
        recognitionId: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.deleteRecognition(
                    recognitionId
                )

                if (_lastRecognitionId.value == recognitionId) {
                    _lastRecognitionId.value = null
                }

                loadPendingConversationCount()

                onSuccess()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to delete recognition."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun clearLastRecognition() {
        _lastRecognitionId.value = null
    }


    fun clearError() {
        _error.value = null
    }
}