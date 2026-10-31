package com.example.evoke.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evoke.database.FaceEmbedding
import com.example.evoke.database.Person
import com.example.evoke.database.PersonDetails
import com.example.evoke.database.RecognitionDetails
import com.example.evoke.database.VoiceEmbedding
import com.example.evoke.database.repository.PersonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PersonViewModel(
    private val repository: PersonRepository
) : ViewModel() {

    private val _persons =
        MutableStateFlow<List<Person>>(emptyList())

    val persons: StateFlow<List<Person>> =
        _persons.asStateFlow()


    private val _selectedPerson =
        MutableStateFlow<Person?>(null)

    val selectedPerson: StateFlow<Person?> =
        _selectedPerson.asStateFlow()


    private val _personDetails =
        MutableStateFlow<PersonDetails?>(null)

    val personDetails: StateFlow<PersonDetails?> =
        _personDetails.asStateFlow()


    private val _recognitionDetails =
        MutableStateFlow<List<RecognitionDetails>>(emptyList())

    val recognitionDetails: StateFlow<List<RecognitionDetails>> =
        _recognitionDetails.asStateFlow()


    private val _faceEmbeddings =
        MutableStateFlow<List<FaceEmbedding>>(emptyList())

    val faceEmbeddings: StateFlow<List<FaceEmbedding>> =
        _faceEmbeddings.asStateFlow()


    private val _voiceEmbeddings =
        MutableStateFlow<List<VoiceEmbedding>>(emptyList())

    val voiceEmbeddings: StateFlow<List<VoiceEmbedding>> =
        _voiceEmbeddings.asStateFlow()


    private val _searchResults =
        MutableStateFlow<List<Person>>(emptyList())

    val searchResults: StateFlow<List<Person>> =
        _searchResults.asStateFlow()


    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()


    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()


    fun loadPersons() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _persons.value =
                    repository.getAllPersons()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load persons."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun loadPerson(personId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _selectedPerson.value =
                    repository.getPerson(personId)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load person."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun loadPersonDetails(personId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _personDetails.value =
                    repository.getPersonDetails(personId)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load person details."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun loadRecognitionDetails(personId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _recognitionDetails.value =
                    repository.getRecognitionDetailsForPerson(personId)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load recognition history."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun createPerson(
        person: Person,
        firstFaceEmbedding: FaceEmbedding,
        dateTime: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        rawConversation: String? = null,
        voiceEmbedding: VoiceEmbedding? = null,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val personId =
                    repository.createPerson(
                        person = person,
                        firstFaceEmbedding = firstFaceEmbedding,
                        dateTime = dateTime,
                        latitude = latitude,
                        longitude = longitude,
                        rawConversation = rawConversation,
                        voiceEmbedding = voiceEmbedding
                    )

                _persons.value =
                    repository.getAllPersons()

                onSuccess(personId)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to create person."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun updatePerson(
        personId: Long,
        name: String,
        relation: String,
        profileImagePath: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.updatePerson(
                    personId = personId,
                    name = name,
                    relation = relation,
                    profileImagePath = profileImagePath
                )

                _selectedPerson.value =
                    repository.getPerson(personId)

                _personDetails.value =
                    repository.getPersonDetails(personId)

                _persons.value =
                    repository.getAllPersons()

                onSuccess()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to update person."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun deletePerson(
        person: Person,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.deletePerson(person)

                _selectedPerson.value = null
                _personDetails.value = null

                _persons.value =
                    repository.getAllPersons()

                onSuccess()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to delete person."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun addFaceEmbedding(
        embedding: FaceEmbedding,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _error.value = null

            try {
                val embeddingId =
                    repository.addFaceEmbedding(embedding)

                _faceEmbeddings.value =
                    repository.getAllFaceEmbeddings()
                        .filter {
                            it.personId == embedding.personId
                        }

                onSuccess(embeddingId)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to add face embedding."
            }
        }
    }


    fun addVoiceEmbedding(
        embedding: VoiceEmbedding,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _error.value = null

            try {
                val embeddingId =
                    repository.addVoiceEmbedding(embedding)

                _voiceEmbeddings.value =
                    repository.getAllVoiceEmbeddings()
                        .filter {
                            it.personId == embedding.personId
                        }

                onSuccess(embeddingId)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to add voice embedding."
            }
        }
    }


    fun loadEmbeddings(personId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _faceEmbeddings.value =
                    repository.getAllFaceEmbeddings()
                        .filter {
                            it.personId == personId
                        }

                _voiceEmbeddings.value =
                    repository.getAllVoiceEmbeddings()
                        .filter {
                            it.personId == personId
                        }

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load embeddings."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun searchPersons(name: String) {
        viewModelScope.launch {
            _error.value = null

            try {
                if (name.isBlank()) {
                    _searchResults.value = emptyList()
                } else {
                    _searchResults.value =
                        repository.searchPersonsByName(name)
                }

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to search persons."
            }
        }
    }


    fun findExistingPerson(
        name: String,
        relation: String,
        onResult: (Person?) -> Unit
    ) {
        viewModelScope.launch {
            _error.value = null

            try {
                val person =
                    repository.findExistingPerson(
                        name = name,
                        relation = relation
                    )

                onResult(person)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to find person."
            }
        }
    }


    fun clearSelectedPerson() {
        _selectedPerson.value = null
        _personDetails.value = null
    }


    fun clearError() {
        _error.value = null
    }
}