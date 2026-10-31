package com.example.evoke.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupDatabaseKeyManager {

    companion object {
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12
    }

    data class EncryptedDatabaseKey(
        val encryptedKey: String,
        val iv: String
    )

    /**
     * Encrypts the SQLCipher database key using the backup key.
     */
    fun encryptDatabaseKey(
        databaseKey: ByteArray,
        backupKey: ByteArray
    ): EncryptedDatabaseKey {

        require(databaseKey.size == 32) {
            "Database key must be 32 bytes."
        }

        require(backupKey.size == 32) {
            "Backup key must be 32 bytes."
        }

        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_MODE)

        val secretKey = SecretKeySpec(
            backupKey,
            "AES"
        )

        val spec = GCMParameterSpec(
            GCM_TAG_LENGTH,
            iv
        )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
            spec
        )

        val encryptedKey = cipher.doFinal(databaseKey)

        return EncryptedDatabaseKey(
            encryptedKey = Base64.encodeToString(
                encryptedKey,
                Base64.NO_WRAP
            ),
            iv = Base64.encodeToString(
                iv,
                Base64.NO_WRAP
            )
        )
    }

    /**
     * Decrypts the SQLCipher database key using the backup key.
     */
    fun decryptDatabaseKey(
        encryptedKey: String,
        iv: String,
        backupKey: ByteArray
    ): ByteArray {

        require(backupKey.size == 32) {
            "Backup key must be 32 bytes."
        }

        val encryptedBytes = Base64.decode(
            encryptedKey,
            Base64.NO_WRAP
        )

        val ivBytes = Base64.decode(
            iv,
            Base64.NO_WRAP
        )

        val cipher = Cipher.getInstance(AES_MODE)

        val secretKey = SecretKeySpec(
            backupKey,
            "AES"
        )

        val spec = GCMParameterSpec(
            GCM_TAG_LENGTH,
            ivBytes
        )

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            spec
        )

        val databaseKey = cipher.doFinal(encryptedBytes)

        require(databaseKey.size == 32) {
            "Invalid decrypted database key."
        }

        return databaseKey
    }
}