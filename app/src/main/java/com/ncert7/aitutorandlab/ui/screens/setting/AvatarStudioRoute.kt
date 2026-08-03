package com.ncert7.aitutorandlab.ui.screens.setting

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anurag.eduai.uikit.components.EduScreenTopBar
import com.anurag.eduai.uikit.screens.EduAvatarStudioScreen
import com.anurag.eduai.uikit.theme.EduAiTheme
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.notification.NotificationAvatarCache
import com.ncert7.aitutorandlab.service.analytics.ScreenName
import com.ncert7.aitutorandlab.service.analytics.TrackScreenEvent
import com.ncert7.aitutorandlab.ui.screens.garden.AvatarGardenSegment
import com.ncert7.aitutorandlab.ui.screens.garden.AvatarGardenGrowHint
import com.ncert7.aitutorandlab.ui.screens.garden.AvatarGardenSegmentBar
import com.ncert7.aitutorandlab.ui.screens.garden.AvatarTabNavigation
import com.ncert7.aitutorandlab.ui.screens.garden.GardenJourneySegment
import com.ncert7.aitutorandlab.ui.screens.garden.GardenSceneSegment
import com.ncert7.aitutorandlab.ui.screens.garden.viewmodel.GardenViewModel
import com.ncert7.aitutorandlab.ui.screens.setting.viewmodel.TutorConfigViewModel
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.ui.theme.BrandPrimary
import com.ncert7.aitutorandlab.ui.theme.TextOnPrimary
import com.ncert7.aitutorandlab.utils.AvatarStudioCopyFactory
import com.ncert7.aitutorandlab.utils.GardenCopyFactory
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import com.ncert7.aitutorandlab.utils.normalizeLanguageCode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarStudioRoute(
    onNavigateBack: () -> Unit = {},
    showBackNavigation: Boolean = true,
    initialSegment: AvatarGardenSegment = AvatarGardenSegment.Scene,
    viewModel: TutorConfigViewModel = hiltViewModel(),
    gardenViewModel: GardenViewModel = hiltViewModel(),
) {
    TrackScreenEvent(screenName = ScreenName.AVATAR)

    val context = LocalContext.current
    val activity = context as? Activity
    val gardenEnabled = GamificationFeatureFlags.isGardenEnabled(context)
    val localeLanguage = getCurrentLanguageCode()
    val languageCode = normalizeLanguageCode(localeLanguage)
    val copy = AvatarStudioCopyFactory.forLanguage(languageCode)
    val gardenCopy = GardenCopyFactory.homeCopy(languageCode)
    val gardenState by gardenViewModel.uiState.collectAsState()

    var segment by rememberSaveable {
        mutableIntStateOf(
            AvatarTabNavigation.consumePendingSegment()?.index ?: initialSegment.index,
        )
    }
    var adInFlight by rememberSaveable { mutableStateOf(false) }
    val selectedSegment = AvatarGardenSegment.fromIndex(segment)
    val stateHolder = rememberSaveableStateHolder()

    LaunchedEffect(localeLanguage) {
        gardenViewModel.syncLanguage(localeLanguage)
    }

    LaunchedEffect(gardenEnabled, selectedSegment) {
        if (gardenEnabled && selectedSegment != AvatarGardenSegment.Look) {
            gardenViewModel.refresh()
        }
    }

    EduAiTheme {
        Scaffold(
            topBar = {
                if (showBackNavigation) {
                    TopAppBar(
                        title = {
                            Text(
                                if (gardenEnabled) {
                                    gardenCopy.avatarTabTitle
                                } else {
                                    copy.screenTitle
                                },
                                fontWeight = FontWeight.SemiBold,
                                color = TextOnPrimary,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                    tint = TextOnPrimary,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandPrimary),
                    )
                } else {
                    EduScreenTopBar(
                        title =
                            if (gardenEnabled) {
                                gardenCopy.avatarTabTitle
                            } else {
                                copy.screenTitle
                            },
                    )
                }
            },
        ) { padding ->
            if (!gardenEnabled) {
                EduAvatarStudioScreen(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    copy = copy,
                    onConfigPersisted = { config, presetId ->
                        viewModel.saveConfig(config, presetId)
                        NotificationAvatarCache.refresh(context, config)
                    },
                    showRewardedAds = activity?.let { host ->
                        { sessionId, totalAds, _ ->
                            viewModel.showAvatarRewardedAds(host, sessionId, totalAds)
                        }
                    },
                )
                return@Scaffold
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                AvatarGardenSegmentBar(
                    selected = selectedSegment,
                    onSelect = { next ->
                        if (adInFlight) return@AvatarGardenSegmentBar
                        segment = next.index
                    },
                    languageCode = languageCode,
                )

                AvatarGardenGrowHint(
                    theme = gardenState.theme,
                    languageCode = languageCode,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Column(
                    Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                ) {
                    when (selectedSegment) {
                        AvatarGardenSegment.Look ->
                            stateHolder.SaveableStateProvider("avatar-look") {
                                EduAvatarStudioScreen(
                                    modifier = Modifier.fillMaxSize(),
                                    copy = copy,
                                    onConfigPersisted = { config, presetId ->
                                        viewModel.saveConfig(config, presetId)
                                        NotificationAvatarCache.refresh(context, config)
                                    },
                                    showRewardedAds = activity?.let { host ->
                                        { sessionId, totalAds, _ ->
                                            adInFlight = true
                                            val ok =
                                                viewModel.showAvatarRewardedAds(host, sessionId, totalAds)
                                            adInFlight = false
                                            ok
                                        }
                                    },
                                )
                            }

                        AvatarGardenSegment.Scene -> {
                            val scene = gardenState.scene
                            val progress = gardenState.progress
                            if (scene != null && progress != null) {
                                stateHolder.SaveableStateProvider("avatar-scene") {
                                    GardenSceneSegment(
                                        scene = scene,
                                        progress = progress,
                                        theme = gardenState.theme,
                                        plantedRows = gardenState.plantedRows,
                                        viewingZone = gardenState.viewingZone ?: progress.currentZone,
                                        languageCode = languageCode,
                                        onViewingZoneChange = gardenViewModel::selectViewingZone,
                                        onSlotSelected = gardenViewModel::setPreferredSlot,
                                        showStarterPlantHighlight = gardenState.showStarterPlantHighlight,
                                        onStarterPlantHighlightSeen = gardenViewModel::acknowledgeStarterPlantHighlight,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            } else {
                                Text(
                                    text = gardenCopy.loadingGarden,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(16.dp),
                                )
                                LaunchedEffect(Unit) { gardenViewModel.refresh() }
                            }
                        }

                        AvatarGardenSegment.Journey -> {
                            val scene = gardenState.scene
                            if (scene != null) {
                                stateHolder.SaveableStateProvider("avatar-journey") {
                                    GardenJourneySegment(
                                        scene = scene,
                                        progress = gardenState.progress,
                                        theme = gardenState.theme,
                                        themeNote = gardenState.themeNote,
                                        onChooseTheme = gardenViewModel::chooseTheme,
                                        onDismissNote = gardenViewModel::clearThemeNote,
                                        onPlaceSelected = { zone ->
                                            gardenViewModel.selectViewingZone(zone)
                                            segment = AvatarGardenSegment.Scene.index
                                        },
                                        languageCode = languageCode,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            } else {
                                LaunchedEffect(Unit) { gardenViewModel.refresh() }
                            }
                        }
                    }
                }
            }
        }
    }
}
