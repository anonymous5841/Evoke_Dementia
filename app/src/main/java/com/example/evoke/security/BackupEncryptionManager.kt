package com.example.evoke.security

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BackupEncryptionManager {

    companion object {
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val AES_KEY_SIZE = 32
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12
    }

    /**
     * Encrypts [inputFile] using AES-256-GCM and writes the result
     * to [outputFile].
     *
     * File format:
     *
     * [12-byte IV][encrypted data + GCM authentication tag]
     */
    fun encryptFile(
        inputFile: File,
        outputFile: File,
        backupKey: ByteArray
    ) {
        require(backupKey.size == AES_KEY_SIZE) {
            "Backup key must be 32 bytes."
        }

        require(inputFile.exists()) {
            "Input file does not exist: ${inputFile.absolutePath}"
        }

        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)

        val secretKey =
            SecretKeySpec(
                backupKey,
                "AES"
            )

        val cipher =
            Cipher.getInstance(AES_MODE)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            secretKey,
            GCMParameterSpec(
                GCM_TAG_LENGTH,
                iv
            )
        )

        FileInputStream(inputFile).use { input ->
            FileOutputStream(outputFile).use { output ->

                // Store IV at the beginning of the encrypted file.
                output.write(iv)

                val buffer = ByteArray(8192)

                while (true) {

                    val bytesRead =
                        input.read(buffer)

                    if (bytesRead == -1) {
                        break
                    }

                    val encryptedChunk =
                        cipher.update(
                            buffer,
                            0,
                            bytesRead
                        )

                    if (encryptedChunk != null) {
                        output.write(encryptedChunk)
                    }
                }

                val finalBytes =
                    cipher.doFinal()

                output.write(finalBytes)
            }
        }
    }

    /**
     * Decrypts an AES-256-GCM encrypted backup file.
     *
     * The first 12 bytes of the file contain the IV.
     */
    fun decryptFile(
        encryptedFile: File,
        outputFile: File,
        backupKey: ByteArray
    ) {
        require(backupKey.size == AES_KEY_SIZE) {
            "Backup key must be 32 bytes."
        }

        require(encryptedFile.exists()) {
            "Encrypted file does not exist: ${encryptedFile.absolutePath}"
        }

        val secretKey =
            SecretKeySpec(
                backupKey,
                "AES"
            )

        FileInputStream(encryptedFile).use { input ->

            val iv = ByteArray(IV_SIZE)

            val ivBytesRead =
                input.read(iv)

            require(ivBytesRead == IV_SIZE) {
                "Invalid encrypted backup: IV is missing or incomplete."
            }

            val cipher =
                Cipher.getInstance(AES_MODE)

            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(
                    GCM_TAG_LENGTH,
                    iv
                )
            )

            val temporaryOutputFile =
                File(
                    outputFile.parentFile,
                    "${outputFile.name}.tmp"
                )

            try {
                FileOutputStream(
                    temporaryOutputFile
                ).use { output ->

                    val buffer =
                        ByteArray(8192)

                    while (true) {
                        val bytesRead =
                            input.read(buffer)

                        if (bytesRead == -1) {
                            break
                        }

                        val decryptedChunk =
                            cipher.update(
                                buffer,
                                0,
                                bytesRead
                            )

                        if (decryptedChunk != null) {
                            output.write(
                                decryptedChunk
                            )
                        }
                    }

                    // Authentication is completed here.
                    val finalBytes =
                        cipher.doFinal()

                    output.write(finalBytes)

                    output.flush()
                }

                // Only expose the decrypted file after
                // successful GCM authentication.
                if (outputFile.exists()) {
                    outputFile.delete()
                }

                require(
                    temporaryOutputFile.renameTo(
                        outputFile
                    )
                ) {
                    "Unable to finalize decrypted backup."
                }

            } catch (exception: Exception) {
                temporaryOutputFile.delete()
                throw exception
            }
        }
    }
}