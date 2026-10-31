package com.example.evoke.auth

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.evoke.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException

class GoogleSignInManager(
    private val context: Context
) {

    private val credentialManager =
        CredentialManager.create(context)

    suspend fun signIn(activity: Activity): GoogleAccount {

        // Check internet before starting Google Sign-In.
        if (!isInternetAvailable()) {
            throw NoInternetConnectionException()
        }

        try {

            val googleSignInOption =
                GetSignInWithGoogleOption.Builder(
                    serverClientId = BuildConfig.W8jnNFpds9I1z1xkRCdn
                )
                    .build()

            val request =
                GetCredentialRequest.Builder()
                    .addCredentialOption(googleSignInOption)
                    .build()

            val result =
                credentialManager.getCredential(
                    context = activity,
                    request = request
                )

            val credential =
                GoogleIdTokenCredential.createFrom(
                    result.credential.data
                )

            return GoogleAccount(
                id = credential.id,
                displayName = credential.displayName,
                profilePictureUri =
                    credential.profilePictureUri?.toString()
            )

        } catch (e: CancellationException) {

            // Never convert coroutine cancellation into a network error.
            throw e

        } catch (e: Exception) {

            // If internet disconnected during sign-in,
            // provide a meaningful error to the UI.
            if (!isInternetAvailable()) {
                throw NoInternetConnectionException()
            }

            // Otherwise preserve the original exception.
            throw e
        }
    }

    private fun isInternetAvailable(): Boolean {

        val connectivityManager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            connectivityManager.activeNetwork ?: return false

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

class NoInternetConnectionException :
    Exception("No internet connection")

data class GoogleAccount(
    val id: String,
    val displayName: String?,
    val profilePictureUri: String?
)
