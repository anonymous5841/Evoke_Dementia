package com.example.evoke.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GoogleDriveBackupManager {

    companion object {

        private const val DRIVE_FILES_URL =
            "https://www.googleapis.com/drive/v3/files"

        private const val DRIVE_UPLOAD_URL =
            "https://www.googleapis.com/upload/drive/v3/files"

        private const val APP_DATA_FOLDER =
            "appDataFolder"

        private const val BACKUP_FILE_NAME =
            "evoke_backup.enc"

        private const val METADATA_FILE_NAME =
            "evoke_backup_metadata.json"
    }

    /**
     * Uploads or replaces the encrypted backup and its metadata
     * inside Google's private appDataFolder.
     */
    suspend fun uploadBackup(
        accessToken: String,
        backupFile: File,
        metadataFile: File
    ): DriveBackupUploadResult =
        withContext(Dispatchers.IO) {

            require(accessToken.isNotBlank()) {
                "Google Drive access token is missing."
            }

            require(backupFile.exists()) {
                "Encrypted backup file does not exist."
            }

            require(metadataFile.exists()) {
                "Backup metadata file does not exist."
            }

            try {

                val backupFileId =
                    findFile(
                        accessToken = accessToken,
                        fileName = BACKUP_FILE_NAME
                    )

                val metadataFileId =
                    findFile(
                        accessToken = accessToken,
                        fileName = METADATA_FILE_NAME
                    )

                val uploadedBackupId =
                    if (backupFileId != null) {

                        updateFile(
                            accessToken = accessToken,
                            fileId = backupFileId,
                            file = backupFile,
                            mimeType = "application/octet-stream"
                        )

                    } else {

                        createFile(
                            accessToken = accessToken,
                            fileName = BACKUP_FILE_NAME,
                            file = backupFile,
                            mimeType = "application/octet-stream"
                        )
                    }

                val uploadedMetadataId =
                    if (metadataFileId != null) {

                        updateFile(
                            accessToken = accessToken,
                            fileId = metadataFileId,
                            file = metadataFile,
                            mimeType = "application/json"
                        )

                    } else {

                        createFile(
                            accessToken = accessToken,
                            fileName = METADATA_FILE_NAME,
                            file = metadataFile,
                            mimeType = "application/json"
                        )
                    }

                DriveBackupUploadResult.Success(
                    backupFileId = uploadedBackupId,
                    metadataFileId = uploadedMetadataId
                )

            } catch (e: Exception) {

                DriveBackupUploadResult.Failed(
                    e.message
                        ?: "Google Drive backup upload failed."
                )
            }
        }

    /**
     * Downloads the encrypted backup and metadata
     * from Google's private appDataFolder.
     */
    suspend fun downloadBackup(
        accessToken: String,
        outputDirectory: File
    ): DriveBackupDownloadResult =
        withContext(Dispatchers.IO) {

            require(accessToken.isNotBlank()) {
                "Google Drive access token is missing."
            }

            try {

                if (!outputDirectory.exists()) {
                    require(outputDirectory.mkdirs() || outputDirectory.exists()) {
                        "Unable to create restore directory."
                    }
                }

                val backupFileId =
                    findFile(
                        accessToken = accessToken,
                        fileName = BACKUP_FILE_NAME
                    )
                        ?: throw IllegalStateException(
                            "No Evoke backup was found in Google Drive."
                        )

                val metadataFileId =
                    findFile(
                        accessToken = accessToken,
                        fileName = METADATA_FILE_NAME
                    )
                        ?: throw IllegalStateException(
                            "Backup metadata was not found in Google Drive."
                        )

                val backupFile =
                    File(
                        outputDirectory,
                        BACKUP_FILE_NAME
                    )

                val metadataFile =
                    File(
                        outputDirectory,
                        METADATA_FILE_NAME
                    )

                downloadFile(
                    accessToken = accessToken,
                    fileId = backupFileId,
                    outputFile = backupFile
                )

                downloadFile(
                    accessToken = accessToken,
                    fileId = metadataFileId,
                    outputFile = metadataFile
                )

                DriveBackupDownloadResult.Success(
                    backupFile = backupFile,
                    metadataFile = metadataFile
                )

            } catch (e: Exception) {

                outputDirectory.deleteRecursively()

                DriveBackupDownloadResult.Failed(
                    e.message
                        ?: "Google Drive backup download failed."
                )
            }
        }
    /**
     * Finds a file belonging to this app's private appDataFolder.
     */
    private fun findFile(
        accessToken: String,
        fileName: String
    ): String? {

        val encodedName =
            fileName
                .replace("\\", "\\\\")
                .replace("'", "\\'")

        val query =
            "'$APP_DATA_FOLDER' in parents " +
                    "and name = '$encodedName' " +
                    "and trashed = false"

        val encodedQuery =
            URLEncoder.encode(
                query,
                "UTF-8"
            )

        val url =
            URL(
                "$DRIVE_FILES_URL" +
                        "?spaces=appDataFolder" +
                        "&q=$encodedQuery" +
                        "&fields=files(id,name)"
            )

        val connection =
            url.openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "GET"

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {

                throw Exception(
                    "Google Drive file search failed. " +
                            "HTTP $responseCode"
                )
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            val json =
                JSONObject(response)

            val files =
                json.optJSONArray("files")

            if (
                files == null ||
                files.length() == 0
            ) {
                return null
            }

            return files
                .getJSONObject(0)
                .getString("id")

        } finally {

            connection.disconnect()
        }
    }

    /**
     * Downloads a single file from Google Drive.
     */
    private fun downloadFile(
        accessToken: String,
        fileId: String,
        outputFile: File
    ) {

        val url =
            URL(
                "$DRIVE_FILES_URL/$fileId?alt=media"
            )

        val connection =
            url.openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "GET"

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Accept",
                "*/*"
            )

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {

                val errorBody =
                    try {
                        connection.errorStream
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }
                    } catch (_: Exception) {
                        null
                    }

                throw Exception(
                    "Google Drive backup download failed. " +
                            "HTTP $responseCode" +
                            if (!errorBody.isNullOrBlank()) {
                                "\n$errorBody"
                            } else {
                                ""
                            }
                )
            }

            val temporaryFile =
                File(
                    outputFile.parentFile,
                    "${outputFile.name}.tmp"
                )

            try {

                connection.inputStream.use { input ->

                    temporaryFile.outputStream().use { output ->

                        input.copyTo(
                            output,
                            bufferSize = 8192
                        )
                    }
                }

                require(
                    temporaryFile.renameTo(outputFile)
                ) {
                    "Unable to save downloaded backup file."
                }

            } catch (e: Exception) {

                temporaryFile.delete()
                throw e
            }

        } finally {

            connection.disconnect()
        }
    }

    /**
     * Creates a new file inside appDataFolder.
     */
    private fun createFile(
        accessToken: String,
        fileName: String,
        file: File,
        mimeType: String
    ): String {

        val url =
            URL(
                "$DRIVE_UPLOAD_URL" +
                        "?uploadType=multipart" +
                        "&fields=id"
            )

        val boundary =
            "EvokeBackupBoundary"

        val connection =
            url.openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "POST"

            connection.doOutput = true

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Content-Type",
                "multipart/related; boundary=$boundary"
            )

            val metadata =
                JSONObject().apply {

                    put(
                        "name",
                        fileName
                    )

                    put(
                        "parents",
                        org.json.JSONArray().apply {
                            put(APP_DATA_FOLDER)
                        }
                    )
                }

            connection.outputStream.use { output ->

                writeMultipart(
                    output = output,
                    boundary = boundary,
                    metadata = metadata.toString(),
                    file = file,
                    mimeType = mimeType
                )
            }

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {

                throw Exception(
                    "Google Drive upload failed. " +
                            "HTTP $responseCode"
                )
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            return JSONObject(response)
                .getString("id")

        } finally {

            connection.disconnect()
        }
    }

    /**
     * Replaces the content of an existing Drive file.
     */
    private fun updateFile(
        accessToken: String,
        fileId: String,
        file: File,
        mimeType: String
    ): String {

        val url =
            URL(
                "$DRIVE_UPLOAD_URL/$fileId" +
                        "?uploadType=media" +
                        "&fields=id"
            )

        val connection =
            url.openConnection() as HttpURLConnection

        try {

            connection.requestMethod = "PATCH"

            connection.doOutput = true

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Content-Type",
                mimeType
            )

            connection.setRequestProperty(
                "Content-Length",
                file.length().toString()
            )

            file.inputStream().use { input ->
                connection.outputStream.use { output ->

                    input.copyTo(
                        output,
                        bufferSize = 8192
                    )
                }
            }

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {

                throw Exception(
                    "Google Drive update failed. " +
                            "HTTP $responseCode"
                )
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        it.readText()
                    }

            return JSONObject(response)
                .getString("id")

        } finally {

            connection.disconnect()
        }
    }

    private fun writeMultipart(
        output: java.io.OutputStream,
        boundary: String,
        metadata: String,
        file: File,
        mimeType: String
    ) {

        val lineBreak =
            "\r\n"

        output.write(
            "--$boundary$lineBreak"
                .toByteArray()
        )

        output.write(
            "Content-Type: application/json; charset=UTF-8$lineBreak"
                .toByteArray()
        )

        output.write(
            lineBreak.toByteArray()
        )

        output.write(
            metadata.toByteArray()
        )

        output.write(
            lineBreak.toByteArray()
        )

        output.write(
            "--$boundary$lineBreak"
                .toByteArray()
        )

        output.write(
            "Content-Type: $mimeType$lineBreak"
                .toByteArray()
        )

        output.write(
            lineBreak.toByteArray()
        )

        file.inputStream().use { input ->

            input.copyTo(
                output,
                bufferSize = 8192
            )
        }

        output.write(
            lineBreak.toByteArray()
        )

        output.write(
            "--$boundary--$lineBreak"
                .toByteArray()
        )

        output.flush()
    }

    /**
     * Checks whether an Evoke backup or its metadata
     * already exists in Google's private appDataFolder.
     */
    suspend fun hasExistingBackup(
        accessToken: String
    ): Boolean = withContext(Dispatchers.IO) {

        require(accessToken.isNotBlank()) {
            "Google Drive access token is missing."
        }

        val backupFileId = findFile(
            accessToken = accessToken,
            fileName = BACKUP_FILE_NAME
        )

        val metadataFileId = findFile(
            accessToken = accessToken,
            fileName = METADATA_FILE_NAME
        )

        // Treat even a partially existing backup as an existing backup.
        backupFileId != null || metadataFileId != null
    }
}

sealed class DriveBackupUploadResult {

    data class Success(
        val backupFileId: String,
        val metadataFileId: String
    ) : DriveBackupUploadResult()

    data class Failed(
        val message: String
    ) : DriveBackupUploadResult()
}

sealed class DriveBackupDownloadResult {

    data class Success(
        val backupFile: File,
        val metadataFile: File
    ) : DriveBackupDownloadResult()

    data class Failed(
        val message: String
    ) : DriveBackupDownloadResult()
}