package com.example.evoke.backup

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupArchiveManager(
    private val context: Context
) {

    companion object {
        private const val DATABASE_FILE_NAME = "evoke_database"

        private const val DATABASE_ENTRY =
            "database/$DATABASE_FILE_NAME"

        private const val PEOPLE_DIRECTORY = "people"
        private const val PEOPLE_ENTRY = "people/"

        private const val AUDIO_DIRECTORY = "audio"
        private const val AUDIO_ENTRY = "audio/"

        private const val BUFFER_SIZE = 8192

        // Suffixes for the staged copy and the previous copy during a restore.
        private const val STAGED_SUFFIX = ".restore_new"
        private const val OLD_SUFFIX = ".restore_old"

        // UUID filenames with any optional alphanumeric extension.
        private val AUDIO_FILE_NAME_REGEX = Regex(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(\\.[A-Za-z0-9]+)?"
        )
    }

    private val databaseFile: File
        get() = context.getDatabasePath(DATABASE_FILE_NAME)

    // A stored audio file: UUID name, optional extension, never a temp file.
    private fun isStoredAudioName(name: String): Boolean {
        return AUDIO_FILE_NAME_REGEX.matches(name) &&
                !name.endsWith(".tmp", ignoreCase = true) &&
                !name.endsWith(".part", ignoreCase = true)
    }

    private val peopleDirectory: File
        get() = File(context.filesDir, PEOPLE_DIRECTORY)

    private val audioDirectory: File
        get() = File(context.filesDir, AUDIO_DIRECTORY)

    fun hasLocalBackupData(): Boolean {

        val databaseExists =
            databaseFile.exists() && databaseFile.length() > 0

        val peopleImagesExist =
            peopleDirectory.listFiles()?.any { it.isFile } == true

        val audioFilesExist =
            audioDirectory.listFiles()?.any { it.isFile } == true

        return databaseExists || peopleImagesExist || audioFilesExist
    }

    /**
     * Creates a ZIP archive containing:
     *
     * database/dementia_database
     *
     * people/
     *     person_1.jpg
     *     person_2.png
     *
     * audio/
     *     <UUID>.m4a
     *     <UUID>.wav
     *     <UUID>.mp3
     *
     * Audio extensions are preserved.
     * Temporary audio files are excluded.
     *
     * The resulting ZIP is NOT encrypted here.
     * BackupEncryptionManager handles encryption separately.
     */
    fun createBackupArchive(
        outputFile: File
    ) {

        require(databaseFile.exists()) {
            "Database file does not exist."
        }

        outputFile.parentFile?.mkdirs()

        ZipOutputStream(
            FileOutputStream(outputFile)
        ).use { zipOutputStream ->

            // Add database.
            addFileToZip(
                file = databaseFile,
                zipPath = DATABASE_ENTRY,
                zipOutputStream = zipOutputStream
            )

            // Add people directory.
            if (peopleDirectory.exists()) {

                zipOutputStream.putNextEntry(
                    ZipEntry(PEOPLE_ENTRY)
                )

                zipOutputStream.closeEntry()

                val peopleFiles = peopleDirectory.listFiles()
                    ?: throw IllegalStateException(
                        "Unable to read people directory."
                    )

                peopleFiles
                    .filter { it.isFile }
                    .forEach { imageFile ->

                        addFileToZip(
                            file = imageFile,
                            zipPath = "$PEOPLE_ENTRY${imageFile.name}",
                            zipOutputStream = zipOutputStream
                        )
                    }
            }

            // Add audio directory and valid stored audio files.
            if (audioDirectory.exists()) {

                zipOutputStream.putNextEntry(
                    ZipEntry(AUDIO_ENTRY)
                )

                zipOutputStream.closeEntry()

                val audioFiles = audioDirectory.listFiles()
                    ?: throw IllegalStateException(
                        "Unable to read audio directory."
                    )

                audioFiles
                    .filter {
                        it.isFile &&
                                isStoredAudioName(it.name)
                    }
                    .forEach { audioFile ->

                        addFileToZip(
                            file = audioFile,
                            zipPath = "$AUDIO_ENTRY${audioFile.name}",
                            zipOutputStream = zipOutputStream
                        )
                    }
            }
        }
    }

    /**
     * Restores a previously created backup archive.
     *
     * Extraction happens in a temporary directory first.
     * The live database and directories are not touched
     * until the archive has been successfully extracted.
     */
    fun extractBackupArchive(
        archiveFile: File
    ): File {

        require(archiveFile.exists()) {
            "Backup archive does not exist."
        }

        val temporaryDirectory =
            File(
                context.cacheDir,
                "backup_restore_${System.currentTimeMillis()}"
            )

        if (!temporaryDirectory.mkdirs()) {
            throw IllegalStateException(
                "Unable to create temporary restore directory."
            )
        }

        try {

            ZipInputStream(
                FileInputStream(archiveFile)
            ).use { zipInputStream ->

                while (true) {

                    val entry =
                        zipInputStream.nextEntry ?: break

                    validateZipEntry(entry)

                    // Resolve the entry to its canonical location.
                    val outputFile = File(
                        temporaryDirectory,
                        entry.name
                    ).canonicalFile

                    val canonicalTemporaryDirectory =
                        temporaryDirectory.canonicalFile

                    // Prevent ZIP path traversal.
                    require(
                        outputFile.path.startsWith(
                            canonicalTemporaryDirectory.path +
                                    File.separator
                        )
                    ) {
                        "Unsafe ZIP entry path detected."
                    }

                    if (entry.isDirectory) {

                        if (!outputFile.mkdirs() &&
                            !outputFile.exists()
                        ) {
                            throw IllegalStateException(
                                "Unable to create directory: ${entry.name}"
                            )
                        }

                    } else {

                        outputFile.parentFile?.mkdirs()

                        FileOutputStream(
                            outputFile
                        ).use { output ->

                            val buffer =
                                ByteArray(BUFFER_SIZE)

                            while (true) {

                                val bytesRead =
                                    zipInputStream.read(buffer)

                                if (bytesRead == -1) {
                                    break
                                }

                                output.write(
                                    buffer,
                                    0,
                                    bytesRead
                                )
                            }
                        }
                    }

                    zipInputStream.closeEntry()
                }
            }

            validateExtractedBackup(temporaryDirectory)

            return temporaryDirectory

        } catch (exception: Exception) {

            temporaryDirectory.deleteRecursively()

            throw exception
        }
    }

    /**
     * Swaps a successfully extracted backup into place so that it can be
     * undone.
     *
     * 1. The new database, people/ and audio/ are first copied next to the
     *    live locations ("*.restore_new"). If that fails, nothing live has
     *    been touched.
     * 2. The live data is then renamed to "*.restore_old" and the staged
     *    copies are renamed into place. Renames are cheap and stay on the
     *    same filesystem.
     * 3. The caller restores the database key, then calls
     *    [RestoreSwap.commit] (deletes the old data) or
     *    [RestoreSwap.rollback] (puts the old data back).
     *
     * The Room database must be closed before calling this.
     */
    fun swapInRestoredBackup(
        extractedDirectory: File
    ): RestoreSwap {

        val extractedDatabase =
            File(extractedDirectory, DATABASE_ENTRY)

        val extractedPeople =
            File(extractedDirectory, PEOPLE_DIRECTORY)

        val extractedAudio =
            File(extractedDirectory, AUDIO_DIRECTORY)

        require(extractedDatabase.exists()) {
            "Extracted backup database is missing."
        }

        val databaseDirectory =
            databaseFile.parentFile
                ?: throw IllegalStateException(
                    "Database directory is unavailable."
                )

        databaseDirectory.mkdirs()

        val sidecarSuffixes = listOf("-wal", "-shm", "-journal")

        val sidecars = sidecarSuffixes.map { suffix ->
            File(databaseFile.absolutePath + suffix)
        }

        val stagedDatabase =
            File(databaseDirectory, DATABASE_FILE_NAME + STAGED_SUFFIX)

        val stagedPeople =
            File(context.filesDir, PEOPLE_DIRECTORY + STAGED_SUFFIX)

        val stagedAudio =
            File(context.filesDir, AUDIO_DIRECTORY + STAGED_SUFFIX)

        val stagedCopies =
            listOf(stagedDatabase, stagedPeople, stagedAudio)

        fun oldCopyOf(file: File): File =
            File(file.parentFile, file.name + OLD_SUFFIX)

        val oldCopies =
            listOf(databaseFile, peopleDirectory, audioDirectory)
                .map { oldCopyOf(it) } +
                    sidecars.map { oldCopyOf(it) }

        // Leftovers from an earlier interrupted restore.
        (stagedCopies + oldCopies).forEach { it.deleteRecursively() }

        // ---- Step 1: stage. Nothing live is touched. ----
        try {

            extractedDatabase.copyTo(
                target = stagedDatabase,
                overwrite = true
            )

            if (extractedPeople.exists()) {
                check(
                    extractedPeople.copyRecursively(
                        target = stagedPeople,
                        overwrite = true
                    )
                ) {
                    "Unable to stage restored people images."
                }
            }

            if (extractedAudio.exists()) {
                check(
                    extractedAudio.copyRecursively(
                        target = stagedAudio,
                        overwrite = true
                    )
                ) {
                    "Unable to stage restored audio."
                }
            }

        } catch (exception: Exception) {

            stagedCopies.forEach { it.deleteRecursively() }

            throw exception
        }

        // ---- Step 2: swap, remembering how to undo each move. ----
        val undoActions = mutableListOf<() -> Unit>()

        fun move(from: File, to: File) {
            check(from.renameTo(to)) {
                "Unable to move ${from.name}."
            }
        }

        fun replace(live: File, staged: File) {

            val old = oldCopyOf(live)

            if (live.exists()) {
                move(live, old)
                undoActions.add { move(old, live) }
            }

            if (staged.exists()) {
                move(staged, live)
                undoActions.add { move(live, staged) }
            }
        }

        try {

            // Old SQLite sidecar files must never be paired with the new database.
            sidecars.forEach { sidecar ->
                if (sidecar.exists()) {
                    val old = oldCopyOf(sidecar)
                    move(sidecar, old)
                    undoActions.add { move(old, sidecar) }
                }
            }

            replace(databaseFile, stagedDatabase)
            replace(peopleDirectory, stagedPeople)
            replace(audioDirectory, stagedAudio)

        } catch (exception: Exception) {

            undoActions.asReversed().forEach { runCatching(it) }

            stagedCopies.forEach { it.deleteRecursively() }

            throw exception
        }

        return RestoreSwap(
            undoActions = undoActions,
            oldCopies = oldCopies,
            stagedCopies = stagedCopies
        )
    }

    /**
     * Removes a temporary extracted backup.
     */
    fun deleteTemporaryBackup(
        extractedDirectory: File
    ) {
        if (extractedDirectory.exists()) {
            extractedDirectory.deleteRecursively()
        }
    }

    private fun addFileToZip(
        file: File,
        zipPath: String,
        zipOutputStream: ZipOutputStream
    ) {

        zipOutputStream.putNextEntry(
            ZipEntry(zipPath)
        )

        FileInputStream(file).use { input ->

            val buffer =
                ByteArray(BUFFER_SIZE)

            while (true) {

                val bytesRead =
                    input.read(buffer)

                if (bytesRead == -1) {
                    break
                }

                zipOutputStream.write(
                    buffer,
                    0,
                    bytesRead
                )
            }
        }

        zipOutputStream.closeEntry()
    }

    /**
     * Prevents ZIP path traversal attacks.
     *
     * Only the database and direct files inside people/ and audio/
     * are accepted.
     */
    private fun validateZipEntry(
        entry: ZipEntry
    ) {

        val name = entry.name

        require(!name.startsWith("/")) {
            "Unsafe absolute ZIP path."
        }

        require(!name.contains("\\")) {
            "Unsafe ZIP path."
        }

        val normalizedParts = name.split("/")

        require(
            normalizedParts.none {
                it == ".." || it == "." || it.isEmpty()
            } || (entry.isDirectory &&
                    normalizedParts.lastOrNull() == "")
        ) {
            "Unsafe ZIP path traversal detected."
        }

        val isDatabaseEntry =
            name == DATABASE_ENTRY

        val isPeopleDirectory =
            entry.isDirectory && name == PEOPLE_ENTRY

        val isPeopleFile =
            !entry.isDirectory &&
                    name.startsWith(PEOPLE_ENTRY) &&
                    name.removePrefix(PEOPLE_ENTRY).isNotBlank() &&
                    !name.removePrefix(PEOPLE_ENTRY).contains("/")

        val isAudioDirectory =
            entry.isDirectory && name == AUDIO_ENTRY

        val audioFileName =
            name.removePrefix(AUDIO_ENTRY)

        val isAudioFile =
            !entry.isDirectory &&
                    name.startsWith(AUDIO_ENTRY) &&
                    isStoredAudioName(audioFileName)

        require(
            isDatabaseEntry ||
                    isPeopleDirectory ||
                    isPeopleFile ||
                    isAudioDirectory ||
                    isAudioFile
        ) {
            "Unexpected file in backup: $name"
        }
    }

    /**
     * Makes sure the archive contains the files we expect.
     */
    private fun validateExtractedBackup(
        directory: File
    ) {

        val extractedDatabase =
            File(
                directory,
                DATABASE_ENTRY
            )

        require(
            extractedDatabase.exists() &&
                    extractedDatabase.isFile &&
                    extractedDatabase.length() > 0
        ) {
            "Backup archive does not contain a valid database file."
        }

        val extractedPeopleDirectory =
            File(
                directory,
                PEOPLE_DIRECTORY
            )

        if (extractedPeopleDirectory.exists()) {

            require(extractedPeopleDirectory.isDirectory) {
                "Invalid people directory."
            }

            val peopleFiles =
                extractedPeopleDirectory.listFiles()
                    ?: throw IllegalStateException(
                        "Unable to read extracted people directory."
                    )

            peopleFiles.forEach { file ->
                require(file.isFile) {
                    "Invalid file inside people directory."
                }
            }
        }

        val extractedAudioDirectory = File(
            directory,
            AUDIO_DIRECTORY
        )

        if (extractedAudioDirectory.exists()) {

            require(extractedAudioDirectory.isDirectory) {
                "Invalid audio directory."
            }

            val audioFiles =
                extractedAudioDirectory.listFiles()
                    ?: throw IllegalStateException(
                        "Unable to read extracted audio directory."
                    )

            audioFiles.forEach { file ->

                require(file.isFile) {
                    "Invalid file inside audio directory."
                }

                require(
                    isStoredAudioName(file.name)
                ) {
                    "Unexpected file inside audio directory."
                }
            }
        }
    }
}


/**
 * Handle returned by BackupArchiveManager.swapInRestoredBackup().
 *
 * Call exactly one of [commit] or [rollback].
 */
class RestoreSwap internal constructor(
    private val undoActions: List<() -> Unit>,
    private val oldCopies: List<File>,
    private val stagedCopies: List<File>
) {

    // The restore succeeded: the previous data is no longer needed.
    fun commit() {
        oldCopies.forEach { it.deleteRecursively() }
    }

    // The restore failed: put the previous data back.
    fun rollback() {
        val failures = mutableListOf<Throwable>()

        undoActions.asReversed().forEach { undo ->
            try { undo() } catch (e: Throwable) { failures.add(e) }
        }
        stagedCopies.forEach { it.deleteRecursively() }

        if (failures.isNotEmpty()) {
            throw IllegalStateException(
                "Rollback incomplete: previous data may still be in the *.restore_old files.",
                failures.first()
            )
        }
    }
}
