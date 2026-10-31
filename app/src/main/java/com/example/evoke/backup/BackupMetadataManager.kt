package com.example.evoke.backup

import android.content.Context
import android.util.Base64
import com.example.evoke.security.BackupDatabaseKeyManager
import com.example.evoke.security.BackupKeyManager
import com.example.evoke.security.BackupKeyWrapper
import com.example.evoke.security.DatabaseKeyManager
import com.example.evoke.security.PasswordKeyManager

class BackupMetadataManager(
    private val context: Context
) {

    companion object {
        private const val FORMAT_VERSION = 1
    }

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

    fun createMetadata(
        password: CharArray
    ): BackupMetadata {

        require(password.isNotEmpty()) {
            "Backup password cannot be empty."
        }

        var backupKey: ByteArray? = null
        var databaseKey: ByteArray? = null
        var kek: ByteArray? = null

        try {

            // Get the existing BackupKey.
            backupKey =
                backupKeyManager.getBackupKey()

            // Get the local SQLCipher DatabaseKey.
            databaseKey =
                databaseKeyManager.getDatabaseKeyForBackup()

            // Derive the KEK from the caregiver's password.
            val result =
                passwordKeyManager.createKey(
                    password
                )

            kek = result.first

            val kdfParameters =
                result.second

            // Wrap the BackupKey using the password-derived KEK.
            val wrapped =
                backupKeyWrapper.wrapBackupKey(
                    backupKey = backupKey,
                    kek = kek
                )

            // Encrypt the SQLCipher DatabaseKey using the BackupKey.
            val encryptedDatabaseKey =
                backupDatabaseKeyManager.encryptDatabaseKey(
                    databaseKey = databaseKey,
                    backupKey = backupKey
                )

            return BackupMetadata(

                formatVersion =
                    FORMAT_VERSION,

                kdfVersion =
                    kdfParameters.version,

                salt =
                    Base64.encodeToString(
                        kdfParameters.salt,
                        Base64.NO_WRAP
                    ),

                memoryKb =
                    kdfParameters.memoryKb,

                iterations =
                    kdfParameters.iterations,

                parallelism =
                    kdfParameters.parallelism,

                wrappedBackupKey =
                    wrapped.encryptedKey,

                backupKeyIv =
                    wrapped.iv,

                encryptedDatabaseKey =
                    encryptedDatabaseKey.encryptedKey,

                databaseKeyIv =
                    encryptedDatabaseKey.iv
            )

        } finally {
            backupKey?.fill(0)
            databaseKey?.fill(0)
            kek?.fill(0)
        }
    }
}
