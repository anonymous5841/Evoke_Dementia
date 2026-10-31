package com.example.evoke.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evoke.database.EmergencyContact
import com.example.evoke.database.repository.EmergencyContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EmergencyContactViewModel(
    private val repository: EmergencyContactRepository
) : ViewModel() {

    private val _contacts =
        MutableStateFlow<List<EmergencyContact>>(emptyList())

    val contacts: StateFlow<List<EmergencyContact>> =
        _contacts.asStateFlow()


    private val _contactCount =
        MutableStateFlow(0)

    val contactCount: StateFlow<Int> =
        _contactCount.asStateFlow()


    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()


    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()


    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                refreshContacts()
            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load emergency contacts."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun addContact(
        contact: EmergencyContact,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val id = repository.addContact(contact)

                refreshContacts()

                onSuccess(id)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to add emergency contact."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun deleteContact(
        contact: EmergencyContact,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.deleteContact(contact)

                refreshContacts()

                onSuccess()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to delete emergency contact."
            } finally {
                _isLoading.value = false
            }
        }
    }


    private suspend fun refreshContacts() {
        _contacts.value = repository.getContacts()
        _contactCount.value = repository.getContactCount()
    }


    fun clearError() {
        _error.value = null
    }
}