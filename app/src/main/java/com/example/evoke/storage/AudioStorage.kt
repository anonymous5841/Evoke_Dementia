package com.example.evoke.storage

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

/**
 * Stores filtered conversation audio in the application's private storage.
 *
 * Audio files are stored separately from the database.
 * Room stores only their relative paths in PendingConversation.audioPath.
 *
 * The original audio extension is preserved.
 * Example paths:
 * audio/8b2f...a91.m4a
 * audio/8b2f...a91.wav
 * audio/8b2f...a91.mp3
 *
 * In-progress copies are written as ".<name>.part". The leading dot means
 * they can never match AUDIO_FILE_NAME_REGEX, so they are never served,
 * never backed up and are removed by [deleteTemporaryFiles].
 */
class AudioStorage(
    context: Context
) {

    companion object {
        private const val AUDIO_DIRECTORY = "audio"
        private const val TEMPORARY_SUFFIX = ".part"

        private val AUDIO_FILE_NAME_REGEX = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(\\.[A-Za-z0-9]+)?"
        )
    }

    private val appFilesDirectory = context.filesDir

    // Creates the audio directory when it is first needed.
    private val audioDirectory: File by lazy {
        File(appFilesDirectory, AUDIO_DIRECTORY).apply {
            if (!exists()) {
                check(mkdirs() || exists()) {
                    "Unable to create audio storage directory."
                }
            }

            check(isDirectory) {
                "Audio storage path is not a directory."
            }
        }
    }

    /**
     * Copies a filtered audio file into permanent app storage.
     *
     * Call this BEFORE the Room transaction that inserts the
     * Conversation and PendingConversation records.
     *
     * Once the returned path is handed to RecognitionRepository or
     * PersonRepository, the repository owns the file: it deletes it if the
     * transaction fails or if the recording cannot be queued.
     *
     * Preserves the source file's extension.
     *
     * Returns a relative path to store in PendingConversation.audioPath.
     */
    fun saveFilteredAudio(sourceFile: File): String {

        require(sourceFile.exists() && sourceFile.isFile) {
            "Source audio file does not exist."
        }

        require(sourceFile.length() > 0) {
            "Source audio file is empty."
        }

        // Preserve the original extension, if present.
        val extension = sourceFile.extension

        require(
            extension.isEmpty() ||
                    extension.matches(Regex("[A-Za-z0-9]+"))
        ) {
            "Invalid audio file extension."
        }

        require(!extension.equals("tmp", ignoreCase = true) &&
                !extension.equals("part", ignoreCase = true)) {
            "Temporary files cannot be stored as audio."
        }

        // Generate a unique filename to avoid collisions.
        val fileName = buildString {
            append(UUID.randomUUID())

            if (extension.isNotEmpty()) {
                append(".")
                append(extension)
            }
        }

        require(AUDIO_FILE_NAME_REGEX.matches(fileName)) {
            "Invalid generated audio filename."
        }

        val destinationFile = File(audioDirectory, fileName)
        val temporaryFile = File(audioDirectory, ".$fileName$TEMPORARY_SUFFIX")

        try {

            // Copy into a temporary file first.
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }

            // Verify that the complete file was copied.
            check(temporaryFile.length() == sourceFile.length()) {
                "Audio file copy was incomplete."
            }

            // Rename the temporary file to its permanent filename.
            check(temporaryFile.renameTo(destinationFile)) {
                "Unable to finalize stored audio file."
            }

            // Return a relative path for Room.
            return "$AUDIO_DIRECTORY/$fileName"

        } catch (exception: Exception) {

            // Clean up incomplete files if copying fails.
            temporaryFile.delete()
            destinationFile.delete()

            throw exception
        }
    }

    /**
     * Resolves a stored relative path into a file inside audio/.
     *
     * Paths outside the audio directory are rejected.
     * Only UUID-based filenames with optional extensions are accepted.
     */
    fun getAudioFile(audioPath: String): File? {

        if (audioPath.isBlank()) {
            return null
        }

        // Normalize Windows-style separators.
        val normalizedPath = audioPath.replace('\\', '/')
        val expectedPrefix = "$AUDIO_DIRECTORY/"

        if (!normalizedPath.startsWith(expectedPrefix)) {
            return null
        }

        val fileName = normalizedPath.removePrefix(expectedPrefix)

        // Accept only filenames generated by this class.
        if (!isStoredAudioName(fileName)) {
            return null
        }

        val file = File(audioDirectory, fileName)

        return try {

            // Ensure the resolved file stays inside audio/.
            val directoryPath =
                audioDirectory.canonicalPath + File.separator

            val canonicalPath = file.canonicalPath

            if (canonicalPath.startsWith(directoryPath)) {
                file
            } else {
                null
            }

        } catch (exception: Exception) {
            null
        }
    }

    /**
     * Returns the stored audio file if it exists.
     */
    fun loadAudio(audioPath: String): File? {

        val file = getAudioFile(audioPath)
            ?: return null

        return if (file.exists() && file.isFile) {
            file
        } else {
            null
        }
    }

    /**
     * Deletes a stored audio file using its relative path.
     *
     * Returns true if the file was deleted or did not exist.
     */
    fun deleteAudio(audioPath: String?): Boolean {

        if (audioPath.isNullOrBlank()) {
            return true
        }

        val file = getAudioFile(audioPath)
            ?: return false

        return !file.exists() || file.delete()
    }

    /**
     * Deletes leftover in-progress copies (".<name>.part", old ".tmp").
     *
     * Call once at app start.
     */
    fun deleteTemporaryFiles() {

        val directory = File(appFilesDirectory, AUDIO_DIRECTORY)

        directory.listFiles()?.forEach { file ->
            if (
                file.isFile &&
                (file.name.startsWith(".") ||
                        file.name.endsWith(TEMPORARY_SUFFIX) ||
                        file.name.endsWith(".tmp", ignoreCase = true))
            ) {
                file.delete()
            }
        }
    }

    /**
     * Deletes audio files that no database row references.
     *
     * [referencedPaths] are the relative paths currently stored in Room
     * (PendingConversationDao.getAllAudioPaths()).
     *
     * Only call this at app start, BEFORE any new recording is saved,
     * otherwise a file that is saved but not yet committed to Room
     * could be removed.
     */
    fun deleteUnreferencedAudio(referencedPaths: Collection<String>) {

        val referencedNames = referencedPaths
            .map { it.replace('\\', '/').removePrefix("$AUDIO_DIRECTORY/") }
            .toSet()

        val directory = File(appFilesDirectory, AUDIO_DIRECTORY)

        directory.listFiles()?.forEach { file ->
            if (
                file.isFile &&
                isStoredAudioName(file.name) &&
                file.name !in referencedNames
            ) {
                file.delete()
            }
        }
    }

    /**
     * Deletes every stored audio file, including leftovers.
     *
     * Used when completely resetting local application data.
     */
    fun deleteAllAudio(): Boolean {

        val directory = File(appFilesDirectory, AUDIO_DIRECTORY)

        if (!directory.exists()) {
            return true
        }

        val files = directory.listFiles()
            ?: return false

        var successful = true

        files.filter { it.isFile }.forEach { file ->
            if (!file.delete()) {
                successful = false
            }
        }

        return successful
    }

    private fun isStoredAudioName(fileName: String): Boolean {
        return AUDIO_FILE_NAME_REGEX.matches(fileName) &&
                !fileName.endsWith(".tmp", ignoreCase = true) &&
                !fileName.endsWith(TEMPORARY_SUFFIX, ignoreCase = true)
    }
}
