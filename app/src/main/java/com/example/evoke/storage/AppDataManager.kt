package com.example.evoke.storage

import android.content.Context
import com.example.evoke.database.AppDatabase
import com.example.evoke.security.DatabaseKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppDataManager(
    private val context: Context
) {

    /**
     * Completely clears locally stored application data.
     *
     * This removes:
     * - All Room/SQLCipher database records
     * - The SQLCipher database file
     * - All locally stored profile images
     * - The encrypted database key from SharedPreferences
     * - The Android Keystore master key
     *
     * After this operation, the local database cannot be recovered.
     *
     * IMPORTANT:
     * The database must not be used again after this method
     * until the application creates a new database/key.
     */
    suspend fun clearAllLocalData() = withContext(Dispatchers.IO) {

        val applicationContext =
            context.applicationContext

        // 1. Stop access to the database.
        AppDatabase.closeDatabase()

        // 2. Delete the encrypted SQLCipher database.
        applicationContext.deleteDatabase(
            "dementia_database"
        )

        // 3. Delete all locally stored profile images.
        PersonImageStorage(applicationContext)
            .deleteAllProfileImages()

        // 4. Delete the database key and Keystore master key.
        DatabaseKeyManager(applicationContext)
            .deleteDatabaseKey()

        // 5. Remove the now-empty people directory.
        val peopleDirectory =
            File(
                applicationContext.filesDir,
                "people"
            )

        if (peopleDirectory.exists()) {
            peopleDirectory.delete()
        }
    }
}