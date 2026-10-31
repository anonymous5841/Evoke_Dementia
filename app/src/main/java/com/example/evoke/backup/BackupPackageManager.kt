package com.example.evoke.backup

import android.content.Context
import android.util.Base64
import com.example.evoke.database.AppDatabase
import com.example.evoke.security.BackupEncryptionManager
import com.example.evoke.security.BackupKeyManager
import com.example.evoke.security.BackupKeyWrapper
import com.example.evoke.security.DatabaseKeyManager
import com.example.evoke.security.PasswordKeyManager
import com.example.evoke.security.BackupDatabaseKeyManager
import org.json.JSONObject
import java.io.File

class BackupPackageManager(
    private val context: Context
) {

    companion object {
        private const val BACKUP_PACKAGE_DIRECTORY =
            "backup_package"

        private const val ARCHIVE_FILE_NAME =
            "backup.zip"

        private const val ENCRYPTED_BACKUP_FILE_NAME =
            "backup.enc"

        private const val METADATA_FILE_NAME =
            "metadata.json"
    }

    private val archiveManager =
        BackupArchiveManager(context)

    private val encryptionManager =
        BackupEncryptionManager()

    private val metadataManager =
        BackupMetadataManager(context)

    private val backupKeyManager =
        BackupKeyManager(context)

    private val databaseKeyManager =
        DatabaseKeyManager(context)

    private val passwordKeyManager =
        PasswordKeyManager()

    private val backupKeyWrapper =
        BackupKeyWrapper()

    private val backupDatabaseKeyManager =
        BackupDatabaseKeyManager()

    /**
     * Creates a complete encrypted backup package.
     *
     * The returned directory contains only:
     *
     * metadata.json
     * backup.enc
     *
     * The unencrypted ZIP exists only temporarily and is deleted.
     */
    fun createBackup(
        password: CharArray
    ): File {

        require(password.isNotEmpty()) {
            "Backup password cannot be empty."
        }

        val packageDirectory =
            File(
                context.cacheDir,
                "${BACKUP_PACKAGE_DIRECTORY}_${System.currentTimeMillis()}"
            )

        packageDirectory.mkdirs()

        val temporaryArchive =
            File(
                packageDirectory,
                ARCHIVE_FILE_NAME
            )

        val encryptedBackup =
            File(
                packageDirectory,
                ENCRYPTED_BACKUP_FILE_NAME
            )

        val metadataFile =
            File(
                packageDirectory,
                METADATA_FILE_NAME
            )

        try {

            // ---------------------------------------------------------
            // 1. Make sure Room/SQLCipher is closed.
            //
            // This is important because the database may still have
            // changes in WAL files.
            // ---------------------------------------------------------

//            AppDatabase.closeDatabase()
//
//            // ---------------------------------------------------------
//            // 2. Create the unencrypted temporary ZIP.
//            //
//            // This contains:
//            // database/dementia_database
//            // people/*
//            // ---------------------------------------------------------
//
//            archiveManager.createBackupArchive(
//                temporaryArchive
//            )
// Make sure the Room/SQLCipher database is physically created
// before closing it for backup.
            AppDatabase
                .getDatabase(context)
                .openHelper
                .writableDatabase

            AppDatabase.checkpointDatabase()

            AppDatabase.closeDatabase()

            archiveManager.createBackupArchive(
                temporaryArchive
            )
            // ---------------------------------------------------------
            // 3. Get the local BackupKey.
            // ---------------------------------------------------------

            val backupKey =
                backupKeyManager.getBackupKey()

            try {

                // -----------------------------------------------------
                // 4. Encrypt the ZIP using AES-256-GCM.
                // -----------------------------------------------------

                encryptionManager.encryptFile(
                    inputFile = temporaryArchive,
                    outputFile = encryptedBackup,
                    backupKey = backupKey
                )

                // -----------------------------------------------------
                // 5. Create cryptographic metadata.
                //
                // This contains:
                // - Argon2 parameters
                // - wrapped BackupKey
                // - encrypted DatabaseKey
                // - IVs
                // -----------------------------------------------------

                val metadata =
                    metadataManager.createMetadata(
                        password
                    )

                // -----------------------------------------------------
                // 6. Convert metadata to JSON.
                // -----------------------------------------------------

                val metadataJson =
                    JSONObject().apply {

                        put(
                            "formatVersion",
                            metadata.formatVersion
                        )

                        put(
                            "kdfVersion",
                            metadata.kdfVersion
                        )

                        put(
                            "salt",
                            metadata.salt
                        )

                        put(
                            "memoryKb",
                            metadata.memoryKb
                        )

                        put(
                            "iterations",
                            metadata.iterations
                        )

                        put(
                            "parallelism",
                            metadata.parallelism
                        )

                        put(
                            "wrappedBackupKey",
                            metadata.wrappedBackupKey
                        )

                        put(
                            "backupKeyIv",
                            metadata.backupKeyIv
                        )

                        put(
                            "encryptedDatabaseKey",
                            metadata.encryptedDatabaseKey
                        )

                        put(
                            "databaseKeyIv",
                            metadata.databaseKeyIv
                        )
                    }

                metadataFile.writeText(
                    metadataJson.toString()
                )

                // -----------------------------------------------------
                // 7. Delete the unencrypted ZIP.
                //
                // The final package must never contain it.
                // -----------------------------------------------------

                if (temporaryArchive.exists()) {
                    require(
                        temporaryArchive.delete()
                    ) {
                        "Unable to delete temporary backup archive."
                    }
                }

            } finally {

                backupKey.fill(0)
            }

            // ---------------------------------------------------------
            // 8. Verify final package.
            // ---------------------------------------------------------

            require(
                encryptedBackup.exists()
            ) {
                "Encrypted backup was not created."
            }

            require(
                metadataFile.exists()
            ) {
                "Backup metadata was not created."
            }

            require(
                !temporaryArchive.exists()
            ) {
                "Unencrypted backup archive still exists."
            }

            return packageDirectory

        } catch (exception: Exception) {

            // Never leave an unencrypted archive behind.
            temporaryArchive.delete()

            // Delete incomplete package.
            packageDirectory.deleteRecursively()

            throw exception
        }
    }

    /**
     * Reads and validates metadata from a backup package.
     */
    private fun readMetadata(
        metadataFile: File
    ): BackupMetadata {

        require(metadataFile.exists()) {
            "Backup metadata file does not exist."
        }

        val json =
            JSONObject(
                metadataFile.readText()
            )

        val formatVersion =
            json.getInt("formatVersion")

        require(formatVersion == 1) {
            "Unsupported backup format version: $formatVersion"
        }

        return BackupMetadata(
            formatVersion = formatVersion,

            kdfVersion =
                json.getInt("kdfVersion"),

            salt =
                json.getString("salt"),

            memoryKb =
                json.getInt("memoryKb"),

            iterations =
                json.getInt("iterations"),

            parallelism =
                json.getInt("parallelism"),

            wrappedBackupKey =
                json.getString("wrappedBackupKey"),

            backupKeyIv =
                json.getString("backupKeyIv"),

            encryptedDatabaseKey =
                json.getString("encryptedDatabaseKey"),

            databaseKeyIv =
                json.getString("databaseKeyIv")
        )
    }

    /**
     * Restores a complete backup package.
     *
     * The password is required to recover the BackupKey.
     */
    fun restoreBackup(
        packageDirectory: File,
        password: CharArray
    ) {

        require(password.isNotEmpty()) {
            "Backup password cannot be empty."
        }

        require(packageDirectory.exists()) {
            "Backup package does not exist."
        }

        val metadataFile =
            File(
                packageDirectory,
                METADATA_FILE_NAME
            )

        val encryptedBackup =
            File(
                packageDirectory,
                ENCRYPTED_BACKUP_FILE_NAME
            )

        val decryptedArchive =
            File(
                packageDirectory,
                "restored.zip"
            )

        var kek: ByteArray? = null
        var backupKey: ByteArray? = null
        var databaseKey: ByteArray? = null

        try {

            // ---------------------------------------------------------
            // 1. Read metadata.
            // ---------------------------------------------------------

            val metadata =
                readMetadata(
                    metadataFile
                )

            // ---------------------------------------------------------
            // 2. Recreate the KEK from the caregiver password.
            // ---------------------------------------------------------

            val kdfParameters =
                PasswordKeyManager.KdfParameters(
                    version = metadata.kdfVersion,

                    salt =
                        Base64.decode(
                            metadata.salt,
                            Base64.NO_WRAP
                        ),

                    memoryKb =
                        metadata.memoryKb,

                    iterations =
                        metadata.iterations,

                    parallelism =
                        metadata.parallelism
                )

            kek =
                passwordKeyManager.deriveKey(
                    password = password,
                    parameters = kdfParameters
                )

            // ---------------------------------------------------------
            // 3. Recover BackupKey.
            // ---------------------------------------------------------

            backupKey =
                backupKeyWrapper.unwrapBackupKey(
                    encryptedKey =
                        metadata.wrappedBackupKey,

                    iv =
                        metadata.backupKeyIv,

                    kek =
                        kek
                )

            // ---------------------------------------------------------
            // 4. Recover the SQLCipher DatabaseKey.
            // ---------------------------------------------------------

            databaseKey =
                backupDatabaseKeyManager.decryptDatabaseKey(
                    encryptedKey =
                        metadata.encryptedDatabaseKey,

                    iv =
                        metadata.databaseKeyIv,

                    backupKey =
                        backupKey
                )

            // ---------------------------------------------------------
            // 6. Decrypt backup.enc → restored.zip
            // ---------------------------------------------------------

            encryptionManager.decryptFile(
                encryptedFile = encryptedBackup,
                outputFile = decryptedArchive,
                backupKey = backupKey
            )

            // ---------------------------------------------------------
            // 7. Close any existing database before replacing it.
            // ---------------------------------------------------------

            AppDatabase.closeDatabase()

            // ---------------------------------------------------------
            // 8. Extract ZIP.
            // ---------------------------------------------------------

            val extractedDirectory =
                archiveManager.extractBackupArchive(
                    decryptedArchive
                )

            // ---------------------------------------------------------
            // 9. Store recovered DatabaseKey in the local
            //    Android Keystore-protected DatabaseKeyManager.
            //
            //    This happens only after the encrypted backup has
            //    successfully decrypted and the ZIP has been validated.
            // ---------------------------------------------------------

            databaseKeyManager.restoreDatabaseKey(
                databaseKey
            )
            try {

                // -----------------------------------------------------
                // 9. Replace local database + people images.
                // -----------------------------------------------------

                archiveManager.commitRestoredBackup(
                    extractedDirectory
                )

            } finally {

                archiveManager.deleteTemporaryBackup(
                    extractedDirectory
                )
            }

            // ---------------------------------------------------------
            // 10. Delete decrypted temporary ZIP.
            // ---------------------------------------------------------

            if (decryptedArchive.exists()) {
                decryptedArchive.delete()
            }

        } catch (exception: Exception) {

            decryptedArchive.delete()

            throw exception

        } finally {
            kek?.fill(0)
            backupKey?.fill(0)
            databaseKey?.fill(0)
        }
    }
    fun getEncryptedBackupFile(
        packageDirectory: File
    ): File {

        return File(
            packageDirectory,
            ENCRYPTED_BACKUP_FILE_NAME
        )
    }

    fun getMetadataFile(
        packageDirectory: File
    ): File {

        return File(
            packageDirectory,
            METADATA_FILE_NAME
        )
    }
    fun hasLocalBackupData(): Boolean {
        return archiveManager.hasLocalBackupData()
    }
}