package com.example.evoke.backup

data class BackupMetadata(
    val formatVersion: Int,

    // Password KDF information
    val kdfVersion: Int,
    val salt: String,
    val memoryKb: Int,
    val iterations: Int,
    val parallelism: Int,

    // BackupKey wrapped by the password-derived KEK
    val wrappedBackupKey: String,
    val backupKeyIv: String,
    val encryptedDatabaseKey: String,
    val databaseKeyIv: String
)