package com.ncert7.aitutorandlab.ui.screens.setting

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ncert7.aitutorandlab.notification.NotificationPermissionHelper
import com.ncert7.aitutorandlab.notification.NotificationReminderMode
import com.ncert7.aitutorandlab.ui.screens.setting.viewmodel.NotificationSettingsViewModel
import com.ncert7.aitutorandlab.ui.theme.AccentBlue
import com.ncert7.aitutorandlab.ui.theme.BackgroundSecondary
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.IconSecondary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextOnPrimary
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dimens = LocalDimensions.current
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showQuietStartPicker by remember { mutableStateOf(false) }
    var showQuietEndPicker by remember { mutableStateOf(false) }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (granted) {
                viewModel.onPermissionGranted()
            } else {
                viewModel.refreshFromStorage()
            }
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshFromStorage()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notifications",
                        fontWeight = FontWeight.SemiBold,
                        color = TextOnPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextOnPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPrimary),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(BackgroundSecondary)
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
        ) {
            SettingsSection(title = "General") {
                if (uiState.showSystemSettingsFallback) {
                    Text(
                        text = "Notifications are blocked at the system level.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = dimens.spaceSmall),
                    )
                    Button(
                        onClick = { NotificationPermissionHelper.openAppNotificationSettings(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Enable in system settings")
                    }
                } else {
                    NotificationToggleRow(
                        title = "Notifications",
                        subtitle = "Daily reminders and streak alerts",
                        checked = uiState.masterEnabled && uiState.osPermissionGranted,
                        onCheckedChange = { enabled ->
                            if (enabled && !uiState.osPermissionGranted) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.onMasterToggle(true)
                                }
                            } else {
                                viewModel.onMasterToggle(enabled)
                            }
                        },
                    )
                }
            }

            SettingsSection(title = "Daily reminder") {
                NotificationValueRow(
                    title = "Reminder time",
                    value = formatClockTime(uiState.reminderHour, uiState.reminderMinute),
                    enabled = uiState.masterEnabled && uiState.reminderMode != NotificationReminderMode.OFF,
                    onClick = { showReminderTimePicker = true },
                )
                Text(
                    text = "Reminder mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = dimens.spaceSmall),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                ) {
                    ReminderModeChip(
                        label = "Off",
                        selected = uiState.reminderMode == NotificationReminderMode.OFF,
                        onClick = { viewModel.onReminderModeChanged(NotificationReminderMode.OFF) },
                        modifier = Modifier.weight(1f),
                    )
                    ReminderModeChip(
                        label = "Gentle",
                        selected = uiState.reminderMode == NotificationReminderMode.GENTLE,
                        onClick = { viewModel.onReminderModeChanged(NotificationReminderMode.GENTLE) },
                        modifier = Modifier.weight(1f),
                    )
                    ReminderModeChip(
                        label = "Standard",
                        selected = uiState.reminderMode == NotificationReminderMode.STANDARD,
                        onClick = { viewModel.onReminderModeChanged(NotificationReminderMode.STANDARD) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text =
                        when (uiState.reminderMode) {
                            NotificationReminderMode.OFF -> "No scheduled reminders."
                            NotificationReminderMode.GENTLE -> "Up to 1 notification per day."
                            NotificationReminderMode.STANDARD -> "Up to 3 notifications per day."
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            SettingsSection(title = "Categories") {
                uiState.categoryEnabled.forEach { (category, enabled) ->
                    NotificationToggleRow(
                        title = category.channelLabel,
                        subtitle = null,
                        checked = enabled,
                        onCheckedChange = { viewModel.onCategoryToggle(category, it) },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    NotificationPermissionHelper.openChannelNotificationSettings(
                                        context,
                                        category.channelId,
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Open Android channel settings",
                                    tint = IconSecondary,
                                )
                            }
                        },
                    )
                }
            }

            SettingsSection(title = "Quiet hours") {
                NotificationValueRow(
                    title = "Start",
                    value = formatHourLabel(uiState.quietHoursStart),
                    enabled = true,
                    onClick = { showQuietStartPicker = true },
                )
                NotificationValueRow(
                    title = "End",
                    value = formatHourLabel(uiState.quietHoursEnd),
                    enabled = true,
                    onClick = { showQuietEndPicker = true },
                )
                Text(
                    text = "No notifications are sent during quiet hours.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }

    if (showReminderTimePicker) {
        HourMinutePickerDialog(
            title = "Daily reminder time",
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            onDismiss = { showReminderTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.onReminderTimeChanged(hour, minute)
                showReminderTimePicker = false
            },
        )
    }

    if (showQuietStartPicker) {
        HourPickerDialog(
            title = "Quiet hours start",
            initialHour = uiState.quietHoursStart,
            onDismiss = { showQuietStartPicker = false },
            onConfirm = { hour ->
                viewModel.onQuietHoursChanged(hour, uiState.quietHoursEnd)
                showQuietStartPicker = false
            },
        )
    }

    if (showQuietEndPicker) {
        HourPickerDialog(
            title = "Quiet hours end",
            initialHour = uiState.quietHoursEnd,
            onDismiss = { showQuietEndPicker = false },
            onConfirm = { hour ->
                viewModel.onQuietHoursChanged(uiState.quietHoursStart, hour)
                showQuietEndPicker = false
            },
        )
    }
}

@Composable
private fun ReminderModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
    )
}

@Composable
private fun NotificationToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val dimens = LocalDimensions.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = dimens.spaceSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            subtitle?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            trailingContent?.invoke()
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun NotificationValueRow(
    title: String,
    value: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val dimens = LocalDimensions.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = dimens.spaceSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) AccentBlue else TextSecondary,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourMinutePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HourPickerDialog(
    title: String,
    initialHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int) -> Unit,
) {
    val state =
        rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = 0,
            is24Hour = false,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun formatClockTime(hour: Int, minute: Int): String {
    val normalizedHour = hour.coerceIn(0, 23)
    val normalizedMinute = minute.coerceIn(0, 59)
    val amPm = if (normalizedHour < 12) "AM" else "PM"
    val displayHour =
        when (val h = normalizedHour % 12) {
            0 -> 12
            else -> h
        }
    return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, normalizedMinute, amPm)
}

private fun formatHourLabel(hour: Int): String {
    val normalizedHour = hour.coerceIn(0, 23)
    val amPm = if (normalizedHour < 12) "AM" else "PM"
    val displayHour =
        when (val h = normalizedHour % 12) {
            0 -> 12
            else -> h
        }
    return "$displayHour:00 $amPm"
}
