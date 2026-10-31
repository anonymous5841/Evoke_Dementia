package com.example.evoke.database.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.evoke.database.Location
import com.example.evoke.database.NearbyResult
import com.example.evoke.database.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LocationViewModel(
    private val repository: LocationRepository
) : ViewModel() {

    private val _locations =
        MutableStateFlow<List<Location>>(emptyList())

    val locations: StateFlow<List<Location>> =
        _locations.asStateFlow()


    private val _selectedLocation =
        MutableStateFlow<Location?>(null)

    val selectedLocation: StateFlow<Location?> =
        _selectedLocation.asStateFlow()


    private val _nearbyResults =
        MutableStateFlow<List<NearbyResult>>(emptyList())

    val nearbyResults: StateFlow<List<NearbyResult>> =
        _nearbyResults.asStateFlow()


    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()


    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()


    fun loadLocations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _locations.value =
                    repository.getAllLocations()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load locations."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun loadLocation(locationId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _selectedLocation.value =
                    repository.getLocation(locationId)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to load location."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun addLocation(
        location: Location,
        onSuccess: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val id =
                    repository.addLocation(location)

                loadLocations()

                onSuccess(id)

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to add location."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun updateLocation(
        location: Location,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.updateLocation(location)

                _selectedLocation.value = location

                _locations.value =
                    repository.getAllLocations()

                onSuccess()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to update location."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun deleteLocation(
        location: Location,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                repository.deleteLocation(location)

                _selectedLocation.value = null

                loadLocations()

                onSuccess()

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to delete location."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun loadNearbyLocations(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 5.0
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _nearbyResults.value =
                    repository.getNearbyResults(
                        latitude = latitude,
                        longitude = longitude,
                        radiusKm = radiusKm
                    )

            } catch (e: Exception) {
                _error.value =
                    e.message ?: "Failed to find nearby locations."
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun clearSelectedLocation() {
        _selectedLocation.value = null
    }


    fun clearError() {
        _error.value = null
    }
}