package com.ncert7.aitutorandlab.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.domain.onboarding.OnboardingGate
import com.ncert7.aitutorandlab.ui.screens.login.LoginScreen
import com.ncert7.aitutorandlab.ui.screens.login.UserDetailEntryScreen
import com.ncert7.aitutorandlab.ui.screens.login.viewmodel.UserViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.anurag.eduai.uikit.avatar.AllAvatarPresets
import com.anurag.eduai.uikit.avatar.AvatarUnlockStore
import com.anurag.eduai.uikit.avatar.TutorConfigStore
import com.anurag.eduai.uikit.screens.EduOnboardingScreen
import com.ncert7.aitutorandlab.di.TutorConfigEntryPoint
import com.ncert7.aitutorandlab.ui.screens.onboarding.OnboardingViewModel
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.service.analytics.EngagementAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.FunnelAnalyticsTracker
import com.ncert7.aitutorandlab.service.analytics.FunnelStep
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.repository.FirebaseRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LoginNavigator() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val sharedPreferenceUtils = remember { SharedPreferenceUtils(context) }
    val logoutTriggered = remember { mutableStateOf(false) }
    var sessionChecked by remember { mutableStateOf(false) }
    var startDestination by remember { mutableStateOf("login") }

    val userViewModel: UserViewModel = hiltViewModel()

    LaunchedEffect(logoutTriggered.value) {
        sessionChecked = false
        startDestination = if (logoutTriggered.value || !userViewModel.hasValidLocalSession()) {
            "login"
        } else {
            "main"
        }
        sessionChecked = true
    }

    if (!sessionChecked) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }
        composable("userDetailEntry") {
            UserDetailEntryScreen(
                navController = navController,
                userViewModel = userViewModel
            )
        }
        composable("main") {
            // First-run gate at the destination itself, so every path into "main" (fresh login and
            // restored session alike) shows the intro + subject/chapter/world picks exactly once.
            var onboarded by remember {
                mutableStateOf(!OnboardingGate.shouldShowOnboarding(sharedPreferenceUtils))
            }
            if (!onboarded) {
                TrackScreenEvent(screenName = ScreenName.ONBOARDING)
                val onboardingViewModel: OnboardingViewModel = hiltViewModel()
                val chaptersBySubject by onboardingViewModel.chaptersBySubject.collectAsState()
                // Use the applied app locale (what the student picked at sign-in), not the prefs key
                // which may still be the default "en" at first run.
                val languageCode = getCurrentLanguageCode()
                LaunchedEffect(languageCode) {
                    onboardingViewModel.refreshChapters(languageCode)
                }
                LaunchedEffect(Unit) {
                    FunnelAnalyticsTracker.track(FunnelStep.ONBOARDING_START)
                }
                EduOnboardingScreen(
                    languageCode = languageCode,
                    chaptersBySubject = chaptersBySubject,
                    onSlideView = { EngagementAnalyticsTracker.onboardingSlideView(it) },
                    onSlideSkip = { EngagementAnalyticsTracker.onboardingSkip(it) },
                    onSubjectSelected = { subject ->
                        FunnelAnalyticsTracker.track(FunnelStep.ONBOARDING_SUBJECT_SELECTED)
                        EngagementAnalyticsTracker.onboardingSubjectSelected(subject)
                    },
                    onChapterSelected = { chapter ->
                        FunnelAnalyticsTracker.track(FunnelStep.ONBOARDING_CHAPTER_SELECTED)
                        EngagementAnalyticsTracker.onboardingChapterSelected(chapter)
                    },
                    onWorldSelected = { world ->
                        FunnelAnalyticsTracker.track(FunnelStep.ONBOARDING_WORLD_SELECTED)
                        EngagementAnalyticsTracker.onboardingWorldSelected(world)
                    },
                    onAvatarSelected = { presetId ->
                        EngagementAnalyticsTracker.onboardingAvatarSelected(presetId)
                    },
                    onFinish = { result ->
                        FunnelAnalyticsTracker.track(FunnelStep.ONBOARDING_COMPLETE)
                        EngagementAnalyticsTracker.onboardingPicks(
                            subject = result.subject,
                            chapter = result.chapter,
                            world = result.world,
                        )
                        sharedPreferenceUtils.setFirstRunResult(
                            subject = result.subject,
                            chapter = result.chapter,
                            world = result.world,
                        )
                        OnboardingGate.markOnboardingCompleted(sharedPreferenceUtils)
                        // Persist the chosen tutor look (free onboarding pick) so it renders everywhere
                        // (Home mascot, top bar, moments) and shows as unlocked in Avatar studio.
                        // Must go through TutorConfigRepository — store-only saves get overwritten by
                        // ensureLoaded()'s default Scholar seed from Room.
                        result.avatarPresetId.takeIf { it.isNotBlank() }?.let { presetId ->
                            AvatarUnlockStore.unlock(context, presetId)
                            sharedPreferenceUtils.setOnboardingAvatar(presetId)
                            val userId = sharedPreferenceUtils.getUserId().orEmpty()
                            val appCtx = context.applicationContext
                            AllAvatarPresets.firstOrNull { it.id == presetId }?.let { preset ->
                                TutorConfigStore.save(appCtx, preset.config)
                            }
                            CoroutineScope(Dispatchers.IO).launch {
                                runCatching {
                                    EntryPointAccessors
                                        .fromApplication(appCtx, TutorConfigEntryPoint::class.java)
                                        .tutorConfigRepository()
                                        .applyPreset(appCtx, userId, presetId)
                                }
                            }
                        }
                        val userId = sharedPreferenceUtils.getUserId().orEmpty()
                        if (userId.isNotBlank()) {
                            // Best-effort cloud mirror — local prefs already gate the UI.
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    FirebaseRepository().updateOnboardingPicks(
                                        userId = userId,
                                        subject = result.subject,
                                        chapter = result.chapter,
                                        world = result.world,
                                        picksApplied = false,
                                    )
                                } catch (_: Exception) {
                                }
                            }
                        }
                        onboarded = true
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                )
            } else {
                BottomNavBar(
                    onLogout = {
                        logoutTriggered.value = true
                        userViewModel.resetUserState()

                        navController.navigate("login") {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}