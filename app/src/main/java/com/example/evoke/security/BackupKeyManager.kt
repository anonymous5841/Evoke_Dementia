package com.example.evoke.security

import android.content.Context
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BackupKeyManager(
    private val context: Context
) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "dementia_backup_master_key"

        private const val PREFS_NAME = "backup_security"
        private const val ENCRYPTED_BACKUP_KEY = "encrypted_backup_key"
        private const val BACKUP_KEY_IV = "backup_key_iv"

        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private const val BACKUP_KEY_SIZE = 32
    }

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Returns the application's backup-encryption key.
     *
     * The key itself is random and is never stored as plaintext
     * in SharedPreferences.
     */
    fun getBackupKey(): ByteArray {

        val encryptedKey =
            preferences.getString(
                ENCRYPTED_BACKUP_KEY,
                null
            )

        val iv =
            preferences.getString(
                BACKUP_KEY_IV,
                null
            )

        return if (
            encryptedKey == null ||
            iv == null
        ) {

            val backupKey =
                generateBackupKey()

            saveBackupKey(backupKey)

            backupKey

        } else {

            decryptBackupKey(
                encryptedKey = Base64.decode(
                    encryptedKey,
                    Base64.DEFAULT
                ),
                iv = Base64.decode(
                    iv,
                    Base64.DEFAULT
                )
            )
        }
    }

    /**
     * Generates a cryptographically secure 256-bit backup key.
     */
    private fun generateBackupKey(): ByteArray {

        val key =
            ByteArray(BACKUP_KEY_SIZE)

        SecureRandom().nextBytes(key)

        return key
    }

    /**
     * Encrypts the backup key using an Android Keystore master key
     * before storing it in SharedPreferences.
     */
    private fun saveBackupKey(
        backupKey: ByteArray
    ) {

        val cipher =
            Cipher.getInstance(AES_MODE)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateMasterKey()
        )

        val encrypted =
            cipher.doFinal(backupKey)

        val iv =
            cipher.iv

        preferences.edit()
            .putString(
                ENCRYPTED_BACKUP_KEY,
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP
                )
            )
            .putString(
                BACKUP_KEY_IV,
                Base64.encodeToString(
                    iv,
                    Base64.NO_WRAP
                )
            )
            .apply()
    }

    /**
     * Decrypts the locally stored backup key.
     */
    private fun decryptBackupKey(
        encryptedKey: ByteArray,
        iv: ByteArray
    ): ByteArray {

        val cipher =
            Cipher.getInstance(AES_MODE)

        val spec =
            GCMParameterSpec(
                GCM_TAG_LENGTH,
                iv
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateMasterKey(),
            spec
        )

        return cipher.doFinal(encryptedKey)
    }

    /**
     * Gets the Android Keystore master key or creates it
     * if this is the first time backup security is initialized.
     */
    private fun getOrCreateMasterKey(): SecretKey {

        val keyStore =
            KeyStore.getInstance(
                KEYSTORE_PROVIDER
            ).apply {
                load(null)
            }

        val existingKey =
            keyStore.getKey(
                MASTER_KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .build()
        )

        return keyGenerator.generateKey()
    }
}