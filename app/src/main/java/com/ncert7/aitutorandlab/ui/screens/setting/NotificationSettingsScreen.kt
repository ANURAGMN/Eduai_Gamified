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
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker
import com.ncert7.aitutorandlab.ui.screens.setting.viewmodel.NotificationSettingsViewModel
import com.ncert7.aitutorandlab.ui.theme.AccentBlue
import com.ncert7.aitutorandlab.ui.theme.BackgroundSecondary
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.IconSecondary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextOnPrimary
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import com.ncert7.aitutorandlab.utils.NotificationSettingsCopy
import com.ncert7.aitutorandlab.utils.RewardMomentCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
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
    val language = getCurrentLanguageCode()
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
                        NotificationSettingsCopy.screenTitle(language),
                        fontWeight = FontWeight.SemiBold,
                        color = TextOnPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                NotificationSettingsCopy.backContentDescription(language),
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
            SettingsSection(title = NotificationSettingsCopy.generalSection(language)) {
                if (uiState.showSystemSettingsFallback) {
                    Text(
                        text = NotificationSettingsCopy.blockedAtSystem(language),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = dimens.spaceSmall),
                    )
                    Button(
                        onClick = {
                            EngagementAnalyticsTracker.notifPrefOpenSystem("app")
                            NotificationPermissionHelper.openAppNotificationSettings(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(NotificationSettingsCopy.enableInSystemSettings(language))
                    }
                } else {
                    NotificationToggleRow(
                        title = NotificationSettingsCopy.notificationsToggleTitle(language),
                        subtitle = NotificationSettingsCopy.notificationsToggleSubtitle(language),
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

            SettingsSection(title = NotificationSettingsCopy.dailyReminderSection(language)) {
                NotificationValueRow(
                    title = NotificationSettingsCopy.reminderTime(language),
                    value = formatClockTime(uiState.reminderHour, uiState.reminderMinute),
                    enabled = uiState.masterEnabled && uiState.reminderMode != NotificationReminderMode.OFF,
                    onClick = { showReminderTimePicker = true },
                )
                Text(
                    text = NotificationSettingsCopy.reminderMode(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = dimens.spaceSmall),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                ) {
                    ReminderModeChip(
                        label = NotificationSettingsCopy.modeOff(language),
                        selected = uiState.reminderMode == NotificationReminderMode.OFF,
                        onClick = { viewModel.onReminderModeChanged(NotificationReminderMode.OFF) },
                        modifier = Modifier.weight(1f),
                    )
                    ReminderModeChip(
                        label = NotificationSettingsCopy.modeGentle(language),
                        selected = uiState.reminderMode == NotificationReminderMode.GENTLE,
                        onClick = { viewModel.onReminderModeChanged(NotificationReminderMode.GENTLE) },
                        modifier = Modifier.weight(1f),
                    )
                    ReminderModeChip(
                        label = NotificationSettingsCopy.modeStandard(language),
                        selected = uiState.reminderMode == NotificationReminderMode.STANDARD,
                        onClick = { viewModel.onReminderModeChanged(NotificationReminderMode.STANDARD) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = NotificationSettingsCopy.modeHint(language, uiState.reminderMode),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            SettingsSection(title = NotificationSettingsCopy.categoriesSection(language)) {
                uiState.categoryEnabled.forEach { (category, enabled) ->
                    NotificationToggleRow(
                        title = RewardMomentCopy.categoryLabel(category, language),
                        subtitle = null,
                        checked = enabled,
                        onCheckedChange = { viewModel.onCategoryToggle(category, it) },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    EngagementAnalyticsTracker.notifPrefOpenSystem(category.channelId)
                                    NotificationPermissionHelper.openChannelNotificationSettings(
                                        context,
                                        category.channelId,
                                    )
                                },
                            ) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription =
                                        NotificationSettingsCopy.openChannelSettingsCd(language),
                                    tint = IconSecondary,
                                )
                            }
                        },
                    )
                }
            }

            SettingsSection(title = NotificationSettingsCopy.quietHoursSection(language)) {
                NotificationValueRow(
                    title = NotificationSettingsCopy.startLabel(language),
                    value = formatHourLabel(uiState.quietHoursStart),
                    enabled = true,
                    onClick = { showQuietStartPicker = true },
                )
                NotificationValueRow(
                    title = NotificationSettingsCopy.endLabel(language),
                    value = formatHourLabel(uiState.quietHoursEnd),
                    enabled = true,
                    onClick = { showQuietEndPicker = true },
                )
                Text(
                    text = NotificationSettingsCopy.quietHoursHint(language),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }

    if (showReminderTimePicker) {
        HourMinutePickerDialog(
            title = NotificationSettingsCopy.reminderTimePickerTitle(language),
            initialHour = uiState.reminderHour,
            initialMinute = uiState.reminderMinute,
            onDismiss = { showReminderTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.onReminderTimeChanged(hour, minute)
                showReminderTimePicker = false
            },
            okLabel = NotificationSettingsCopy.okLabel(language),
            cancelLabel = NotificationSettingsCopy.cancelLabel(language),
        )
    }

    if (showQuietStartPicker) {
        HourPickerDialog(
            title = NotificationSettingsCopy.quietStartPickerTitle(language),
            initialHour = uiState.quietHoursStart,
            onDismiss = { showQuietStartPicker = false },
            onConfirm = { hour ->
                viewModel.onQuietHoursChanged(hour, uiState.quietHoursEnd)
                showQuietStartPicker = false
            },
            okLabel = NotificationSettingsCopy.okLabel(language),
            cancelLabel = NotificationSettingsCopy.cancelLabel(language),
        )
    }

    if (showQuietEndPicker) {
        HourPickerDialog(
            title = NotificationSettingsCopy.quietEndPickerTitle(language),
            initialHour = uiState.quietHoursEnd,
            onDismiss = { showQuietEndPicker = false },
            onConfirm = { hour ->
                viewModel.onQuietHoursChanged(uiState.quietHoursStart, hour)
                showQuietEndPicker = false
            },
            okLabel = NotificationSettingsCopy.okLabel(language),
            cancelLabel = NotificationSettingsCopy.cancelLabel(language),
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
    okLabel: String = "OK",
    cancelLabel: String = "Cancel",
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
                Text(okLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
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
    okLabel: String = "OK",
    cancelLabel: String = "Cancel",
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
                Text(okLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
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
