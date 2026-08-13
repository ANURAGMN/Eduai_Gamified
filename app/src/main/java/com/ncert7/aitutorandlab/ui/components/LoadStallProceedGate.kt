package com.ncert7.aitutorandlab.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AppDialog
import kotlinx.coroutines.delay

/**
 * Offers "continue to next section" when content is stuck or failed:
 * - [waiting] true for [stallAfterMs] (default 15s) — loading / no URL / init spinner
 * - [errorMessage] non-blank — **immediate** dialog for app/server/error text
 *
 * Localized via string resources (EN / KN / …). "Keep waiting" dismisses and restarts the
 * stall timer (errors still re-show if [errorMessage] remains). Clearing waiting + error hides it.
 */
@Composable
fun LoadStallProceedGate(
    waiting: Boolean,
    onContinue: () -> Unit,
    resetKey: Any? = null,
    stallAfterMs: Long = 15_000L,
    /** App / server / load error — shows the dialog immediately when non-blank. */
    errorMessage: String? = null,
) {
    val hasError = !errorMessage.isNullOrBlank()
    var showDialog by remember { mutableStateOf(false) }
    var timerGeneration by remember { mutableIntStateOf(0) }
    val currentOnContinue by rememberUpdatedState(onContinue)
    val stallMessage = stringResource(R.string.load_stall_message)

    LaunchedEffect(waiting, hasError, errorMessage, timerGeneration, resetKey) {
        if (!waiting && !hasError) {
            showDialog = false
            return@LaunchedEffect
        }
        if (hasError) {
            // Errors surface right away — don't make the learner wait out the stall timer.
            showDialog = true
            return@LaunchedEffect
        }
        showDialog = false
        delay(stallAfterMs)
        if (waiting) showDialog = true
    }

    val title = if (hasError) {
        stringResource(R.string.load_stall_error_title)
    } else {
        stringResource(R.string.load_stall_title)
    }
    val body = if (hasError) {
        buildString {
            append(errorMessage!!.trim())
            append("\n\n")
            append(stallMessage)
        }
    } else {
        stallMessage
    }

    AppDialog(
        show = showDialog,
        title = title,
        message = body,
        confirmText = stringResource(R.string.load_stall_continue),
        dismissText = if (hasError) {
            stringResource(R.string.load_stall_dismiss_error)
        } else {
            stringResource(R.string.load_stall_keep_waiting)
        },
        onConfirm = {
            showDialog = false
            currentOnContinue()
        },
        onDismiss = {
            showDialog = false
            if (!hasError) {
                // Restart the 15s clock so they aren't stuck if load never recovers.
                timerGeneration++
            }
        },
    )
}
