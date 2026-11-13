package com.example.evoke.database.repository

internal fun validateCoordinates(latitude: Double?, longitude: Double?) {
    require((latitude == null) == (longitude == null)) {
        "Latitude and longitude must both be provided or both be omitted."
    }
    if (latitude != null && longitude != null) {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90." }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180." }
    }
}