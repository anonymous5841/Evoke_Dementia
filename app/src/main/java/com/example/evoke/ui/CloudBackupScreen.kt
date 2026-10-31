package com.example.evoke.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.evoke.auth.DriveAuthorizationResult
import com.example.evoke.auth.GoogleDriveBackupManager
import com.example.evoke.auth.GoogleDriveManager
import com.example.evoke.auth.GoogleSignInManager
import com.example.evoke.auth.GoogleAccount
import com.example.evoke.backup.BackupPackageManager
import com.example.evoke.ui.components.HeaderSection
import androidx.compose.foundation.BorderStroke
import com.example.evoke.ui.theme.AppTheme
import com.example.evoke.ui.theme.GreenTheme
import com.example.evoke.ui.theme.BlueTheme
import com.example.evoke.ui.theme.MartelFont
import com.example.evoke.ui.theme.OutfitFont
import kotlinx.coroutines.launch
import java.io.File
import androidx.activity.compose.LocalActivity
import com.example.evoke.auth.NoInternetConnectionException
import android.util.Log


@Composable
fun CloudBackupScreen(
    onBack: () -> Unit
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    var backupEnabled by remember {
        mutableStateOf(false)
    }


    var googleAccount by remember {
        mutableStateOf<GoogleAccount?>(null)
    }

    var driveAuthorized by remember {
        mutableStateOf(false)
    }

    var driveAccessToken by remember {
        mutableStateOf<String?>(null)
    }

    var statusMessage by remember {
        mutableStateOf<String?>(null)
    }

    var isBackingUp by remember {
        mutableStateOf(false)
    }
    var isRestoring by remember {
        mutableStateOf(false)
    }
    var showPasswordDialog by remember {
        mutableStateOf(false)
    }
    var showReplaceBackupConfirmation by remember {
        mutableStateOf(false)
    }

    var isCheckingExistingBackup by remember {
        mutableStateOf(false)
    }
    var showRestorePasswordDialog by remember {
        mutableStateOf(false)
    }
    var showRestoreOverwriteConfirmation by remember {
        mutableStateOf(false)
    }

    val googleSignInManager = remember {
        GoogleSignInManager(context)
    }

    val googleDriveManager = remember {
        GoogleDriveManager()
    }

    val googleDriveBackupManager = remember {
        GoogleDriveBackupManager()
    }

    val backupPackageManager = remember {
        BackupPackageManager(context)
    }

    // =============================================================
    // GOOGLE DRIVE AUTHORIZATION RESULT
    // =============================================================

    val driveAuthorizationLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (activity == null) {
                statusMessage =
                    "Unable to access the current activity."

                return@rememberLauncherForActivityResult
            }

            if (result.resultCode != Activity.RESULT_OK) {

                driveAuthorized = false
                driveAccessToken = null

                statusMessage =
                    "Google Drive authorization was cancelled."

                return@rememberLauncherForActivityResult
            }

            try {

                val authorizationResult =
                    googleDriveManager
                        .getAuthorizationResultFromIntent(
                            activity = activity,
                            data = result.data
                        )

                when (authorizationResult) {

                    is DriveAuthorizationResult.Authorized -> {

                        val token =
                            authorizationResult.accessToken

                        if (token.isNullOrEmpty()) {

                            driveAuthorized = false
                            driveAccessToken = null

                            statusMessage =
                                "Google Drive authorization failed."

                        } else {

                            driveAuthorized = true
                            driveAccessToken = token

                            statusMessage =
                                "Google Drive connected successfully."
                        }
                    }

                    is DriveAuthorizationResult.Failed -> {

                        driveAuthorized = false
                        driveAccessToken = null

                        statusMessage =
                            authorizationResult.message
                    }

                    is DriveAuthorizationResult.NeedsResolution -> {

                        driveAuthorized = false
                        driveAccessToken = null

                        statusMessage =
                            "Additional authorization is required."
                    }
                }

            } catch (e: Exception) {

                driveAuthorized = false
                driveAccessToken = null

                statusMessage =
                    e.message
                        ?: "Google Drive authorization failed."
            }
        }

    //Confirmation daiglog
    if (showReplaceBackupConfirmation) {

        AlertDialog(
            onDismissRequest = {
                showReplaceBackupConfirmation = false
            },
            containerColor = colors.background,
            titleContentColor = colors.pagesText,
            textContentColor = colors.pagesText,

            title = {
                Text(
                    text = "Replace Existing Backup?",
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text = "An Evoke backup already exists in Google Drive. " +
                            "Do you want to replace it with a new backup? " +
                            "Your existing cloud backup will be overwritten.",
                    fontFamily = OutfitFont,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showReplaceBackupConfirmation = false
                        showPasswordDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.pagesText
                    )
                ) {
                    Text(
                        text = "Replace Backup",
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showReplaceBackupConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.pagesText
                    )
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = OutfitFont
                    )
                }
            }
        )
    }
    // =============================================================
    // PASSWORD DIALOG
    // =============================================================

    if (showPasswordDialog) {

        var password by remember {
            mutableStateOf("")
        }

        var confirmPassword by remember {
            mutableStateOf("")
        }

        var passwordError by remember {
            mutableStateOf<String?>(null)
        }

        AlertDialog(
            onDismissRequest = {

                if (!isBackingUp) {
                    showPasswordDialog = false
                    password = ""
                    confirmPassword = ""
                    passwordError = null
                }
            },
            containerColor = colors.background,
            titleContentColor = colors.pagesText,
            textContentColor = colors.pagesText,
            title = {
                Text(
                    text = "Create Backup Password",
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {

                Column {

                    Text(
                        text =
                            "This password protects your backup. " +
                                    "You will need the same password " +
                                    "to restore your backup on another device.",
                        fontFamily = OutfitFont,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        enabled = !isBackingUp,
                        label = {
                            Text(
                                text = "Backup password",
                                fontFamily = OutfitFont
                            )
                        },
                        singleLine = true,
                        colors = backupFieldColors(),
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            passwordError = null
                        },
                        enabled = !isBackingUp && !isRestoring,
                        label = {
                            Text(
                                text = "Confirm password",
                                fontFamily = OutfitFont
                            )
                        },
                        singleLine = true,
                        colors = backupFieldColors(),
                        visualTransformation =
                            androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passwordError != null) {

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = passwordError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontFamily = OutfitFont
                        )
                    }
                }
            },
            confirmButton = {

                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.pagesText),
                    enabled = !isBackingUp,
                    onClick = {

                        when {

                            password.isEmpty() -> {

                                passwordError =
                                    "Please enter a password."
                            }

                            password.length < 8 -> {

                                passwordError =
                                    "Password must be at least 8 characters."
                            }

                            password != confirmPassword -> {

                                passwordError =
                                    "Passwords do not match."
                            }

                            driveAccessToken.isNullOrEmpty() -> {

                                passwordError =
                                    "Google Drive is not authorized."
                            }

                            else -> {

                                val passwordChars =
                                    password.toCharArray()

                                password = ""
                                confirmPassword = ""
                                passwordError = null

                                isBackingUp = true

                                scope.launch {

                                    try {

                                        statusMessage = "Creating encrypted backup..."

                                        val packageDirectory =
                                            backupPackageManager.createBackup(passwordChars)
                                        try {

                                            statusMessage =
                                                "Uploading encrypted backup..."

                                            val uploadResult =
                                                googleDriveBackupManager
                                                    .uploadBackup(
                                                        accessToken =
                                                            driveAccessToken!!,
                                                        backupFile =
                                                            backupPackageManager
                                                                .getEncryptedBackupFile(
                                                                    packageDirectory
                                                                ),
                                                        metadataFile =
                                                            backupPackageManager
                                                                .getMetadataFile(
                                                                    packageDirectory
                                                                )
                                                    )

                                            when (uploadResult) {

                                                is com.example.evoke.auth.DriveBackupUploadResult.Success -> {

                                                    statusMessage =
                                                        "Backup completed successfully."

                                                    showPasswordDialog = false
                                                }

                                                is com.example.evoke.auth.DriveBackupUploadResult.Failed -> {

                                                    statusMessage =
                                                        "Backup upload failed:\n${uploadResult.message}"
                                                }
                                            }


                                        } catch (e: NoInternetConnectionException) {

                                            statusMessage =
                                                "No internet connection. Please connect to the internet and try again."

                                        } catch (e: kotlinx.coroutines.CancellationException) {

                                            throw e

                                        } catch (e: Exception) {

                                            statusMessage =
                                                "Backup upload failed. Please try again."

                                            Log.e("CloudBackup", "Backup upload failed", e)

                                        } finally {

                                            packageDirectory.deleteRecursively()
                                        }

                                    } catch (e: Exception) {

                                        statusMessage =
                                            "Backup creation failed:\n${e.message}"

                                    } finally {

                                        passwordChars.fill('\u0000')

                                        isBackingUp = false
                                    }
                                }
                            }
                        }
                    }
                ) {

                    Text(
                        text =
                            if (isBackingUp)
                                "Backing up..."
                            else
                                "Create Backup",
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {

                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.pagesText),
                    enabled = !isBackingUp,
                    onClick = {

                        showPasswordDialog = false
                        password = ""
                        confirmPassword = ""
                        passwordError = null
                    }
                ) {

                    Text(
                        text = "Cancel",
                        fontFamily = OutfitFont
                    )
                }
            }
        )
    }

    if (showRestoreOverwriteConfirmation) {

        AlertDialog(
            onDismissRequest = {
                showRestoreOverwriteConfirmation = false
            },
            containerColor = colors.background,
            titleContentColor = colors.pagesText,
            textContentColor = colors.pagesText,

            title = {
                Text(
                    text = "Replace Existing Data?",
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text = "Existing data on this device was found. " +
                            "Restoring will replace your current local database " +
                            "and saved people images with the data from your Google Drive backup. " +
                            "This action cannot be undone. Do you want to continue?",
                    fontFamily = OutfitFont,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreOverwriteConfirmation = false
                        showRestorePasswordDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.pagesText
                    )
                ) {
                    Text(
                        text = "Continue",
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreOverwriteConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.pagesText
                    )
                ) {
                    Text(
                        text = "Cancel",
                        fontFamily = OutfitFont
                    )
                }
            }
        )
    }

    if (showRestorePasswordDialog) {

        var restorePassword by remember {
            mutableStateOf("")
        }

        var restorePasswordError by remember {
            mutableStateOf<String?>(null)
        }

        AlertDialog(
            onDismissRequest = {
                if (!isRestoring) {
                    showRestorePasswordDialog = false
                    restorePassword = ""
                    restorePasswordError = null
                }
            },
            containerColor = colors.background,
            titleContentColor = colors.pagesText,
            textContentColor = colors.pagesText,
            title = {
                Text(
                    text = "Restore My Data",
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {

                Column {

                    Text(
                        text =
                            "Enter the password you used when creating your backup.",
                        fontFamily = OutfitFont,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = {
                            restorePassword = it
                            restorePasswordError = null
                        },
                        label = {
                            Text(
                                text = "Backup Password",
                                fontFamily = OutfitFont
                            )
                        },
                        singleLine = true,
                        colors = backupFieldColors(),
                        enabled = !isRestoring,
                        visualTransformation =
                            PasswordVisualTransformation(),
                        isError =
                            restorePasswordError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    restorePasswordError?.let { error ->

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontFamily = OutfitFont
                        )
                    }
                }
            },
            confirmButton = {

                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.pagesText),
                    enabled =
                        !isRestoring &&
                                restorePassword.isNotEmpty(),
                    onClick = {

                        restorePasswordError = null

                        if (restorePassword.length < 8) {

                            restorePasswordError =
                                "Password must be at least 8 characters."

                            return@TextButton
                        }

                        if (driveAccessToken.isNullOrEmpty()) {

                            restorePasswordError =
                                "Google Drive is not authorized."

                            return@TextButton
                        }

                        val passwordChars =
                            restorePassword.toCharArray()

                        restorePassword = ""

                        isRestoring = true

                        scope.launch {

                            var restoreDirectory: File? = null

                            try {

                                statusMessage =
                                    "Downloading encrypted backup..."

                                restoreDirectory =
                                    File(
                                        context.cacheDir,
                                        "restore_${System.currentTimeMillis()}"
                                    )

                                val downloadResult =
                                    googleDriveBackupManager
                                        .downloadBackup(
                                            accessToken =
                                                driveAccessToken!!,
                                            outputDirectory =
                                                restoreDirectory
                                        )

                                when (downloadResult) {

                                    is com.example.evoke.auth.DriveBackupDownloadResult.Failed -> {

                                        statusMessage =
                                            "Backup download failed:\n" +
                                                    downloadResult.message
                                    }

                                    is com.example.evoke.auth.DriveBackupDownloadResult.Success -> {

                                        statusMessage =
                                            "Preparing backup for restore..."

                                        val downloadedBackup =
                                            downloadResult.backupFile

                                        val downloadedMetadata =
                                            downloadResult.metadataFile

                                        val backupPackageDirectory =
                                            downloadedBackup.parentFile
                                                ?: throw IllegalStateException(
                                                    "Invalid restore directory."
                                                )

                                        val expectedBackupFile =
                                            File(
                                                backupPackageDirectory,
                                                "backup.enc"
                                            )

                                        val expectedMetadataFile =
                                            File(
                                                backupPackageDirectory,
                                                "metadata.json"
                                            )

                                        require(
                                            downloadedBackup.renameTo(
                                                expectedBackupFile
                                            )
                                        ) {
                                            "Unable to prepare encrypted backup."
                                        }

                                        require(
                                            downloadedMetadata.renameTo(
                                                expectedMetadataFile
                                            )
                                        ) {
                                            "Unable to prepare backup metadata."
                                        }

                                        statusMessage =
                                            "Restoring your data..."

                                        backupPackageManager.restoreBackup(
                                                packageDirectory = backupPackageDirectory,
                                                password = passwordChars
                                            )

                                        statusMessage =
                                            "Data restored successfully."

                                        showRestorePasswordDialog =
                                            false
                                    }
                                }

                            } catch (e: NoInternetConnectionException) {

                                statusMessage =
                                    "No internet connection. Please connect to the internet and try again."

                            } catch (e: kotlinx.coroutines.CancellationException) {

                                throw e

                            } catch (e: Exception) {

                                statusMessage =
                                    "Restore failed. Please check your backup password and try again."

                                Log.e("CloudBackup", "Restore failed", e)

                            } finally {

                                passwordChars.fill('\u0000')

                                restoreDirectory?.deleteRecursively()

                                isRestoring = false
                            }
                        }
                    }
                ) {

                    Text(
                        text =
                            if (isRestoring)
                                "Restoring..."
                            else
                                "Restore",
                        fontFamily = OutfitFont,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {

                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.pagesText),
                    enabled = !isRestoring,
                    onClick = {

                        showRestorePasswordDialog = false
                        restorePassword = ""
                        restorePasswordError = null
                    }
                ) {

                    Text(
                        text = "Cancel",
                        fontFamily = OutfitFont
                    )
                }
            }
        )
    }

    Scaffold(
        containerColor = colors.background
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // =============================================================
            // SCROLLABLE CONTENT
            // =============================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {

                Spacer(
                    modifier = Modifier.height(218.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .offset(y = (-40).dp)
                ) {

                    // =====================================================
                    // BACKUP STATUS CARD
                    // =====================================================

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                colors.boxOuter
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 1.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color =
                                            colors.pagesText,
                                        shape =
                                            RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        if (backupEnabled)
                                            Icons.Default.CloudDone
                                        else
                                            Icons.Default.Cloud,
                                    contentDescription = null,
                                    tint =
                                        colors.iconSelected,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(14.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text =
                                        if (backupEnabled)
                                            "Cloud Backup Enabled"
                                        else
                                            "Cloud Backup",
                                    color =
                                        colors.pagesText,
                                    fontSize = 18.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    fontFamily = MartelFont
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        when {
                                            driveAuthorized ->
                                                "Google Drive is connected."

                                            backupEnabled ->
                                                "Your backup is ready to be configured."

                                            else ->
                                                "Keep an encrypted copy of your data."
                                        },
                                    color =
                                        colors.pagesText
                                            .copy(alpha = 0.75f),
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    fontFamily = OutfitFont
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    // =====================================================
                    // ENABLE BACKUP
                    // =====================================================

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                colors.textfield
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 18.dp,
                                    vertical = 16.dp
                                ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "Enable Cloud Backup",
                                    color =
                                        colors.pagesText,
                                    fontSize = 18.sp,
                                    fontWeight =
                                        FontWeight.SemiBold,
                                    fontFamily = OutfitFont
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        "Save an encrypted backup to your cloud storage.",
                                    color =
                                        colors.textFieldHint,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = OutfitFont
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(10.dp)
                            )

                            Switch(
                                checked = backupEnabled,
                                onCheckedChange = {

                                    backupEnabled = it

                                    if (!it) {
                                        statusMessage = null
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor =
                                        colors.iconSelected,
                                    checkedTrackColor =
                                        colors.toggleColor,
                                    uncheckedThumbColor =
                                        colors.textfield,
                                    uncheckedTrackColor =
                                        colors.selectedFieldOutline
                                )
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    // =====================================================
                    // SECURITY CARD
                    // =====================================================

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                colors.textfield
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Security,
                                    contentDescription = null,
                                    tint =
                                        colors.pagesText,
                                    modifier = Modifier.size(25.dp)
                                )

                                Spacer(
                                    modifier = Modifier.width(10.dp)
                                )

                                Text(
                                    text = "Your data stays protected",
                                    color =
                                        colors.pagesText,
                                    fontSize = 17.sp,
                                    fontWeight =
                                        FontWeight.Bold,
                                    fontFamily = OutfitFont
                                )
                            }

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            BackupSecurityItem(
                                icon = Icons.Default.Lock,
                                title = "Encrypted backup",
                                description =
                                    "Your backup is encrypted before it leaves your device."
                            )

                            Spacer(
                                modifier = Modifier.height(14.dp)
                            )

                            BackupSecurityItem(
                                icon = Icons.Default.Storage,
                                title = "Private storage",
                                description =
                                    "Only this app can access its backup storage."
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    // =====================================================
                    // GOOGLE ACCOUNT CARD
                    // =====================================================

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                colors.textfield
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {

                            Text(
                                text = "Google Account",
                                color =
                                    colors.pagesText,
                                fontSize = 17.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                fontFamily = OutfitFont
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    when {
                                        googleAccount != null ->
                                            "Connected to ${googleAccount?.displayName ?: "your Google Account"}."

                                        backupEnabled ->
                                            "Connect your Google Account to use cloud backup."

                                        else ->
                                            "Connect an account when you enable cloud backup."
                                    },
                                color =
                                    colors.textFieldHint,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = OutfitFont
                            )

                            if (backupEnabled) {

                                Spacer(
                                    modifier = Modifier.height(14.dp)
                                )

                                OutlinedButton(
                                    onClick = {

                                        if (activity == null) {

                                            statusMessage =
                                                "Unable to access the current activity."

                                            return@OutlinedButton
                                        }

                                        scope.launch {

                                            try {

                                                // =============================================
                                                // STEP 1:
                                                // GOOGLE SIGN-IN
                                                // =============================================

                                                if (googleAccount == null) {

                                                    val account =
                                                        googleSignInManager
                                                            .signIn(activity)

                                                    googleAccount =
                                                        account

                                                    statusMessage =
                                                        "Google Account connected."
                                                }

                                                // =============================================
                                                // STEP 2:
                                                // GOOGLE DRIVE AUTHORIZATION
                                                // =============================================

                                                val driveResult =
                                                    googleDriveManager
                                                        .requestDriveAccess(
                                                            activity
                                                        )

                                                when (driveResult) {

                                                    is DriveAuthorizationResult.Authorized -> {

                                                        val token =
                                                            driveResult.accessToken

                                                        if (token.isNullOrEmpty()) {

                                                            driveAuthorized =
                                                                false

                                                            driveAccessToken =
                                                                null

                                                            statusMessage =
                                                                "Google Drive authorization failed."

                                                        } else {

                                                            driveAuthorized =
                                                                true

                                                            driveAccessToken =
                                                                token

                                                            statusMessage =
                                                                "Google Drive connected successfully."
                                                        }
                                                    }

                                                    is DriveAuthorizationResult.NeedsResolution -> {

                                                        val pendingIntent =
                                                            driveResult.pendingIntent

                                                        if (pendingIntent == null) {

                                                            statusMessage =
                                                                "Unable to start Google Drive authorization."

                                                        } else {

                                                            val intentSenderRequest =
                                                                IntentSenderRequest
                                                                    .Builder(
                                                                        pendingIntent.intentSender
                                                                    )
                                                                    .build()

                                                            driveAuthorizationLauncher
                                                                .launch(
                                                                    intentSenderRequest
                                                                )
                                                        }
                                                    }

                                                    is DriveAuthorizationResult.Failed -> {

                                                        driveAuthorized =
                                                            false

                                                        driveAccessToken =
                                                            null

                                                        statusMessage =
                                                            driveResult.message
                                                    }
                                                }

                                            } catch (e: NoInternetConnectionException) {

                                                statusMessage =
                                                    "No internet connection. Please connect to the internet and try again."

                                            } catch (e: kotlinx.coroutines.CancellationException) {

                                                throw e

                                            } catch (e: Exception) {

                                                statusMessage =
                                                    "Unable to connect your Google Account. Please try again."

                                                Log.e(
                                                    "CloudBackup",
                                                    "Google Account connection failed",
                                                    e
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isBackingUp,
                                    shape =
                                        RoundedCornerShape(10.dp),
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    colors =
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor =
                                                colors.pagesText
                                        ),
                                    border = BorderStroke(1.dp, colors.pagesText)
                                ) {

                                    Text(
                                        text =
                                            when {
                                                driveAuthorized ->
                                                    "Google Drive Connected"

                                                googleAccount != null ->
                                                    "Authorize Google Drive"

                                                else ->
                                                    "Connect Google Account"
                                            },
                                        fontSize = 15.sp,
                                        fontWeight =
                                            FontWeight.SemiBold,
                                        fontFamily = OutfitFont
                                    )
                                }
                            }

                            // =================================================
                            // BACKUP BUTTON
                            // =================================================

                            if (driveAuthorized) {

                                Spacer(
                                    modifier = Modifier.height(12.dp)
                                )

                                Button(
                                    onClick = {
                                        val token = driveAccessToken

                                        if (token.isNullOrEmpty()) {

                                            statusMessage =
                                                "Google Drive authorization is required."

                                        } else {

                                            isCheckingExistingBackup = true

                                            scope.launch {
                                                try {

                                                    statusMessage =
                                                        "Checking Google Drive for an existing backup..."

                                                    val backupExists =
                                                        googleDriveBackupManager.hasExistingBackup(
                                                            accessToken = token
                                                        )

                                                    if (backupExists) {

                                                        showReplaceBackupConfirmation = true

                                                    } else {

                                                        showPasswordDialog = true
                                                    }

                                                } catch (e: kotlinx.coroutines.CancellationException) {

                                                    throw e

                                                } catch (e: Exception) {

                                                    statusMessage =
                                                        "Unable to check Google Drive for an existing backup. Please try again."

                                                    Log.e(
                                                        "CloudBackup",
                                                        "Existing backup check failed",
                                                        e
                                                    )

                                                } finally {

                                                    isCheckingExistingBackup = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isBackingUp && !isCheckingExistingBackup,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.pagesText,
                                        contentColor = colors.iconSelected
                                    )
                                ) {

                                    Text(
                                        text =
                                            if (isBackingUp)
                                                "Creating Backup..."
                                            else
                                                "Back Up My Data",
                                        fontSize = 15.sp,
                                        fontWeight =
                                            FontWeight.SemiBold,
                                        fontFamily = OutfitFont
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (backupPackageManager.hasLocalBackupData()) {
                                            showRestoreOverwriteConfirmation = true
                                        } else {
                                            showRestorePasswordDialog = true
                                        }
                                    },
                                    enabled = !isBackingUp && !isRestoring,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.pagesText,
                                        contentColor = colors.iconSelected
                                    )
                                ) {
                                    Text(
                                        text = if (isRestoring) "Restoring..." else "Restore My Data",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = OutfitFont
                                    )
                                }
                            }

                            // =================================================
                            // STATUS MESSAGE
                            // =================================================

                            if (statusMessage != null) {

                                Spacer(
                                    modifier = Modifier.height(10.dp)
                                )

                                Text(
                                    text = statusMessage!!,
                                    color =
                                        if (driveAuthorized)
                                            colors.pagesText
                                        else
                                            colors.pagesText.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = OutfitFont
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    // =====================================================
                    // INFORMATION
                    // =====================================================

                    Text(
                        text =
                            "Cloud backup is optional. Your data will remain " +
                                    "on this device if backup is not enabled.",
                        color =
                            colors.textFieldHint,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        fontFamily = OutfitFont,
                        modifier = Modifier.padding(
                            horizontal = 4.dp
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(30.dp)
                    )
                }
            }

            // =============================================================
            // HEADER
            // =============================================================

//            HeaderSection(
//                title = "Cloud Backup",
//                spacing = 72.dp,
//                textSize = 39.sp,
//                bottomspace = 30.dp,
//                leaves = 9.dp,
//                headerHeight = 218.dp,
//                onBack = onBack
//            )
            HeaderSection(
                title = "Cloud Backup",

                headerHeight = 218.dp,

                textSize = 39.sp,

                spacing = 58.dp,

                bottomspace = 34.dp,

                leaves =
                    colors.headerDecorOffset2,

                onBack = {
                    onBack()
                }
            )
        }
    }
}

@Composable
private fun BackupSecurityItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint =
                colors.pagesText,
            modifier = Modifier
                .size(21.dp)
                .padding(top = 1.dp)
        )

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                color =
                    colors.pagesText,
                fontSize = 15.sp,
                fontWeight =
                    FontWeight.SemiBold,
                fontFamily = OutfitFont
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = description,
                color =
                    colors.pagesText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontFamily = OutfitFont
            )
        }
    }
}

@Composable
private fun backupFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppTheme.colors.selectedFieldOutline,
    unfocusedBorderColor = AppTheme.colors.textFieldHint,
    focusedLabelColor = AppTheme.colors.selectedFieldOutline,
    unfocusedLabelColor = AppTheme.colors.textFieldHint,
    focusedTextColor = AppTheme.colors.pagesText,
    unfocusedTextColor = AppTheme.colors.pagesText,
    cursorColor = AppTheme.colors.pagesText
)
// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun CloudBackupScreenPreview() {
    GreenTheme {
        CloudBackupScreen(
            onBack = {}
        )
    }
}
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CloudBackupScreenBluePreview() {
    BlueTheme { CloudBackupScreen(onBack = {}) }
}