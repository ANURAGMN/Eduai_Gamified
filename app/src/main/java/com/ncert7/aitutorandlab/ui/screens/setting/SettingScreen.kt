package com.ncert7.aitutorandlab.ui.screens.setting

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.anurag.eduai.uikit.components.EduScreenTopBar
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.config.ReelsFeatureFlags
import androidx.compose.material3.Switch
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.notification.NotificationType
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.setting.components.CenterPopupCard
import com.ncert7.aitutorandlab.ui.screens.setting.components.ContactSupportCard
import com.ncert7.aitutorandlab.ui.screens.setting.components.EditProfileScreen
import com.ncert7.aitutorandlab.ui.screens.setting.components.ProfileCard
import com.ncert7.aitutorandlab.ui.screens.setting.viewmodel.LogoutState
import com.ncert7.aitutorandlab.ui.theme.AccentBlue
import com.ncert7.aitutorandlab.ui.theme.BackgroundSecondary
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.CardBackground
import com.ncert7.aitutorandlab.ui.theme.ColorError
import com.ncert7.aitutorandlab.ui.theme.ColorWarning
import com.ncert7.aitutorandlab.ui.theme.IconSecondary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextOnPrimary
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import com.ncert7.aitutorandlab.ui.screens.friends.viewmodel.FriendsViewModel
import com.ncert7.aitutorandlab.ui.screens.setting.components.TutorAvatarSettingsSection
import com.ncert7.aitutorandlab.ui.screens.setting.viewmodel.SettingViewModel
import android.provider.Settings

