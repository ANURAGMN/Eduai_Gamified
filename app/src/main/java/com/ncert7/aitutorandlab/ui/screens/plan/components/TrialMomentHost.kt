package com.ncert7.aitutorandlab.ui.screens.plan.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.anurag.eduai.uikit.components.MomentOverlay
import com.anurag.eduai.uikit.garden.GardenPlantedIllustration
import com.ncert7.aitutorandlab.domain.moment.MomentUiModel
import com.ncert7.aitutorandlab.utils.RewardMomentCopy
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode

/**
 * Renders a resolved [MomentUiModel] as a full-screen celebration or nudge.
 * Pass `null` to show nothing. [onPrimary] is the main action (keep going / finish / collect);
 * [onSecondary] backs the optional second button (later / leave anyway).
 */
@Composable
fun TrialMomentHost(
    moment: MomentUiModel?,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val language = getCurrentLanguageCode()
    val gardenArt = moment?.gardenArt
    MomentOverlay(
        visible = moment != null,
        celebratory = moment?.celebratory ?: false,
        avatar = moment?.avatarConfig ?: com.anurag.eduai.uikit.avatar.TutorConfig(),
        emotion = moment?.emotion ?: com.anurag.eduai.uikit.avatar.core.EmotionType.Happy,
        headline = moment?.headline.orEmpty(),
        body = moment?.body.orEmpty(),
        primaryCta = moment?.primaryCta ?: RewardMomentCopy.okLabel(language),
        onPrimary = onPrimary,
        secondaryCta = moment?.secondaryCta,
        onSecondary = onSecondary,
        gems = moment?.gems ?: 0,
        xp = moment?.xp ?: 0,
        xpLabel = RewardMomentCopy.xpChipLabel(language),
        gemsLabel = RewardMomentCopy.gemsChipLabel(language),
        illustration =
            if (gardenArt != null) {
                {
                    GardenPlantedIllustration(
                        zone = gardenArt.zone,
                        slot = gardenArt.slot,
                        theme = gardenArt.theme,
                    )
                }
            } else {
                null
            },
        modifier = modifier,
    )
}
