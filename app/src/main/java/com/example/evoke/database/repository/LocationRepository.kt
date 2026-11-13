package com.example.evoke.database.repository

import com.example.evoke.database.AppDatabase
import com.example.evoke.database.GeoPoint
import com.example.evoke.database.Location
import com.example.evoke.database.NearbyResult
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LocationRepository(
    private val db: AppDatabase
) {

    private val locationDao = db.locationDao()

    companion object {
        private const val EARTH_RADIUS_KM = 6371.0
        private const val KM_PER_DEGREE = 111.32

        const val SOURCE_LOCATION = "LOCATION"
        const val SOURCE_PERSON_HISTORY = "PERSON_HISTORY"
    }

    suspend fun getAllLocations(): List<Location> {
        return locationDao.getAllLocations()
    }
    suspend fun addLocation(location: Location): Long {
        validateCoordinates(location.latitude, location.longitude)
        return locationDao.insert(location)
    }

    suspend fun updateLocation(location: Location) {
        validateCoordinates(location.latitude, location.longitude)
        locationDao.update(location)
    }
    suspend fun getLocation(
        locationId: Long
    ): Location? {
        return locationDao.getById(locationId)
    }

    suspend fun deleteLocation(
        location: Location
    ) {
        locationDao.delete(location)
    }

    /**
     * Finds saved locations and person-history points within [radiusKm]
     * of the given point, nearest first.
     *
     * SQL only narrows the search to a bounding box (using the indices).
     * The exact great-circle distance is computed here in Kotlin.
     *
     * Handles the antimeridian (longitude wrap) and the poles.
     *
     * If source == LOCATION        -> locationRepository.getLocation(id)
     * If source == PERSON_HISTORY  -> use the person history DAO with the id
     */
    suspend fun getNearbyResults(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 5.0
    ): List<NearbyResult> {

        require(latitude in -90.0..90.0) {
            "Latitude must be between -90 and 90."
        }

        require(longitude in -180.0..180.0) {
            "Longitude must be between -180 and 180."
        }

        require(radiusKm > 0.0) {
            "Radius must be greater than zero."
        }

        val latDelta = radiusKm / KM_PER_DEGREE

        val minLatitude = max(-90.0, latitude - latDelta)
        val maxLatitude = min(90.0, latitude + latDelta)

        val cosLatitude = cos(Math.toRadians(latitude))

        val reachesPole =
            latitude - latDelta <= -90.0 || latitude + latDelta >= 90.0

        // Near a pole every longitude is within range.
        val lonDelta =
            if (reachesPole || cosLatitude < 1e-6) {
                180.0
            } else {
                min(180.0, radiusKm / (KM_PER_DEGREE * cosLatitude))
            }

        val longitudeRanges = buildLongitudeRanges(longitude, lonDelta)

        val results = mutableListOf<NearbyResult>()

        for ((minLongitude, maxLongitude) in longitudeRanges) {

            locationDao.getLocationsInBox(
                minLatitude = minLatitude,
                maxLatitude = maxLatitude,
                minLongitude = minLongitude,
                maxLongitude = maxLongitude
            ).forEach { point ->
                addIfWithinRadius(
                    point = point,
                    source = SOURCE_LOCATION,
                    latitude = latitude,
                    longitude = longitude,
                    radiusKm = radiusKm,
                    results = results
                )
            }

            locationDao.getHistoryPointsInBox(
                minLatitude = minLatitude,
                maxLatitude = maxLatitude,
                minLongitude = minLongitude,
                maxLongitude = maxLongitude
            ).forEach { point ->
                addIfWithinRadius(
                    point = point,
                    source = SOURCE_PERSON_HISTORY,
                    latitude = latitude,
                    longitude = longitude,
                    radiusKm = radiusKm,
                    results = results
                )
            }
        }

        return results.sortedBy { it.distanceKm }
    }

    // Splits a longitude window into one or two ranges inside -180..180.
    private fun buildLongitudeRanges(
        longitude: Double,
        lonDelta: Double
    ): List<Pair<Double, Double>> {

        if (lonDelta >= 180.0) {
            return listOf(-180.0 to 180.0)
        }

        val minLongitude = longitude - lonDelta
        val maxLongitude = longitude + lonDelta

        return when {

            minLongitude < -180.0 -> listOf(
                (minLongitude + 360.0) to 180.0,
                -180.0 to maxLongitude
            )

            maxLongitude > 180.0 -> listOf(
                minLongitude to 180.0,
                -180.0 to (maxLongitude - 360.0)
            )

            else -> listOf(minLongitude to maxLongitude)
        }
    }

    private fun addIfWithinRadius(
        point: GeoPoint,
        source: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        results: MutableList<NearbyResult>
    ) {
        val distance = haversineKm(
            latitude,
            longitude,
            point.latitude,
            point.longitude
        )

        if (distance <= radiusKm) {
            results.add(
                NearbyResult(
                    id = point.id,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    distanceKm = distance,
                    source = source
                )
            )
        }
    }

    private fun haversineKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) *
                    cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)

        return 2.0 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(a)))
    }
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
