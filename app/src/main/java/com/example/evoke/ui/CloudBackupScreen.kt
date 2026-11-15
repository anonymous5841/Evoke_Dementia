package com.example.evoke.ui

import com.example.evoke.R
import android.app.Activity
import android.content.Context
import android.content.Intent
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
import com.example.evoke.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


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

    var restartRequired by remember { mutableStateOf(false) }

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

    suspend fun refreshDriveToken(): String {
        val act = activity ?: throw IllegalStateException(context.getString(R.string.cb_no_activity))
        val result = googleDriveManager.requestDriveAccess(act)
        val token = (result as? DriveAuthorizationResult.Authorized)?.accessToken
        if (token.isNullOrEmpty()) {
            throw IllegalStateException(context.getString(R.string.cb_drive_auth_expired))
        }
        driveAccessToken = token
        return token
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
                    context.getString(R.string.cb_no_activity)

                return@rememberLauncherForActivityResult
            }

            if (result.resultCode != Activity.RESULT_OK) {

                driveAuthorized = false
                driveAccessToken = null

                statusMessage =
                    context.getString(R.string.cb_drive_auth_cancelled)

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
                                context.getString(R.string.cb_drive_auth_failed)

                        } else {

                            driveAuthorized = true
                            driveAccessToken = token

                            statusMessage =
                                context.getString(R.string.cb_drive_connected)
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
                            context.getString(R.string.cb_additional_auth)
                    }
                }

            } catch (e: Exception) {

                driveAuthorized = false
                driveAccessToken = null

                statusMessage =
                    e.message
                        ?: context.getString(R.string.cb_drive_auth_failed)
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
                    text = context.getString(R.string.cb_replace_existing_backup_title),
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text = context.getString(R.string.cb_replace_existing_backup_message),
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
                        text = context.getString(R.string.cb_replace_backup),
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
                        text = context.getString(R.string.cb_cancel),
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
                    text = context.getString(R.string.cb_create_password_title),
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {

                Column {

                    Text(
                        text =
                            context.getString(R.string.cb_create_password_message),
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
                                text = context.getString(R.string.cb_backup_password_hint),
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
                                text = context.getString(R.string.cb_confirm_password),
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
                                    context.getString(R.string.cb_enter_password)
                            }

                            password.length < 8 -> {

                                passwordError =
                                    context.getString(R.string.cb_password_min_length)
                            }

                            password != confirmPassword -> {

                                passwordError =
                                    context.getString(R.string.cb_passwords_mismatch)
                            }

                            driveAccessToken.isNullOrEmpty() -> {

                                passwordError =
                                    context.getString(R.string.cb_drive_not_authorized)
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
                                        val freshToken = refreshDriveToken()   // <-- new

                                        statusMessage = context.getString(R.string.cb_creating_encrypted)

                                        val packageDirectory = withContext(Dispatchers.IO) {
                                            backupPackageManager.createBackup(passwordChars)
                                        }
                                        try {

                                            statusMessage =
                                                context.getString(R.string.cb_uploading_encrypted)

                                            val uploadResult =
                                                googleDriveBackupManager
                                                    .uploadBackup(
                                                        accessToken = freshToken,
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
                                                        context.getString(R.string.cb_backup_completed)

                                                    Log.d("CloudBackup", "Backup completed")
                                                    showPasswordDialog = false
                                                }

                                                is com.example.evoke.auth.DriveBackupUploadResult.Failed -> {

                                                    statusMessage =
                                                        context.getString(R.string.cb_backup_upload_failed_detail, "${uploadResult.message}")
                                                }
                                            }


                                        } catch (e: NoInternetConnectionException) {

                                            statusMessage =
                                                context.getString(R.string.cb_no_internet)

                                        } catch (e: kotlinx.coroutines.CancellationException) {

                                            throw e

                                        } catch (e: Exception) {

                                            statusMessage =
                                                context.getString(R.string.cb_backup_upload_failed)

                                            Log.e("CloudBackup", "Backup upload failed", e)

                                        } finally {

                                            packageDirectory.deleteRecursively()
                                        }

                                    } catch (e: Exception) {

                                        statusMessage =
                                            context.getString(R.string.cb_backup_creation_failed_detail, e.message ?: "")

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
                                context.getString(R.string.cb_backing_up)
                            else
                                context.getString(R.string.cb_create_backup),
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
                        text = context.getString(R.string.cb_cancel),
                        fontFamily = OutfitFont
                    )
                }
            }
        )
    }

    if (restartRequired) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = colors.background,
            titleContentColor = colors.pagesText,
            textContentColor = colors.pagesText,
            title = { Text(context.getString(R.string.cb_restart_required_title), fontFamily = OutfitFont, fontWeight = FontWeight.Bold) },
            text = { Text(context.getString(R.string.cb_restart_required_message), fontFamily = OutfitFont) },
            confirmButton = {
                TextButton(
                    onClick = { restartApp(context) },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.pagesText)
                ) { Text(context.getString(R.string.cb_restart), fontFamily = OutfitFont) }
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
                    text = context.getString(R.string.cb_replace_existing_data_title),
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text = context.getString(R.string.cb_replace_existing_data_message),
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
                        text = context.getString(R.string.cb_continue),
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
                        text = context.getString(R.string.cb_cancel),
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
                    text = context.getString(R.string.cb_restore_my_data),
                    fontFamily = OutfitFont,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {

                Column {

                    Text(
                        text =
                            context.getString(R.string.cb_restore_password_message),
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
                                text = context.getString(R.string.cb_backup_password_label),
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
                                context.getString(R.string.cb_password_min_length)

                            return@TextButton
                        }

                        if (driveAccessToken.isNullOrEmpty()) {

                            restorePasswordError =
                                context.getString(R.string.cb_drive_not_authorized)

                            return@TextButton
                        }

                        val passwordChars =
                            restorePassword.toCharArray()

                        restorePassword = ""

                        isRestoring = true

                        scope.launch {

                            var restoreDirectory: File? = null

                            try {
                                val freshToken = refreshDriveToken()   // <-- new

                                statusMessage =
                                    context.getString(R.string.cb_downloading_encrypted)

                                restoreDirectory =
                                    File(
                                        context.cacheDir,
                                        "restore_${System.currentTimeMillis()}"
                                    )

                                val downloadResult =
                                    googleDriveBackupManager
                                        .downloadBackup(
                                            accessToken = freshToken,
                                            outputDirectory =
                                                restoreDirectory
                                        )

                                when (downloadResult) {

                                    is com.example.evoke.auth.DriveBackupDownloadResult.Failed -> {

                                        statusMessage =
                                            context.getString(R.string.cb_backup_download_failed_detail, "${downloadResult.message}")
                                    }

                                    is com.example.evoke.auth.DriveBackupDownloadResult.Success -> {

                                        val backupPackageDirectory =
                                            downloadResult.backupFile.parentFile
                                                ?: throw IllegalStateException("Invalid restore directory.")

                                        statusMessage = context.getString(R.string.cb_restoring_data)

                                        withContext(Dispatchers.IO) {
                                            backupPackageManager.restoreBackup(
                                                packageDirectory = backupPackageDirectory,
                                                password = passwordChars
                                            )
                                        }

                                        statusMessage = context.getString(R.string.cb_data_restored)

                                        Log.d("CloudBackup", "Restore completed")

                                        showRestorePasswordDialog = false
                                        restartRequired = true
                                    }
                                }

                            } catch (e: NoInternetConnectionException) {

                                statusMessage =
                                    context.getString(R.string.cb_no_internet)

                            } catch (e: kotlinx.coroutines.CancellationException) {

                                throw e

                            } catch (e: Exception) {

                                statusMessage =
                                    context.getString(R.string.cb_restore_failed)

                                Log.e("CloudBackup", "Restore failed", e)

                                if (!AppDatabase.isOpen()) {
                                    showRestorePasswordDialog = false
                                    restartRequired = true
                                }

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
                                context.getString(R.string.cb_restoring)
                            else
                                context.getString(R.string.cb_restore),
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
                        text = context.getString(R.string.cb_cancel),
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
                                            context.getString(R.string.cb_cloud_backup_enabled)
                                        else
                                            context.getString(R.string.cloud_backup),
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
                                                context.getString(R.string.cb_status_drive_connected)

                                            backupEnabled ->
                                                context.getString(R.string.cb_status_ready)

                                            else ->
                                                context.getString(R.string.cb_status_default)
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
                                    text = context.getString(R.string.cb_enable_cloud_backup),
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
                                        context.getString(R.string.cb_enable_cloud_backup_desc),
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
                                    text = context.getString(R.string.cb_data_protected),
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
                                title = context.getString(R.string.cb_encrypted_backup),
                                description =
                                    context.getString(R.string.cb_encrypted_backup_desc)
                            )

                            Spacer(
                                modifier = Modifier.height(14.dp)
                            )

                            BackupSecurityItem(
                                icon = Icons.Default.Storage,
                                title = context.getString(R.string.cb_private_storage),
                                description =
                                    context.getString(R.string.cb_private_storage_desc)
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
                                text = context.getString(R.string.cb_google_account),
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
                                            context.getString(R.string.cb_connected_to, googleAccount?.displayName ?: context.getString(R.string.cb_your_google_account))

                                        backupEnabled ->
                                            context.getString(R.string.cb_connect_account_prompt)

                                        else ->
                                            context.getString(R.string.cb_connect_account_later)
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
                                                context.getString(R.string.cb_no_activity)

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
                                                        context.getString(R.string.cb_account_connected)
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
                                                                context.getString(R.string.cb_drive_auth_failed)

                                                        } else {

                                                            driveAuthorized =
                                                                true

                                                            driveAccessToken =
                                                                token

                                                            statusMessage =
                                                                context.getString(R.string.cb_drive_connected)
                                                        }
                                                    }

                                                    is DriveAuthorizationResult.NeedsResolution -> {

                                                        val pendingIntent =
                                                            driveResult.pendingIntent

                                                        if (pendingIntent == null) {

                                                            statusMessage =
                                                                context.getString(R.string.cb_drive_auth_start_failed)

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
                                                    context.getString(R.string.cb_no_internet)

                                            } catch (e: kotlinx.coroutines.CancellationException) {

                                                throw e

                                            } catch (e: Exception) {

                                                statusMessage =
                                                    context.getString(R.string.cb_account_connect_failed)

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
                                                    context.getString(R.string.cb_drive_connected_button)

                                                googleAccount != null ->
                                                    context.getString(R.string.cb_authorize_drive)

                                                else ->
                                                    context.getString(R.string.cb_connect_google_account)
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
                                        isCheckingExistingBackup = true

                                        scope.launch {
                                            try {
                                                val token = refreshDriveToken()

                                                statusMessage =
                                                    context.getString(R.string.cb_checking_existing)

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
                                            } catch (e: NoInternetConnectionException) {
                                                statusMessage =
                                                    context.getString(R.string.cb_no_internet)
                                            } catch (e: Exception) {
                                                statusMessage =
                                                    e.message ?: context.getString(R.string.cb_check_existing_failed)
                                                Log.e("CloudBackup", "Existing backup check failed", e)
                                            } finally {
                                                isCheckingExistingBackup = false
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
                                                context.getString(R.string.cb_creating_backup)
                                            else
                                                context.getString(R.string.cb_back_up_my_data),
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
                                        text = if (isRestoring) context.getString(R.string.cb_restoring) else context.getString(R.string.cb_restore_my_data),
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
                            context.getString(R.string.cb_optional_info),
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
//                title = context.getString(R.string.cloud_backup),
//                spacing = 72.dp,
//                textSize = 39.sp,
//                bottomspace = 30.dp,
//                leaves = 9.dp,
//                headerHeight = 218.dp,
//                onBack = onBack
//            )
            HeaderSection(
                title = context.getString(R.string.cloud_backup),

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
//Restart after restore (CloudBackupScreen.kt)
private fun restartApp(context: Context) {
    val intent = context.packageManager
        .getLaunchIntentForPackage(context.packageName)!!
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
    Runtime.getRuntime().exit(0)
}
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