sealed class PopupScreen {
    object EditProfile : PopupScreen()
    object ContactUs : PopupScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onNavigateBack: () -> Unit = {},
    showBackNavigation: Boolean = true,
    onLogout: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    onNavigateToAvatarStudio: () -> Unit = {},
    onNavigateToProgress: () -> Unit = {},
    onNavigateToQuests: () -> Unit = {},
) {
    TrackScreenEvent(screenName = ScreenName.SETTINGS)

    val dimens = LocalDimensions.current

    var activeScreen by remember { mutableStateOf<PopupScreen?>(null) }
    var showNotificationSettings by remember { mutableStateOf(false) }

    val viewModel: SettingViewModel = hiltViewModel()

    val student by viewModel.student.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val logoutState by viewModel.logoutState.collectAsState()

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val sharedPrefs = remember(context) { SharedPreferenceUtils(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var gamifiedHomeEnabled by remember {
        mutableStateOf(GamificationFeatureFlags.isGamifiedHomeEnabled(context))
    }
    var nativeTutorAvatarEnabled by remember {
        mutableStateOf(GamificationFeatureFlags.isNativeTutorAvatarEnabled(context))
    }
    var forceOnboardingAfterSignIn by remember {
        mutableStateOf(sharedPrefs.isForceOnboardingDebugEnabled())
    }
    val friendsViewModel: FriendsViewModel = hiltViewModel()
    val friendCount by friendsViewModel.friendCount.collectAsState()

    fun dismissOrNavigateBack() {
        if (showNotificationSettings) {
            showNotificationSettings = false
        } else if (activeScreen != null) {
            activeScreen = null
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = showNotificationSettings || activeScreen != null) {
        dismissOrNavigateBack()
    }

    val useGamifiedTabChrome = !showBackNavigation && gamifiedHomeEnabled

    Scaffold(
        topBar = {
            if (useGamifiedTabChrome) {
                EduAiTheme {
                    EduScreenTopBar(title = "Profile")
                }
            } else {
                TopAppBar(
                    title = {
                        Text(
                            if (showBackNavigation) stringResource(R.string.settings) else "Profile",
                            fontWeight = FontWeight.SemiBold,
                            color = TextOnPrimary
                        )
                    },
                    navigationIcon = {
                        if (showBackNavigation) {
                            IconButton(onClick = { dismissOrNavigateBack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    modifier = Modifier.size(dimens.iconMedium),
                                    tint = TextOnPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPrimary)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(BackgroundSecondary)
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(dimens.screenPadding),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
            ) {
                // Learning Language Section
                SettingsSection(title = stringResource(R.string.language)) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = dimens.spaceSmall),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                    ) {
                        LanguageButton(
                            text = stringResource(R.string.language_english),
                            isSelected = selectedLanguage == "en",
                            onClick = { viewModel.setLanguage("en") },
                            modifier = Modifier.weight(1f)
                        )
                        LanguageButton(
                            text = stringResource(R.string.language_kannada),
                            isSelected = selectedLanguage == "kn",
                            onClick = { viewModel.setLanguage("kn") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.profile),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                )

                if (student == null) {
                    Text(
                        text = stringResource(R.string.loading_profile),
                        modifier = Modifier.padding(dimens.spaceMedium),
                        color = TextSecondary
                    )
                } else {
                    ProfileCard(
                        profileImageUri = student!!.profilePhotoUrl,
                        name = student!!.studentName,
                        email = student!!.email,
                        phone = student!!.phoneNumber,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Account Section
                SettingsSection(title = stringResource(R.string.account)) {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        iconTint = AccentBlue,
                        title = stringResource(R.string.edit_profile),
                        onClick = { activeScreen = PopupScreen.EditProfile }
                    )
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        iconTint = ColorWarning,
                        title = stringResource(R.string.notifications),
                        onClick = {
                            if (gamifiedHomeEnabled) {
                                showNotificationSettings = true
                            } else {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                // Support Section
                SettingsSection(title = stringResource(R.string.support)) {
                    SettingsItem(
                        icon = Icons.Default.Email,
                        iconTint = AccentBlue,
                        title = stringResource(R.string.contact_us),
                        onClick = { activeScreen = PopupScreen.ContactUs }
                    )
                }

                SettingsSection(title = stringResource(R.string.legal_section_title)) {
                    SettingsItem(
                        icon = Icons.Default.Policy,
                        iconTint = AccentBlue,
                        title = stringResource(R.string.privacy_policy_label),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    android.net.Uri.parse(context.getString(R.string.privacy_policy_url)),
                                ),
                            )
                        },
                    )
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Article,
                        iconTint = AccentBlue,
                        title = stringResource(R.string.terms_of_service_label),
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    android.net.Uri.parse(context.getString(R.string.terms_of_service_url)),
                                ),
                            )
                        },
                    )
                }

                if (gamifiedHomeEnabled) {
                    SettingsSection(title = "Learning") {
                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            iconTint = BrandPrimary,
                            title = "Progress & skills",
                            onClick = onNavigateToProgress,
                        )
                        // Quests move here when the Reels tab takes their bottom-bar slot.
                        if (ReelsFeatureFlags.isReelsEnabled()) {
                            SettingsItem(
                                icon = Icons.Outlined.TrackChanges,
                                iconTint = BrandPrimary,
                                title = "Today's quest",
                                onClick = onNavigateToQuests,
                            )
                        }
                    }
                    SettingsSection(title = "Social") {
                        SettingsItem(
                            icon = Icons.Default.Group,
                            iconTint = BrandPrimary,
                            title = "Friends · $friendCount",
                            onClick = onNavigateToFriends,
                        )
                    }
                }

                if (BuildConfig.DEBUG) {
                    SettingsSection(title = "Developer") {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = dimens.spaceSmall),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gamified Home (ui-kit)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Preview Sprint 1 Home. Re-open Home tab to apply.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            Switch(
                                checked = gamifiedHomeEnabled,
                                onCheckedChange = { enabled ->
                                    sharedPrefs.setGamifiedHomeDebugEnabled(enabled)
                                    gamifiedHomeEnabled = enabled
                                },
                            )
                        }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = dimens.spaceSmall),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Native tutor avatar (all agents)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Sprint 8: Compose tutor + TTS lip sync. Also on when NATIVE_TUTOR_AVATAR_ENABLED or gamified Home is on.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            Switch(
                                checked = nativeTutorAvatarEnabled,
                                onCheckedChange = { enabled ->
                                    sharedPrefs.setNativeTutorAvatarDebugEnabled(enabled)
                                    nativeTutorAvatarEnabled = enabled
                                },
                            )
                        }
                        if (nativeTutorAvatarEnabled) {
                            TutorAvatarSettingsSection(
                                onOpenStudio = onNavigateToAvatarStudio,
                                modifier = Modifier.padding(top = dimens.spaceSmall),
                            )
                        }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = dimens.spaceSmall),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Force onboarding after sign-in",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = "Sign out, then sign in again to replay onboarding. Uses the language selected above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            Switch(
                                checked = forceOnboardingAfterSignIn,
                                onCheckedChange = { enabled ->
                                    sharedPrefs.setForceOnboardingDebugEnabled(enabled)
                                    forceOnboardingAfterSignIn = enabled
                                },
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.debugPrepareQuestAdTest {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Quest ready: sims 3/3 + study 1/1. Open Home and tap a quest node.",
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Prepare quest ad test")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.debugSimulateFriendRequests { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Simulate 2 friend requests")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.debugSimulateBotFriendUpdates { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Simulate bot friend updates")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.debugPurgeSelfFriendFeed { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Purge self from Friends' updates")
                        }
                        if (gamifiedHomeEnabled) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.DAILY_REMINDER) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire daily reminder (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.STREAK_AT_RISK) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire streak at risk (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.STREAK_SAVED) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire streak saved (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.TASKS_PENDING) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire tasks pending (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.EXAM_COUNTDOWN) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire exam countdown (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.INACTIVITY_3) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire inactivity 3d (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.INACTIVITY_7) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire inactivity 7d (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.INACTIVITY_14) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire inactivity 14d (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.CHAPTER_PROGRESS) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire chapter progress (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.STREAK_COMEBACK) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire streak comeback (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.WEEKLY_XP_CLOSE) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire weekly XP close (debug)")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.debugFireTestNotification(NotificationType.AVATAR_UNLOCK_EXPIRING) { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Fire avatar expiring (debug)")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Logout Button
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimens.buttonHeightLarge),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = ColorError.copy(alpha = 0.1f)
                        ),
                    shape = RoundedCornerShape(dimens.cornerRadiusMedium)
                ) {
                    Text(
                        text = stringResource(R.string.logout),
                        color = ColorError,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (showNotificationSettings) {
            NotificationSettingsScreen(onNavigateBack = { showNotificationSettings = false })
        }

        CenterPopupCard(visible = activeScreen != null, onDismiss = { activeScreen = null }) {
            when (activeScreen) {
                PopupScreen.EditProfile ->
                    EditProfileScreen(
                        userId = viewModel.userId,
                        student = student,
                        userViewModel = viewModel
                    ) { activeScreen = null }
                PopupScreen.ContactUs ->
                    ContactSupportCard(
                        emailAddress = stringResource(R.string.contact_email),
                        whatsappNumber = stringResource(R.string.contact_number),
                        websiteUrl = stringResource(R.string.contact_website),
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.send_us_mail_msg),
                        subtitle = stringResource(R.string.we_would_love_msg),
                        emailButtonText = stringResource(R.string.open_email_app_msg)
                    ) { activeScreen = null }
                null -> {}
            }
        }
        // Handle logout success
        LaunchedEffect(logoutState) {
            when (logoutState) {
                is LogoutState.Success -> {
                    onLogout()
                }
                is LogoutState.Error -> {
                    // Log error or show toast if needed
                    val error = (logoutState as? LogoutState.Error)?.message
                    DebugLogger.errorLog("SettingScreen", "Logout failed: $error")
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val dimens = LocalDimensions.current

    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = dimens.spaceSmall)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimens.cornerRadiusMedium),
            colors = CardDefaults.cardColors(containerColor = CardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = dimens.cardElevation)
        ) {
            Column(
                modifier = Modifier.padding(dimens.cardPadding),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)
            ) { content() }
        }
    }
}

@Composable
fun LanguageButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = LocalDimensions.current

    Button(
        onClick = onClick,
        modifier = modifier.height(dimens.buttonHeight),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = if (isSelected) BrandPrimary else CardBackground,
                contentColor = if (isSelected) TextOnPrimary else TextPrimary
            ),
        shape = RoundedCornerShape(dimens.cornerRadiusMedium),
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation =
                    if (isSelected) dimens.cardElevation
                    else dimens.cardElevation / 2
            )
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingsItem(icon: ImageVector, iconTint: Color, title: String, onClick: () -> Unit) {
    val dimens = LocalDimensions.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = dimens.spaceSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(dimens.iconMedium)
            )
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = IconSecondary,
            modifier = Modifier.size(dimens.iconMedium)
        )
    }
}