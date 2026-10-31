package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

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
    @Query("""
    SELECT *
    FROM (
        
        SELECT
            locationId AS id,
            latitude,
            longitude,

            (
                6371.0 * 2.0 * ASIN(
                    SQRT(
                        POWER(
                            SIN(
                                (latitude - :latitude)
                                * 3.141592653589793 / 180.0 / 2.0
                            ),
                            2
                        )
                        +
                        COS(
                            :latitude * 3.141592653589793 / 180.0
                        )
                        *
                        COS(
                            latitude * 3.141592653589793 / 180.0
                        )
                        *
                        POWER(
                            SIN(
                                (longitude - :longitude)
                                * 3.141592653589793 / 180.0 / 2.0
                            ),
                            2
                        )
                    )
                )
            ) AS distanceKm,

            'LOCATION' AS source

        FROM locations

        WHERE latitude BETWEEN :minLatitude AND :maxLatitude
          AND longitude BETWEEN :minLongitude AND :maxLongitude


        UNION ALL


        SELECT
            historyId AS id,
            latitude,
            longitude,

            (
                6371.0 * 2.0 * ASIN(
                    SQRT(
                        POWER(
                            SIN(
                                (latitude - :latitude)
                                * 3.141592653589793 / 180.0 / 2.0
                            ),
                            2
                        )
                        +
                        COS(
                            :latitude * 3.141592653589793 / 180.0
                        )
                        *
                        COS(
                            latitude * 3.141592653589793 / 180.0
                        )
                        *
                        POWER(
                            SIN(
                                (longitude - :longitude)
                                * 3.141592653589793 / 180.0 / 2.0
                            ),
                            2
                        )
                    )
                )
            ) AS distanceKm,

            'PERSON_HISTORY' AS source

        FROM person_history

        WHERE latitude IS NOT NULL
          AND longitude IS NOT NULL
          AND latitude BETWEEN :minLatitude AND :maxLatitude
          AND longitude BETWEEN :minLongitude AND :maxLongitude
    )

    WHERE distanceKm <= :radiusKm

    ORDER BY distanceKm ASC
""")
    suspend fun getNearbyResults(
        latitude: Double,
        longitude: Double,
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double,
        radiusKm: Double
    ): List<NearbyResult>
}