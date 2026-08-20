package com.ncert7.aitutorandlab.ui.screens.login.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.debug.OnboardingDebugHelper
import com.ncert7.aitutorandlab.service.auth.PadaamsEmailAuth
import com.ncert7.aitutorandlab.service.analytics.FunnelAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.FunnelStep
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.ExistingUserSyncState
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.LoginState
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.UserViewModel
import com.ncert7.aitutorandlab.ui.theme.BackgroundPrimary
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import com.ncert7.aitutorandlab.ui.theme.White

@Composable
fun EmailSignInForm(
    navController: NavController,
    userViewModel: UserViewModel,
    onError: (String) -> Unit
) {
    if (!PadaamsEmailAuth.isEnabled()) return

    val context = LocalContext.current
    val dimens = LocalDimensions.current
    var expanded by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var hasNavigated by remember { mutableStateOf(false) }
    var institutionalSignInActive by remember { mutableStateOf(false) }

    val loginState by userViewModel.loginState.collectAsStateWithLifecycle()
    val existingUserSyncState by userViewModel.existingUserSyncState.collectAsStateWithLifecycle()

    val isLoading = loginState is LoginState.Loading ||
        existingUserSyncState is ExistingUserSyncState.Syncing

    LaunchedEffect(existingUserSyncState) {
        if (!institutionalSignInActive && existingUserSyncState !is ExistingUserSyncState.Syncing) {
            return@LaunchedEffect
        }
        when (val state = existingUserSyncState) {
            is ExistingUserSyncState.Success -> {
                if (!hasNavigated) {
                    hasNavigated = true
                    institutionalSignInActive = false
                    OnboardingDebugHelper.prepareOnboardingReplayAfterSignIn(context)
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                    userViewModel.resetExistingUserSyncState()
                    userViewModel.resetLoginState()
                }
            }
            is ExistingUserSyncState.Error -> {
                if (!institutionalSignInActive) return@LaunchedEffect
                onError(
                    state.exception.message?.takeIf { it.isNotBlank() }
                        ?: "Sign-in failed. Please try again."
                )
                institutionalSignInActive = false
                userViewModel.resetExistingUserSyncState()
                userViewModel.resetLoginState()
            }
            else -> {}
        }
    }

    LaunchedEffect(loginState) {
        // Shared loginState is also used by Gmail — ignore errors from the Google flow.
        if (!institutionalSignInActive) return@LaunchedEffect
        if (loginState is LoginState.Error) {
            val msg = (loginState as LoginState.Error).exception.message
            onError(
                msg?.takeIf { it.isNotBlank() }
                    ?: "Invalid credentials. Use your @padaams.in account — not Gmail."
            )
            institutionalSignInActive = false
            userViewModel.resetLoginState()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        if (!expanded) {
            TextButton(
                onClick = {
                    expanded = true
                    FunnelAnalyticsTracker.track(FunnelStep.INSTITUTIONAL_EXPAND)
                },
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(R.string.padaams_sign_in),
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandPrimary
                )
            }
        } else {
            Text(
                text = stringResource(R.string.padaams_sign_in_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimens.spaceSmall))

            // Login card is always light; force dark text so dark app theme cannot wash out the fields.
            val lightFieldColors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    disabledTextColor = TextSecondary,
                    focusedContainerColor = BackgroundPrimary,
                    unfocusedContainerColor = BackgroundPrimary,
                    disabledContainerColor = BackgroundPrimary,
                    cursorColor = BrandPrimary,
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = TextSecondary,
                    focusedLabelColor = TextPrimary,
                    unfocusedLabelColor = TextSecondary,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary,
                    focusedSupportingTextColor = TextSecondary,
                    unfocusedSupportingTextColor = TextSecondary,
                )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim().lowercase() },
                label = { Text(stringResource(R.string.padaams_email_label)) },
                placeholder = { Text(stringResource(R.string.padaams_email_hint)) },
                supportingText = {
                    Text(stringResource(R.string.padaams_email_helper))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = lightFieldColors,
            )

            Spacer(modifier = Modifier.height(dimens.spaceSmall))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = lightFieldColors,
            )

            Spacer(modifier = Modifier.height(dimens.spaceMedium))

            Button(
                onClick = {
                    if (isLoading) return@Button
                    if (!PadaamsEmailAuth.isPadaamsEmail(email)) {
                        onError("Use your @padaams.in email. For Gmail, tap Continue with Gmail above.")
                        return@Button
                    }
                    FunnelAnalyticsTracker.track(FunnelStep.INSTITUTIONAL_SIGN_IN)
                    institutionalSignInActive = true
                    userViewModel.signInWithPadaamsEmail(context, email, password)
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandPrimary,
                    contentColor = White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.sign_in))
                }
            }

            TextButton(
                onClick = {
                    if (!isLoading) {
                        expanded = false
                        email = ""
                        password = ""
                        institutionalSignInActive = false
                    }
                },
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
