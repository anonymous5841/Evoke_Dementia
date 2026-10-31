//package com.example.myapplication
//
//import android.app.Application
//import android.util.Log
//import com.example.myapplication.backup.BackupArchiveManager
//import com.example.myapplication.database.AppDatabase
//import com.example.myapplication.database.User
//import java.io.File
//import com.example.myapplication.security.BackupDatabaseKeyManager
//import com.example.myapplication.security.BackupKeyManager
//import com.example.myapplication.security.DatabaseKeyManager
//class MyApplication : Application() {
//
//    val database: AppDatabase by lazy {
//        AppDatabase.getDatabase(this)
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        System.loadLibrary("sqlcipher")
//
//        runRestoreTest()
//    }
//
//    private fun runRestoreTest() {
//
//        Thread {
//
//            val tag = "RESTORE_TEST"
//
//            try {
//
//                Log.d(tag, "========== RESTORE TEST START ==========")
//
//                val backupKeyManager =
//                    BackupKeyManager(this)
//
//                val databaseKeyManager =
//                    DatabaseKeyManager(this)
//
//                val backupDatabaseKeyManager =
//                    BackupDatabaseKeyManager()
//
//                val backupKey =
//                    backupKeyManager.getBackupKey()
//
//                val databaseKey =
//                    databaseKeyManager.getDatabaseKey()
//
//                val encryptedDatabaseKey =
//                    backupDatabaseKeyManager.encryptDatabaseKey(
//                        databaseKey = databaseKey,
//                        backupKey = backupKey
//                    )
//
//                val restoredDatabaseKey =
//                    backupDatabaseKeyManager.decryptDatabaseKey(
//                        encryptedKey = encryptedDatabaseKey.encryptedKey,
//                        iv = encryptedDatabaseKey.iv,
//                        backupKey = backupKey
//                    )
//
//                Log.d(
//                    tag,
//                    "Database key size = ${databaseKey.size} bytes"
//                )
//
//                Log.d(
//                    tag,
//                    "Backup key size = ${backupKey.size} bytes"
//                )
//
//                Log.d(
//                    tag,
//                    "Database key restored = ${
//                        databaseKey.contentEquals(restoredDatabaseKey)
//                    }"
//                )
//                // ---------------------------------------------------------
//                // 1. Open the current SQLCipher database
//                // ---------------------------------------------------------
//
//                val db = database
//
//                Log.d(
//                    tag,
//                    "1. Database opened successfully"
//                )
//
//                // ---------------------------------------------------------
//                // 2. Insert a clearly identifiable test user
//                // ---------------------------------------------------------
//
//                val testUser = User(
//                    name = "RESTORE TEST USER",
//                    phoneNo = "03009999999",
//                    latitude = 31.5204,
//                    longitude = 74.3587
//                )
//
//                val userId =
//                    kotlinx.coroutines.runBlocking {
//                        db.userDao().insert(testUser)
//                    }
//
//                Log.d(
//                    tag,
//                    "2. Test user inserted. ID = $userId"
//                )
//
//                // ---------------------------------------------------------
//                // 3. Create test image
//                // ---------------------------------------------------------
//
//                val peopleDirectory =
//                    File(filesDir, "people")
//
//                peopleDirectory.mkdirs()
//
//                val testImage =
//                    File(
//                        peopleDirectory,
//                        "person_restore_test.jpg"
//                    )
//
//                testImage.writeText(
//                    "RESTORE TEST IMAGE"
//                )
//
//                Log.d(
//                    tag,
//                    "3. Test image created = ${testImage.exists()}"
//                )
//
//                // ---------------------------------------------------------
//                // 4. Create backup archive
//                // ---------------------------------------------------------
//
//                val testDirectory =
//                    File(
//                        cacheDir,
//                        "restore_test"
//                    )
//
//                testDirectory.deleteRecursively()
//                testDirectory.mkdirs()
//
//                val backupArchive =
//                    File(
//                        testDirectory,
//                        "restore_test.zip"
//                    )
//
//
//                Log.d(
//                    tag,
//                    "Database WAL checkpoint completed"
//                )
//
//
//                // ---------------------------------------------------------
//                // 5. Close Room/SQLCipher
//                // ---------------------------------------------------------
//
//                AppDatabase.closeDatabase()
//
//                Log.d(
//                    tag,
//                    "4. Database closed"
//                )
//
//
//                val archiveManager =
//                    BackupArchiveManager(this)
//
//                archiveManager.createBackupArchive(
//                    backupArchive
//                )
//
//                Log.d(
//                    tag,
//                    ". Backup archive created = ${backupArchive.exists()}"
//                )
//
//                // ---------------------------------------------------------
//                // 6. Delete local database
//                // ---------------------------------------------------------
//
//                val databaseFile =
//                    getDatabasePath("dementia_database")
//
//                if (databaseFile.exists()) {
//                    databaseFile.delete()
//                }
//
//                Log.d(
//                    tag,
//                    "6. Local database deleted = ${!databaseFile.exists()}"
//                )
//
//                // ---------------------------------------------------------
//                // 7. Delete local people directory
//                // ---------------------------------------------------------
//
//                if (peopleDirectory.exists()) {
//                    peopleDirectory.deleteRecursively()
//                }
//
//                Log.d(
//                    tag,
//                    "7. People directory deleted = ${!peopleDirectory.exists()}"
//                )
//
//                // ---------------------------------------------------------
//                // 8. Extract backup
//                // ---------------------------------------------------------
//
//                val extractedDirectory =
//                    archiveManager.extractBackupArchive(
//                        backupArchive
//                    )
//
//                Log.d(
//                    tag,
//                    "8. Backup extracted successfully"
//                )
//
//
//                //
//
//                // ---------------------------------------------------------
//                // 9. Restore database + images
//                // ---------------------------------------------------------
//
//                archiveManager.commitRestoredBackup(
//                    extractedDirectory
//                )
//
//                Log.d(
//                    tag,
//                    "9. Backup committed successfully"
//                )
//
//                archiveManager.deleteTemporaryBackup(
//                    extractedDirectory
//                )
//
//                // ---------------------------------------------------------
//                // 10. Verify restored database file
//                // ---------------------------------------------------------
//
//                Log.d(
//                    tag,
//                    "10. Restored database exists = ${databaseFile.exists()}"
//                )
//
//                Log.d(
//                    tag,
//                    "11. Restored database size = ${databaseFile.length()} bytes"
//                )
//
//                // ---------------------------------------------------------
//                // 11. Verify restored image
//                // ---------------------------------------------------------
//
//                val restoredImage =
//                    File(
//                        peopleDirectory,
//                        "person_restore_test.jpg"
//                    )
//
//                Log.d(
//                    tag,
//                    "12. Restored image exists = ${restoredImage.exists()}"
//                )
//
//                if (restoredImage.exists()) {
//                    Log.d(
//                        tag,
//                        "13. Restored image content = ${restoredImage.readText()}"
//                    )
//                }
//
//                // ---------------------------------------------------------
//                // 12. Reopen Room + SQLCipher
//                // ---------------------------------------------------------
//
//                val restoredDatabase =
//                    AppDatabase.getDatabase(this)
//
//                Log.d(
//                    tag,
//                    "14. Restored database opened successfully"
//                )
//
//                // ---------------------------------------------------------
//                // 13. Read the restored test user
//                // ---------------------------------------------------------
//
//                val restoredUser =
//                    kotlinx.coroutines.runBlocking {
//                        restoredDatabase.userDao().getUserById(userId)
//                    }
//
//                Log.d(
//                    tag,
//                    "15. Restored user = $restoredUser"
//                )
//
//                // ---------------------------------------------------------
//                // 14. Final verification
//                // ---------------------------------------------------------
//
//                val databaseRestored =
//                    restoredUser != null
//
//                val imageRestored =
//                    restoredImage.exists() &&
//                            restoredImage.readText() ==
//                            "RESTORE TEST IMAGE"
//
//                Log.d(
//                    tag,
//                    "16. Database restore passed = $databaseRestored"
//                )
//
//                Log.d(
//                    tag,
//                    "17. Image restore passed = $imageRestored"
//                )
//
//                Log.d(
//                    tag,
//                    "========== RESTORE TEST PASSED = ${databaseRestored && imageRestored} =========="
//
//                )
//
//            } catch (e: Exception) {
//
//                Log.e(
//                    tag,
//                    "========== RESTORE TEST FAILED ==========",
//                    e
//                )
//            }
//
//        }.start()
//    }
//}

