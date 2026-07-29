package com.example.myapplication.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.myapplication.ui.PersonItem
import com.example.myapplication.ui.SearchDisplayMode

class SearchViewModel : ViewModel() {

    var mode by mutableStateOf(SearchDisplayMode.PEOPLE)
        private set

    var searchQuery by mutableStateOf("")
        private set

    var selectedLocation by mutableStateOf("")
        private set

    // Dummy data for now — swap for repository calls once DB is attached
    val people: List<PersonItem> = List(20) { PersonItem(name = "Name") }

    fun onSearchQueryChange(query: String) {
        // only letters + spaces allowed
        searchQuery = query.filter { it.isLetter() || it.isWhitespace() }
        mode = SearchDisplayMode.PEOPLE
    }

    fun onLocationButtonClick() {
        mode = SearchDisplayMode.LOCATION
    }

    fun onSelectedLocationChange(value: String) {
        selectedLocation = value
    }
}