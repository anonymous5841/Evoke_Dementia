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
        private const val DATABASE_FILE_NAME = "dementia_database"
        private const val PEOPLE_DIRECTORY = "people"

        private const val DATABASE_ENTRY =
            "database/$DATABASE_FILE_NAME"

        private const val PEOPLE_ENTRY =
            "people/"

        private const val BUFFER_SIZE = 8192
    }

    private val databaseFile: File
        get() = context.getDatabasePath(DATABASE_FILE_NAME)

    private val peopleDirectory: File
        get() = File(
            context.filesDir,
            PEOPLE_DIRECTORY
        )

    fun hasLocalBackupData(): Boolean {
        val databaseExists =
            databaseFile.exists() && databaseFile.length() > 0

        val peopleImagesExist =
            peopleDirectory.listFiles()?.any { it.isFile } == true

        return databaseExists || peopleImagesExist
    }
    /**
     * Creates a ZIP archive containing:
     *
     * database/dementia_database
     * people/
     *     person_1.jpg
     *     person_2.jpg
     *     ...
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

            // Add database
            addFileToZip(
                file = databaseFile,
                zipPath = DATABASE_ENTRY,
                zipOutputStream = zipOutputStream
            )

            // Add people directory
            if (peopleDirectory.exists()) {

                zipOutputStream.putNextEntry(
                    ZipEntry(PEOPLE_ENTRY)
                )

                zipOutputStream.closeEntry()

                peopleDirectory
                    .listFiles()
                    ?.filter { it.isFile }
                    ?.forEach { imageFile ->

                        addFileToZip(
                            file = imageFile,
                            zipPath = "$PEOPLE_ENTRY${imageFile.name}",
                            zipOutputStream = zipOutputStream
                        )
                    }
            }
        }
    }

    /**
     * Restores a previously created backup archive.
     *
     * IMPORTANT:
     * This extracts into a temporary directory first.
     * The live database and people directory are not touched
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
                        zipInputStream.nextEntry
                            ?: break

                    validateZipEntry(entry)

                    val outputFile =
                        File(
                            temporaryDirectory,
                            entry.name
                        )

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

            validateExtractedBackup(
                temporaryDirectory
            )

            return temporaryDirectory

        } catch (exception: Exception) {

            temporaryDirectory.deleteRecursively()

            throw exception
        }
    }

    /**
     * Commits a successfully extracted backup to the application's
     * local storage.
     *
     * This method should only be called after the extracted backup
     * has been validated.
     */

    fun commitRestoredBackup(
        extractedDirectory: File
    ) {

        val extractedDatabase =
            File(
                extractedDirectory,
                DATABASE_ENTRY
            )

        val extractedPeopleDirectory =
            File(
                extractedDirectory,
                PEOPLE_DIRECTORY
            )

        require(extractedDatabase.exists()) {
            "Extracted backup database is missing."
        }

        // Ensure the parent database directory exists.
        databaseFile.parentFile?.mkdirs()

        // Remove SQLite sidecar files before replacing the database.
        val databaseSidecarFiles = listOf(
            File(databaseFile.absolutePath + "-wal"),
            File(databaseFile.absolutePath + "-shm"),
            File(databaseFile.absolutePath + "-journal")
        )

        databaseSidecarFiles.forEach { file ->
            if (file.exists()) {
                check(file.delete()) {
                    "Unable to remove old database file: ${file.name}"
                }
            }
        }

        // Remove the existing database.
        if (databaseFile.exists()) {
            check(databaseFile.delete()) {
                "Unable to remove the existing database."
            }
        }

        // Copy the restored database.
        extractedDatabase.copyTo(
            target = databaseFile,
            overwrite = true
        )

        // Replace the people directory.
        if (peopleDirectory.exists()) {
            check(peopleDirectory.deleteRecursively()) {
                "Unable to remove the existing people directory."
            }
        }

        // Copy restored people images, if included in the backup.
        if (extractedPeopleDirectory.exists()) {
            extractedPeopleDirectory.copyRecursively(
                target = peopleDirectory,
                overwrite = true
            )
        }
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
     * Prevents ZIP path traversal attacks such as:
     *
     * ../../databases/dementia_database
     *
     * or absolute paths.
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

        val normalizedParts =
            name.split("/")

        require(
            normalizedParts.none { it == ".." }
        ) {
            "Unsafe ZIP path traversal detected."
        }

        require(
            name == DATABASE_ENTRY ||
                    name == PEOPLE_ENTRY ||
                    name.startsWith(PEOPLE_ENTRY)
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

        require(extractedDatabase.exists()) {
            "Backup archive does not contain the database."
        }

        val extractedPeopleDirectory =
            File(
                directory,
                PEOPLE_DIRECTORY
            )

        if (extractedPeopleDirectory.exists()) {

            extractedPeopleDirectory
                .listFiles()
                ?.forEach { file ->

                    require(file.isFile) {
                        "Invalid file inside people directory."
                    }
                }
        }
    }
}