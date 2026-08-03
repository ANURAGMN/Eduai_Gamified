package com.ncert7.aitutorandlab.service.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn as GmsGoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.config.AppConfig
import com.ncert7.aitutorandlab.data.firebase.model.User
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.utils.GoogleInfoExtractor
import com.ncert7.aitutorandlab.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class GoogleSignIn {

    companion object {
        private const val TAG = "GoogleSignIn"

        fun doGoogleSignIn(
            context: Context,
            scope: CoroutineScope,
            launcher: ManagedActivityResultLauncher<Intent, ActivityResult>?,
            onLoginSuccess: (user: User) -> Unit,
            onLoginFailed: (error: Throwable) -> Unit
        ) {
            val credentialManager = CredentialManager.create(context)
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(getCredentialOptions())
                .build()

            scope.launch {
                try {
                    val result = credentialManager.getCredential(context, request)
                    when (result.credential) {
                        is CustomCredential -> {
                            if (result.credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(result.credential.data)
                                scope.launch {
                                    completeSignIn(
                                        context = context,
                                        user = GoogleInfoExtractor.extractAndLogUserInfo(googleIdTokenCredential),
                                        idToken = googleIdTokenCredential.idToken,
                                        onLoginSuccess = onLoginSuccess,
                                        onLoginFailed = onLoginFailed,
                                    )
                                }
                            } else {
                                onLoginFailed(
                                    IllegalStateException("Unexpected credential type: ${result.credential.type}")
                                )
                            }
                        }
                        else -> onLoginFailed(IllegalStateException("Unknown credential object"))
                    }
                } catch (e: NoCredentialException) {
                    DebugLogger.debugLog(TAG, "No cached credential — opening Google account picker")
                    launcher?.launch(createAccountPickerIntent(context))
                        ?: onLoginFailed(IllegalStateException("Unable to open Google sign-in"))
                } catch (e: GetCredentialException) {
                    if (isNetworkError(e)) {
                        onLoginFailed(
                            NetworkException("Network error. Please check your connection and try again.", e)
                        )
                    } else {
                        DebugLogger.debugLog(TAG, "Credential Manager failed, using account picker: ${e.message}")
                        launcher?.launch(createAccountPickerIntent(context))
                            ?: onLoginFailed(e)
                    }
                } catch (e: SocketTimeoutException) {
                    onLoginFailed(
                        NetworkException("Connection timeout. Please check your internet connection and try again.", e)
                    )
                } catch (e: UnknownHostException) {
                    onLoginFailed(
                        NetworkException("No internet connection. Please check your network and try again.", e)
                    )
                } catch (e: IOException) {
                    onLoginFailed(NetworkException("Network error occurred. Please try again.", e))
                } catch (e: Exception) {
                    DebugLogger.errorLog(TAG, "Unexpected exception: ${e.message}")
                    onLoginFailed(e)
                }
            }
        }

        /** Legacy Google Sign-In intent — shows accounts already on the device. */
        fun createAccountPickerIntent(context: Context): Intent =
            getGoogleSignInClient(context).signInIntent

        fun handleSignInActivityResult(
            context: Context,
            scope: CoroutineScope,
            resultCode: Int,
            data: Intent?,
            onLoginSuccess: (user: User) -> Unit,
            onLoginFailed: (error: Throwable) -> Unit
        ) {
            if (resultCode != Activity.RESULT_OK) {
                DebugLogger.debugLog(TAG, "Google sign-in cancelled (resultCode=$resultCode)")
                return
            }
            try {
                val account = GmsGoogleSignIn
                    .getSignedInAccountFromIntent(data)
                    .getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    onLoginFailed(IllegalStateException("Google sign-in returned no ID token"))
                    return
                }
                scope.launch {
                    completeSignIn(
                        context = context,
                        user = userFromGoogleAccount(account),
                        idToken = idToken,
                        onLoginSuccess = onLoginSuccess,
                        onLoginFailed = onLoginFailed,
                    )
                }
            } catch (e: ApiException) {
                when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED ->
                        DebugLogger.debugLog(TAG, "User cancelled Google account picker")
                    else -> {
                        DebugLogger.errorLog(TAG, "Google sign-in failed: ${e.statusCode} ${e.message}")
                        onLoginFailed(e)
                    }
                }
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "Error parsing Google sign-in result: ${e.message}")
                onLoginFailed(e)
            }
        }

        private fun getGoogleSignInClient(context: Context): GoogleSignInClient {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(BuildConfig.AUTH_KEY)
                .requestEmail()
                .build()
            return GmsGoogleSignIn.getClient(context, options)
        }

        private fun getCredentialOptions() =
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(BuildConfig.AUTH_KEY)
                .build()

        private suspend fun completeSignIn(
            context: Context,
            user: User,
            idToken: String,
            onLoginSuccess: (User) -> Unit,
            onLoginFailed: (Throwable) -> Unit,
        ) {
            try {
                val firebaseUser = FirebaseAuthBridge.signInWithGoogleIdToken(idToken)
                TokenManager.saveIdToken(context, idToken)
                val resolvedEmail = firebaseUser.email?.trim().orEmpty()
                onLoginSuccess(
                    user.copy(email = resolvedEmail.ifBlank { user.email }),
                )
            } catch (e: Exception) {
                DebugLogger.errorLog(TAG, "Firebase Auth sign-in failed: ${e.message}")
                onLoginFailed(e)
            }
        }

        private fun userFromGoogleAccount(account: GoogleSignInAccount): User {
            val email = account.email.orEmpty()
            return User(
                id = account.id.orEmpty().ifBlank { email },
                email = email,
                displayName = account.displayName,
                profilePictureUri = account.photoUrl?.toString(),
                studentClass = 7,
                jwtToken = account.idToken.orEmpty(),
                appName = AppConfig.APP_NAME
            ).also {
                DebugLogger.debugLog(TAG, "Account picker sign-in: ${it.email}")
            }
        }

        private fun isNetworkError(exception: Throwable): Boolean {
            val message = exception.message?.lowercase().orEmpty()
            val cause = exception.cause
            return message.contains("network") ||
                message.contains("timeout") ||
                message.contains("connection") ||
                message.contains("unable to resolve host") ||
                message.contains("failed to connect") ||
                cause is SocketTimeoutException ||
                cause is UnknownHostException ||
                cause is IOException
        }
    }
}

class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)
