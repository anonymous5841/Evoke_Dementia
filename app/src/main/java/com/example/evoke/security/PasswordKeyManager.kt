package com.example.evoke.security

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom

class PasswordKeyManager {

    companion object {
        const val KDF_VERSION = 1

        private const val SALT_SIZE = 16
        private const val KEY_SIZE = 32

        // Argon2id parameters — version 1 of our backup format.
        private const val MEMORY_KB = 19 * 1024
        private const val ITERATIONS = 2
        private const val PARALLELISM = 1
    }

    data class KdfParameters(
        val version: Int,
        val salt: ByteArray,
        val memoryKb: Int,
        val iterations: Int,
        val parallelism: Int
    )

    /**
     * Used when backup is enabled for the first time.
     *
     * Generates a fresh random salt and derives a 32-byte KEK
     * from the caregiver's password.
     */
    fun createKey(
        password: CharArray
    ): Pair<ByteArray, KdfParameters> {

        require(password.isNotEmpty()) {
            "Backup password cannot be empty."
        }

        val salt =
            ByteArray(SALT_SIZE).also {
                SecureRandom().nextBytes(it)
            }

        val parameters =
            KdfParameters(
                version = KDF_VERSION,
                salt = salt,
                memoryKb = MEMORY_KB,
                iterations = ITERATIONS,
                parallelism = PARALLELISM
            )

        val kek =
            deriveKey(
                password = password,
                parameters = parameters
            )

        return kek to parameters
    }

    /**
     * Used during restore.
     *
     * The salt and KDF parameters come from backup metadata,
     * while the caregiver supplies the password.
     */
    fun deriveKey(
        password: CharArray,
        parameters: KdfParameters
    ): ByteArray {

        require(password.isNotEmpty()) {
            "Backup password cannot be empty."
        }

        require(parameters.version == KDF_VERSION) {
            "Unsupported backup KDF version: ${parameters.version}"
        }

        require(parameters.salt.isNotEmpty()) {
            "Backup salt is missing."
        }

        val argon2Parameters =
            Argon2Parameters.Builder(
                Argon2Parameters.ARGON2_id
            )
                .withVersion(
                    Argon2Parameters.ARGON2_VERSION_13
                )
                .withSalt(parameters.salt)
                .withMemoryAsKB(parameters.memoryKb)
                .withIterations(parameters.iterations)
                .withParallelism(parameters.parallelism)
                .build()

        val generator =
            Argon2BytesGenerator()

        generator.init(argon2Parameters)

        val output =
            ByteArray(KEY_SIZE)

        try {

            generator.generateBytes(
                password,
                output
            )

            return output

        } finally {

            argon2Parameters.clear()
        }
    }
}