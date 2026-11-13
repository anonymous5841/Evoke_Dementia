package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// A bare coordinate returned by the bounding-box queries.
// The exact distance is computed in Kotlin (see LocationRepository),
// because SQLite on Android / SQLCipher is not guaranteed to have
// SIN, COS, ASIN, SQRT and POWER available.
data class GeoPoint(
    val id: Long,
    val latitude: Double,
    val longitude: Double
)

@Dao
interface LocationDao {

    @Insert
    suspend fun insert(location: Location): Long

    @Update
    suspend fun update(location: Location)

    @Delete
    suspend fun delete(location: Location)

    @Query("""
        SELECT *
        FROM locations
        ORDER BY locationId DESC
    """)
    suspend fun getAllLocations(): List<Location>

    @Query("""
        SELECT *
        FROM locations
        WHERE locationId = :locationId
        LIMIT 1
    """)
    suspend fun getById(locationId: Long): Location?

    // Saved locations inside a bounding box (uses the latitude/longitude indices).
    @Query("""
        SELECT locationId AS id, latitude, longitude
        FROM locations
        WHERE latitude BETWEEN :minLatitude AND :maxLatitude
          AND longitude BETWEEN :minLongitude AND :maxLongitude
    """)
    suspend fun getLocationsInBox(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<GeoPoint>

    // Person-history points inside a bounding box.
    @Query("""
        SELECT historyId AS id, latitude, longitude
        FROM person_history
        WHERE latitude IS NOT NULL
          AND longitude IS NOT NULL
          AND latitude BETWEEN :minLatitude AND :maxLatitude
          AND longitude BETWEEN :minLongitude AND :maxLongitude
    """)
    suspend fun getHistoryPointsInBox(
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double
    ): List<GeoPoint>
}