//package com.example.myapplication
//
//import android.app.Application
//import com.example.myapplication.database.AppDatabase
//
//class MyApplication : Application() {
//
//    val database: AppDatabase by lazy {
//        AppDatabase.getDatabase(this)
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        System.loadLibrary("sqlcipher")
//    }
//}

//package com.example.myapplication
//
//import android.app.Application
//import android.util.Log
//import com.example.myapplication.backup.BackupPackageManager
//import com.example.myapplication.database.AppDatabase
//import com.example.myapplication.database.User
//import java.io.File
//import kotlinx.coroutines.runBlocking
//
//class MyApplication : Application() {
//
//    val database: AppDatabase by lazy {
//        AppDatabase.getDatabase(this)
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//
//        System.loadLibrary("sqlcipher")
//
//        runBackupRestoreTest()
//    }
//
//    private fun runBackupRestoreTest() {
//
//        Thread {
//
//            val tag = "BACKUP_RESTORE_TEST"
//
//            try {
//
//                Log.d(
//                    tag,
//                    "========== BACKUP + RESTORE TEST START =========="
//                )
//
//                // -----------------------------------------------------
//                // 1. Open the SQLCipher database
//                // -----------------------------------------------------
//
//                val db =
//                    database
//
//                Log.d(
//                    tag,
//                    "1. Database opened successfully"
//                )
//
//                // -----------------------------------------------------
//                // 2. Insert clearly identifiable test user
//                // -----------------------------------------------------
//
//                val testUser =
//                    User(
//                        name = "BACKUP TEST USER",
//                        phoneNo = "03001112222",
//                        latitude = 31.5204,
//                        longitude = 74.3587
//                    )
//
//                val testUserId =
//                    runBlocking {
//                        db.userDao().insert(testUser)
//                    }
//
//                Log.d(
//                    tag,
//                    "2. Test user inserted. ID = $testUserId"
//                )
//
//                // -----------------------------------------------------
//                // 3. Create test image
//                // -----------------------------------------------------
//
//                val peopleDirectory =
//                    File(
//                        filesDir,
//                        "people"
//                    )
//
//                peopleDirectory.mkdirs()
//
//                val testImage =
//                    File(
//                        peopleDirectory,
//                        "backup_test_image.jpg"
//                    )
//
//                testImage.writeText(
//                    "BACKUP TEST IMAGE"
//                )
//
//                require(testImage.exists()) {
//                    "Test image was not created."
//                }
//
//                Log.d(
//                    tag,
//                    "3. Test image created successfully"
//                )
//
//                // -----------------------------------------------------
//                // 4. Create encrypted backup
//                // -----------------------------------------------------
//
//                val backupPassword =
//                    "EvokeTestPassword123!".toCharArray()
//
//                val backupPackageManager =
//                    BackupPackageManager(this)
//
//                val backupPackageDirectory =
//                    backupPackageManager.createBackup(
//                        backupPassword
//                    )
//
//                backupPassword.fill('\u0000')
//
//                Log.d(
//                    tag,
//                    "4. Encrypted backup created"
//                )
//
//                Log.d(
//                    tag,
//                    "Backup package = ${backupPackageDirectory.absolutePath}"
//                )
//
//                // -----------------------------------------------------
//                // 5. Verify backup files
//                // -----------------------------------------------------
//
//                val encryptedBackup =
//                    backupPackageManager.getEncryptedBackupFile(
//                        backupPackageDirectory
//                    )
//
//                val metadataFile =
//                    backupPackageManager.getMetadataFile(
//                        backupPackageDirectory
//                    )
//
//                val temporaryZip =
//                    File(
//                        backupPackageDirectory,
//                        "backup.zip"
//                    )
//
//                require(encryptedBackup.exists()) {
//                    "Encrypted backup file does not exist."
//                }
//
//                require(
//                    encryptedBackup.length() > 0
//                ) {
//                    "Encrypted backup file is empty."
//                }
//
//                require(metadataFile.exists()) {
//                    "Metadata file does not exist."
//                }
//
//                require(
//                    metadataFile.length() > 0
//                ) {
//                    "Metadata file is empty."
//                }
//
//                require(!temporaryZip.exists()) {
//                    "Unencrypted backup.zip still exists."
//                }
//
//                Log.d(
//                    tag,
//                    "5. Backup verification passed"
//                )
//
//                Log.d(
//                    tag,
//                    "Encrypted backup size = ${encryptedBackup.length()} bytes"
//                )
//
//                Log.d(
//                    tag,
//                    "Metadata size = ${metadataFile.length()} bytes"
//                )
//
//                // -----------------------------------------------------
//                // 6. Delete local database
//                // -----------------------------------------------------
//
//                AppDatabase.closeDatabase()
//
//                val databaseFile =
//                    getDatabasePath(
//                        "dementia_database"
//                    )
//
//                if (databaseFile.exists()) {
//                    require(databaseFile.delete()) {
//                        "Unable to delete local database."
//                    }
//                }
//
//                Log.d(
//                    tag,
//                    "6. Local database deleted = ${!databaseFile.exists()}"
//                )
//
//                // -----------------------------------------------------
//                // 7. Delete local people directory
//                // -----------------------------------------------------
//
//                if (peopleDirectory.exists()) {
//
//                    peopleDirectory.deleteRecursively()
//
//                }
//
//                Log.d(
//                    tag,
//                    "7. Local people directory deleted = ${!peopleDirectory.exists()}"
//                )
//
//                // -----------------------------------------------------
//                // 8. Restore encrypted backup
//                // -----------------------------------------------------
//                var wrongPasswordRejected = false
//                val wrongPassword =
//                    "WrongPassword123!".toCharArray()
//
//                try {
//
//                    backupPackageManager.restoreBackup(
//                        packageDirectory =
//                            backupPackageDirectory,
//                        password =
//                            wrongPassword
//                    )
//
//                    // If we reach here, something is wrong.
//                    Log.e(
//                        tag,
//                        "8. SECURITY TEST FAILED: WRONG PASSWORD WAS ACCEPTED"
//                    )
//
//                } catch (e: Exception) {
//
//                    Log.d(
//                        tag,
//                        "8. Wrong password correctly rejected"
//                    )
//                    wrongPasswordRejected = true
//
//                    Log.d(
//                        tag,
//                        "Wrong password error = ${e.message}"
//                    )
//
//                } finally {
//
//                    wrongPassword.fill('\u0000')
//                }
//
//                // -----------------------------------------------------
//                // 9. Verify wrong password did NOT restore the database
//                // -----------------------------------------------------
//
//                val databaseStillAbsent =
//                    !databaseFile.exists()
//
//                Log.d(
//                    tag,
//                    "9. Database still absent after wrong password = $databaseStillAbsent"
//                )
//
//                // -----------------------------------------------------
//                // 10. Verify people directory was not restored
//                // -----------------------------------------------------
//
//                val peopleStillAbsent =
//                    !peopleDirectory.exists()
//
//                Log.d(
//                    tag,
//                    "10. People directory still absent after wrong password = $peopleStillAbsent"
//                )
//
//                // -----------------------------------------------------
//                // 11. Verify encrypted backup still exists
//                // -----------------------------------------------------
//
//                val backupStillExists =
//                    encryptedBackup.exists() &&
//                            encryptedBackup.length() > 0
//
//                val metadataStillExists =
//                    metadataFile.exists() &&
//                            metadataFile.length() > 0
//
//                Log.d(
//                    tag,
//                    "11. Encrypted backup still exists = $backupStillExists"
//                )
//
//                Log.d(
//                    tag,
//                    "12. Metadata still exists = $metadataStillExists"
//                )
//
//                // -----------------------------------------------------
//                // 12. Final security verification
//                // -----------------------------------------------------
//
//                val securityTestPassed =
//                    wrongPasswordRejected &&
//                            databaseStillAbsent &&
//                            peopleStillAbsent &&
//                            backupStillExists &&
//                            metadataStillExists
//
//                Log.d(
//                    tag,
//                    "13. Wrong password rejected = $wrongPasswordRejected"
//                )
//
//                Log.d(
//                    tag,
//                    "14. Local database remained absent = $databaseStillAbsent"
//                )
//
//                Log.d(
//                    tag,
//                    "15. Backup remained intact = $backupStillExists"
//                )
//
//                Log.d(
//                    tag,
//                    "========== WRONG PASSWORD SECURITY TEST PASSED = $securityTestPassed =========="
//                )
//
//            } catch (e: Exception) {
//
//                Log.e(
//                    tag,
//                    "========== BACKUP + RESTORE TEST FAILED ==========",
//                    e
//                )
//            }
//
//        }.start()
//    }
//}


package com.example.evoke

import android.app.Application
import com.example.evoke.database.AppDatabase

class MyApplication : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()

        System.loadLibrary("sqlcipher")
    }
}