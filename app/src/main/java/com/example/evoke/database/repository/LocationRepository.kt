package com.example.evoke.database.repository

import com.example.evoke.database.AppDatabase
import com.example.evoke.database.Location
import com.example.evoke.database.NearbyResult

class LocationRepository(
    private val db: AppDatabase
) {

    private val locationDao = db.locationDao()

    suspend fun addLocation(
        location: Location
    ): Long {
        return locationDao.insert(location)
    }

    suspend fun getAllLocations(): List<Location> {
        return locationDao.getAllLocations()
    }

    suspend fun getLocation(
        locationId: Long
    ): Location? {
        return locationDao.getById(locationId)
    }

    suspend fun updateLocation(
        location: Location
    ) {
        locationDao.update(location)
    }

    suspend fun deleteLocation(
        location: Location
    ) {
        locationDao.delete(location)
    }

    suspend fun getNearbyResults(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 5.0
    ): List<NearbyResult> {

        val latDelta = radiusKm / 111.32

        val longitudeDegreesPerKm =
            111.32 * kotlin.math.cos(
                Math.toRadians(latitude)
            )

        val lonDelta =
            radiusKm / longitudeDegreesPerKm

        return locationDao.getNearbyResults(
            latitude = latitude,
            longitude = longitude,

            minLatitude = latitude - latDelta,
            maxLatitude = latitude + latDelta,

            minLongitude = longitude - lonDelta,
            maxLongitude = longitude + lonDelta,

            radiusKm = radiusKm
        )
    }
//    If source = LOCATION:
//
//    locationRepository.getLocation(id)
//
//    and you get the full:
//
//    title
//    description
//    latitude
//    longitude
//
//    If source = PERSON_HISTORY:
//
//    personHistoryRepository.getHistoryById(id)
//    Then your frontend can simply call:
//
//    val locations = locationRepository.getNearbyLocations(
//        latitude = pinLatitude,
//        longitude = pinLongitude
//    )
//
//    Because the default is:
//
//    radiusKm = 5.0
//
//    it searches within 5 km.
//
//    Or the frontend can choose:
//
//    getNearbyLocations(
//    latitude = pinLatitude,
//    longitude = pinLongitude,
//    radiusKm = 10.0
//    )
//
//    for 10 km.
}