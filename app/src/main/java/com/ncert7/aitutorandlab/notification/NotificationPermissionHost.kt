package com.ncert7.aitutorandlab.notification

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode
import kotlinx.coroutines.delay

@Composable
fun NotificationPermissionHost() {
    val context = LocalContext.current
    val prefs = remember { SharedPreferenceUtils(context) }
    val languageCode =
        remember {
            normalizeLanguageCode(prefs.getLanguagePreference())
        }
    var pendingVariant by remember { mutableStateOf<NotificationPrimerVariant?>(null) }
    var startupPromptLaunched by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            EngagementAnalyticsTracker.notificationPermissionResult(granted)
            prefs.setAskedNotificationPermission(true)
            pendingVariant = null
            if (granted) {
                prefs.setNotificationsEnabled(true)
                NotificationScheduler.scheduleAll(context.applicationContext)
            }
        }

    // Request OS permission once after login on cold start (product: ask automatically).
    LaunchedEffect(Unit) {
        if (startupPromptLaunched) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        if (!prefs.isLoggedIn()) return@LaunchedEffect
        if (NotificationPermissionHelper.hasPostNotificationsPermission(context)) return@LaunchedEffect
        if (prefs.hasAskedNotificationPermission()) return@LaunchedEffect
        delay(900)
        startupPromptLaunched = true
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    LaunchedEffect(Unit) {
        NotificationPermissionGate.primerRequests.collect { variant ->
            pendingVariant = variant
        }
    }

    pendingVariant?.let { variant ->
        NotificationPermissionPrimerDialog(
            variant = variant,
            languageCode = languageCode,
            onAccept = {
                EngagementAnalyticsTracker.notificationPrimerAccepted(variant.name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    prefs.setAskedNotificationPermission(true)
                    prefs.setNotificationsEnabled(true)
                    NotificationScheduler.scheduleAll(context.applicationContext)
                    pendingVariant = null
                }
            },
            onDecline = {
                // "Not now" / dismiss — the gate already counted this show, so it simply reappears on
                // a later day (next persuasion) up to the cap.
                EngagementAnalyticsTracker.notificationPrimerDeclined(variant.name)
                pendingVariant = null
            },
        )
    }
}
