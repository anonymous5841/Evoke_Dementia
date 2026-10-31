package com.example.evoke.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupKeyWrapper {

    companion object {
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12
        private const val KEY_SIZE = 32
    }

    data class WrappedBackupKey(
        val encryptedKey: String,
        val iv: String
    )

    /**
     * Encrypts the random BackupKey using the password-derived KEK.
     *
     * The KEK itself is never stored.
     */
    fun wrapBackupKey(
        backupKey: ByteArray,
        kek: ByteArray
    ): WrappedBackupKey {

        require(backupKey.size == KEY_SIZE) {
            "Backup key must be 32 bytes."
        }

        require(kek.size == KEY_SIZE) {
            "KEK must be 32 bytes."
        }

        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(AES_MODE)

        val secretKey =
            SecretKeySpec(kek, "AES")

        val spec =
            GCMParameterSpec(
                GCM_TAG_LENGTH,
                iv
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
            spec
        )

        val encryptedKey =
            cipher.doFinal(backupKey)

        return WrappedBackupKey(
            encryptedKey =
                Base64.encodeToString(
                    encryptedKey,
                    Base64.NO_WRAP
                ),
            iv =
                Base64.encodeToString(
                    iv,
                    Base64.NO_WRAP
                )
        )
    }

    /**
     * Recovers the BackupKey using the password-derived KEK.
     *
     * An incorrect KEK/password will cause AES-GCM authentication
     * to fail rather than returning an incorrect BackupKey.
     */
    fun unwrapBackupKey(
        encryptedKey: String,
        iv: String,
        kek: ByteArray
    ): ByteArray {

        require(kek.size == KEY_SIZE) {
            "KEK must be 32 bytes."
        }

        val encryptedBytes =
            Base64.decode(
                encryptedKey,
                Base64.NO_WRAP
            )

        val ivBytes =
            Base64.decode(
                iv,
                Base64.NO_WRAP
            )

        require(ivBytes.size == IV_SIZE) {
            "Invalid IV."
        }

        val cipher =
            Cipher.getInstance(AES_MODE)

        val secretKey =
            SecretKeySpec(kek, "AES")

        val spec =
            GCMParameterSpec(
                GCM_TAG_LENGTH,
                ivBytes
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey,
            spec
        )

        val backupKey =
            cipher.doFinal(encryptedBytes)

        require(backupKey.size == KEY_SIZE) {
            "Invalid decrypted backup key."
        }

        return backupKey
    }
}