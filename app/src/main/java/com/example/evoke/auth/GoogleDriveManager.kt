package com.example.evoke.auth

import android.app.Activity
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class GoogleDriveManager {

    companion object {
        private const val DRIVE_APP_DATA_SCOPE =
            "https://www.googleapis.com/auth/drive.appdata"
    }

    private val driveScope =
        Scope(DRIVE_APP_DATA_SCOPE)

    suspend fun requestDriveAccess(
        activity: Activity
    ): DriveAuthorizationResult {

        // Check internet before requesting Drive authorization.
        if (!isInternetAvailable(activity)) {
            throw NoInternetConnectionException()
        }

        val request =
            AuthorizationRequest.Builder()
                .setRequestedScopes(
                    listOf(driveScope)
                )
                .build()

        return try {

            val authorizationResult =
                Identity
                    .getAuthorizationClient(activity)
                    .authorize(request)
                    .await()

            if (authorizationResult.hasResolution()) {

                DriveAuthorizationResult.NeedsResolution(
                    authorizationResult.pendingIntent
                )

            } else {

                DriveAuthorizationResult.Authorized(
                    authorizationResult.accessToken
                )
            }

        } catch (e: CancellationException) {

            throw e

        } catch (e: Exception) {

            if (!isInternetAvailable(activity)) {
                throw NoInternetConnectionException()
            }

            throw e
        }
    }

    fun getAuthorizationResultFromIntent(
        activity: Activity,
        data: Intent?
    ): DriveAuthorizationResult {

        if (data == null) {
            return DriveAuthorizationResult.Failed(
                "Authorization was cancelled."
            )
        }

        return try {

            val result =
                Identity
                    .getAuthorizationClient(activity)
                    .getAuthorizationResultFromIntent(data)

            DriveAuthorizationResult.Authorized(
                result.accessToken
            )

        } catch (e: Exception) {

            if (!isInternetAvailable(activity)) {

                DriveAuthorizationResult.Failed(
                    "No internet connection. Please connect to the internet and try again."
                )

            } else {

                DriveAuthorizationResult.Failed(
                    "Google Drive authorization failed. Please try again."
                )
            }
        }
    }

    private fun isInternetAvailable(
        activity: Activity
    ): Boolean {

        val connectivityManager =
            activity.getSystemService(
                Activity.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }
}

sealed class DriveAuthorizationResult {

    data class Authorized(
        val accessToken: String?
    ) : DriveAuthorizationResult()

    data class NeedsResolution(
        val pendingIntent:
        android.app.PendingIntent?
    ) : DriveAuthorizationResult()

    data class Failed(
        val message: String
    ) : DriveAuthorizationResult()
}
