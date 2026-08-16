package com.ncert7.aitutorandlab.domain.garden

import android.content.Context
import com.anurag.eduai.uikit.garden.quest.Theme
import com.anurag.eduai.uikit.garden.quest.ZONES
import com.ncert7.aitutorandlab.config.GamificationFeatureFlags
import com.ncert7.aitutorandlab.utils.GardenWorldLabels
import com.ncert7.aitutorandlab.data.local.entities.GrownItemEntity
import com.ncert7.aitutorandlab.domain.moment.MomentTokens
import com.ncert7.aitutorandlab.domain.moment.MomentType
import com.ncert7.aitutorandlab.domain.moment.MomentUiModel
import com.ncert7.aitutorandlab.domain.moment.MomentVariantPicker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class GardenMomentCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val momentVariantPicker: MomentVariantPicker,
) {
    private val _pending = MutableStateFlow<GardenCelebration?>(null)
    val pending: StateFlow<GardenCelebration?> = _pending.asStateFlow()

    // The global celebration host (MainActivity) shows the plant/place moment for tasks completed
    // anywhere in the app. The Plan-trial screen runs its own richer celebration chain, so while it
    // is on screen it suppresses the global host to avoid a double pop-up over the same pending moment.
    private val _suppressGlobalHost = MutableStateFlow(false)
    val suppressGlobalHost: StateFlow<Boolean> = _suppressGlobalHost.asStateFlow()

    fun setGlobalHostSuppressed(suppressed: Boolean) {
        _suppressGlobalHost.value = suppressed
    }

    fun notifyPlanted(
        planted: GrownItemEntity,
        progress: GardenProgress,
        placeCompleted: Boolean,
    ) {
        if (!GamificationFeatureFlags.isGardenEnabled(context)) return
        android.util.Log.i(
            "GardenPlant",
            "notifyPlanted concept=${planted.conceptId} kind=${planted.kind} zone=${planted.zone} placeCompleted=$placeCompleted total=${progress.totalPlanted}",
        )
        val theme =
            when (progress.theme.uppercase()) {
                "OUTPOST" -> Theme.OUTPOST
                "ISLAND" -> Theme.ISLAND
                "COLONY" -> Theme.COLONY
                else -> Theme.GARDEN
            }
        val zoneIndex = planted.zone.coerceIn(0, ZONES.lastIndex)
        val zoneModel = ZONES[zoneIndex]
        val artSlot = GardenSlotResolver.celebrationSlot(progress, planted)
        val itemLabel = zoneModel.slotName(theme, artSlot).lowercase()
        val remainingInPlace =
            (progress.zoneCapacity - progress.filledInZone).coerceAtLeast(0)
        _pending.value =
            GardenCelebration(
                theme = theme,
                zone = zoneIndex,
                slot = artSlot,
                placeCompleted = placeCompleted,
                itemLabel = itemLabel,
                placeLabel = zoneModel.name(theme).lowercase(),
                totalPlanted = progress.totalPlanted,
                remainingInPlace = remainingInPlace,
                remainingScenes = GardenSlotResolver.remainingScenes(progress.currentZone),
            )
    }

    fun buildMoment(languageCode: String): MomentUiModel? {
        val celebration = _pending.value ?: return null
        val zoneModel = ZONES[celebration.zone.coerceIn(0, ZONES.lastIndex)]
        val itemLabel =
            GardenWorldLabels.slotName(zoneModel, celebration.theme, celebration.slot, languageCode)
                .lowercase()
        val placeLabel =
            GardenWorldLabels.zoneName(zoneModel, celebration.theme, languageCode).lowercase()
        val momentType =
            if (celebration.placeCompleted) {
                MomentType.PLACE_COMPLETED
            } else {
                MomentType.PLANT_COMPLETED
            }
        return momentVariantPicker
            .pick(
                moment = momentType,
                tokens =
                    MomentTokens(
                        item = itemLabel,
                        place = placeLabel,
                        planted = celebration.totalPlanted,
                        remainingInPlace = celebration.remainingInPlace,
                        remainingScenes = celebration.remainingScenes,
                    ),
                languageCode = languageCode,
            ).copy(
                gardenArt =
                    GardenMomentArt(
                        zone = celebration.zone,
                        slot = celebration.slot,
                        theme = celebration.theme,
                    ),
            )
    }

    fun clear() {
        _pending.value = null
    }
}
