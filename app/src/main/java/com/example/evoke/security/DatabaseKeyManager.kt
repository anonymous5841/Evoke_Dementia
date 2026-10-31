package com.example.evoke.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DatabaseKeyManager(
    private val context: Context
) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS =
            "dementia_database_master_key"

        private const val PREFS_NAME =
            "database_security"

        private const val ENCRYPTED_KEY =
            "encrypted_database_key"

        private const val IV =
            "database_key_iv"

        private const val AES_MODE =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH =
            128

        private const val DATABASE_KEY_SIZE =
            32
    }

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    /**
     * Gets the local SQLCipher database key.
     *
     * If this is the first run, a new random 32-byte
     * database key is generated and protected using
     * Android Keystore.
     */
    fun getDatabaseKey(): ByteArray {

        val encryptedKey =
            preferences.getString(
                ENCRYPTED_KEY,
                null
            )

        val iv =
            preferences.getString(
                IV,
                null
            )

        return if (
            encryptedKey == null ||
            iv == null
        ) {

            val databaseKey =
                generateDatabaseKey()

            saveDatabaseKey(
                databaseKey
            )

            databaseKey

        } else {

            decryptDatabaseKey(
                encryptedKey =
                    Base64.decode(
                        encryptedKey,
                        Base64.DEFAULT
                    ),
                iv =
                    Base64.decode(
                        iv,
                        Base64.DEFAULT
                    )
            )
        }
    }

    /**
     * Returns the current database key for the
     * optional backup route.
     *
     * This does NOT make the local key portable by itself.
     * It simply obtains the same key that SQLCipher uses.
     */
    fun getDatabaseKeyForBackup(): ByteArray {
        return getDatabaseKey()
    }

    /**
     * Restores a database key obtained from a backup.
     *
     * This is used only when restoring a backup onto
     * a new device.
     *
     * The supplied database key is NOT stored directly.
     * It is encrypted using a newly generated local
     * Android Keystore master key.
     */
    fun restoreDatabaseKey(
        databaseKey: ByteArray
    ) {

        require(
            databaseKey.size == DATABASE_KEY_SIZE
        ) {
            "Database key must be 32 bytes."
        }

        /*
         * Remove any existing local key protection first.
         *
         * This is important when restoring onto an
         * existing installation.
         */
        deleteStoredDatabaseKey()

        /*
         * saveDatabaseKey() will create a new local
         * Android Keystore master key and protect
         * the restored database key with it.
         */
        saveDatabaseKey(
            databaseKey
        )
    }

    private fun generateDatabaseKey(): ByteArray {

        val key =
            ByteArray(
                DATABASE_KEY_SIZE
            )

        SecureRandom().nextBytes(key)

        return key
    }

    private fun saveDatabaseKey(
        databaseKey: ByteArray
    ) {

        require(
            databaseKey.size == DATABASE_KEY_SIZE
        ) {
            "Database key must be 32 bytes."
        }

        val cipher =
            Cipher.getInstance(
                AES_MODE
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateMasterKey()
        )

        val encrypted =
            cipher.doFinal(
                databaseKey
            )

        val iv =
            cipher.iv

        preferences.edit()
            .putString(
                ENCRYPTED_KEY,
                Base64.encodeToString(
                    encrypted,
                    Base64.NO_WRAP
                )
            )
            .putString(
                IV,
                Base64.encodeToString(
                    iv,
                    Base64.NO_WRAP
                )
            )
            .apply()
    }

    private fun decryptDatabaseKey(
        encryptedKey: ByteArray,
        iv: ByteArray
    ): ByteArray {

        val cipher =
            Cipher.getInstance(
                AES_MODE
            )

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

        val databaseKey =
            cipher.doFinal(
                encryptedKey
            )

        require(
            databaseKey.size == DATABASE_KEY_SIZE
        ) {
            "Invalid database key."
        }

        return databaseKey
    }

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

    /**
     * Removes only the locally stored encrypted
     * database key and its Keystore master key.
     *
     * Used internally during restoration.
     */
    private fun deleteStoredDatabaseKey() {

        preferences.edit()
            .remove(ENCRYPTED_KEY)
            .remove(IV)
            .apply()

        val keyStore =
            KeyStore.getInstance(
                KEYSTORE_PROVIDER
            ).apply {
                load(null)
            }

        if (
            keyStore.containsAlias(
                MASTER_KEY_ALIAS
            )
        ) {
            keyStore.deleteEntry(
                MASTER_KEY_ALIAS
            )
        }
    }

    /**
     * Completely deletes the local database key.
     *
     * Used when clearing all local application data.
     */
    fun deleteDatabaseKey() {
        deleteStoredDatabaseKey()
    }
}