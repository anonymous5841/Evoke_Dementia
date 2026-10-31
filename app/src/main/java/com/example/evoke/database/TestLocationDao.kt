package com.example.evoke.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TestLocationDao {

    @Insert
    suspend fun insert(location: TestLocation)

    @Query("SELECT * FROM test_locations")
    suspend fun getAll(): List<TestLocation>
}