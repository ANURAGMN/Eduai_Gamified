package com.ncert7.aitutorandlab.ui.screens.login.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.debug.OnboardingDebugHelper
import com.ncert7.aitutorandlab.service.auth.GoogleSignIn
import com.ncert7.aitutorandlab.service.auth.NetworkException
import com.ncert7.aitutorandlab.service.analytics.FunnelAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.FunnelStep
import com.ncert7.aitutorandlab.ui.theme.ColorHint
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.White
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.ExistingUserSyncState
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.LoginState
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.UserViewModel
import kotlinx.coroutines.launch

@Composable
fun GoogleLoginButton(
    navController: NavController,
    userViewModel: UserViewModel,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val dimens = LocalDimensions.current
    val scope = rememberCoroutineScope()

    val loginState by userViewModel.loginState.collectAsStateWithLifecycle()
    val existingUserSyncState by userViewModel.existingUserSyncState.collectAsStateWithLifecycle()
    var hasNavigated by remember { mutableStateOf(false) }

    // Handle existing user sync completion
    LaunchedEffect(existingUserSyncState) {
        when (val state = existingUserSyncState) {
            is ExistingUserSyncState.Success -> {
                if (!hasNavigated) {
                    hasNavigated = true
                    OnboardingDebugHelper.prepareOnboardingReplayAfterSignIn(context)
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                    userViewModel.resetExistingUserSyncState()
                }
            }
            is ExistingUserSyncState.Error -> {
                onError(
                    state.exception.message?.takeIf { it.isNotBlank() }
                        ?: "Failed to sync user data. Please try again."
                )
                userViewModel.resetExistingUserSyncState()
                userViewModel.resetLoginState()
            }
            else -> {}
        }
    }

    LaunchedEffect(loginState) {
        if (hasNavigated) return@LaunchedEffect

        when (val state = loginState) {
            is LoginState.ExistingUser -> {
                userViewModel.resetLoginState()
                userViewModel.saveExistingUserLocally(context)
            }
            is LoginState.NewUser -> {
                hasNavigated = true
                navController.navigate("userDetailEntry")
            }
            is LoginState.Error -> {
                val errorMessage = when (state.exception) {
                    is NetworkException -> state.exception.message ?: "Network error. Please try again."
                    else -> when {
                        state.exception.message?.contains("busy", ignoreCase = true) == true ||
                            state.exception.message?.contains("quota", ignoreCase = true) == true ->
                            state.exception.message!!
                        state.exception.message?.contains("network", ignoreCase = true) == true ||
                                state.exception.message?.contains("timeout", ignoreCase = true) == true ||
                                state.exception.message?.contains("connection", ignoreCase = true) == true -> {
                            "Network error. Please check your connection and try again."
                        }
                        else -> {
                            state.exception.message?.takeIf { it.isNotBlank() }
                                ?: "Login failed. Please try again."
                        }
                    }
                }
                onError(errorMessage)
                userViewModel.resetLoginState()
            }
            else -> {}
        }
    }

    /**
     * Using rememberLauncherForActivityResult to keep the launcher alive and stable
     * Even if there is an UI update
     */
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        GoogleSignIn.handleSignInActivityResult(
            context = context,
            scope = scope,
            resultCode = result.resultCode,
            data = result.data,
            onLoginSuccess = { firebaseUser ->
                scope.launch {
                    userViewModel.handleGoogleLogin(firebaseUser)
                }
                DebugLogger.debugLog("GoogleLoginButton", "Google sign-in successful: ${firebaseUser.email}")
            },
            onLoginFailed = { error ->
                val errorMessage = when (error) {
                    is NetworkException -> error.message ?: "Network error. Please try again."
                    else -> when {
                        error.message?.contains("network", ignoreCase = true) == true ||
                            error.message?.contains("timeout", ignoreCase = true) == true ||
                            error.message?.contains("connection", ignoreCase = true) == true ->
                            "Network error. Please check your connection and try again."
                        else -> "Sign-in failed. Please try again."
                    }
                }
                onError(errorMessage)
                DebugLogger.debugLog("GoogleLoginButton", "Google sign-in failed: ${error.message}")
            }
        )
    }

    val isLoading = loginState is LoginState.Loading ||
        existingUserSyncState is ExistingUserSyncState.Syncing

    OutlinedButton(
        onClick = {
            if (!isLoading) {
                FunnelAnalyticsTracker.track(FunnelStep.GMAIL_TAP)
                GoogleSignIn.doGoogleSignIn(
                    context = context,
                    scope = scope,
                    launcher = launcher,
                    onLoginSuccess = { firebaseUser ->
                        // Handle login with ViewModel - language is already in ViewModel state
                        scope.launch {
                            userViewModel.handleGoogleLogin(firebaseUser)
                        }
                        DebugLogger.debugLog("GoogleLoginButton", "Google sign-in successful: ${firebaseUser.email}")
                    },
                    onLoginFailed = { error ->
                        // Show error to user - don't show for NoCredentialException
                        val errorMessage = when (error) {
                            is NetworkException -> error.message ?: "Network error. Please try again."
                            else -> when {
                                error.message?.contains("network", ignoreCase = true) == true ||
                                        error.message?.contains("timeout", ignoreCase = true) == true ||
                                        error.message?.contains("connection", ignoreCase = true) == true -> {
                                    "Network error. Please check your connection and try again."
                                }
                                else -> {
                                    "Sign-in failed. Please try again."
                                }
                            }
                        }
                        onError(errorMessage)
                        DebugLogger.debugLog("GoogleLoginButton", "Google sign-in failed: ${error.message}")
                    }
                )
            }
        },
        enabled = !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = White,
            contentColor = TextPrimary
        ),
        border = BorderStroke(dimens.inputBorderWidth, ColorHint),
        shape = RoundedCornerShape(dimens.cornerRadiusMedium),
        modifier = Modifier
            .fillMaxWidth()
            .size(height = dimens.buttonHeight, width = dimens.buttonHeight),
        contentPadding = PaddingValues(horizontal = dimens.buttonPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isLoading) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google),
                    contentDescription = stringResource(R.string.google_icon_desc),
                    modifier = Modifier.size(dimens.iconMedium)
                )
                Spacer(Modifier.width(dimens.spaceSmall + dimens.spaceExtraSmall))
                Text(
                    text = stringResource(R.string.continue_with_google),
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimens.iconMedium),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}