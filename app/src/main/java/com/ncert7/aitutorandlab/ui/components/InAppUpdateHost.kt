package com.ncert7.aitutorandlab.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.InAppUpdateViewModel

/**
 * Checks Play in-app updates on launch and resume, starts the native update UI when available,
 * and prompts to restart after a flexible update finishes downloading.
 */
@Composable
fun InAppUpdateHost(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    if (activity == null) {
        content()
        return
    }

    val viewModel: InAppUpdateViewModel = hiltViewModel()
    val updateState by viewModel.updateState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, activity) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.checkForUpdate(activity)
                    viewModel.checkResumeState(activity)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.checkForUpdate(activity)
    }

    LaunchedEffect(updateState.updateAvailable, updateState.isDownloading) {
        if (updateState.updateAvailable && !updateState.isDownloading) {
            viewModel.startUpdate(activity)
        }
    }

    LaunchedEffect(updateState.updateInstalled) {
        if (!updateState.updateInstalled) return@LaunchedEffect
        val result =
            snackbarHostState.showSnackbar(
                message = "Update downloaded — restart to apply",
                actionLabel = "Restart",
            )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.completeUpdate(activity)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
        )
    }
}
