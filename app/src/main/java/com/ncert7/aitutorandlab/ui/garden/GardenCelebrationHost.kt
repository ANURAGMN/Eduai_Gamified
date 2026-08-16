package com.ncert7.aitutorandlab.ui.garden

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.ncert7.aitutorandlab.domain.garden.GardenMomentCoordinator
import com.ncert7.aitutorandlab.domain.moment.MomentUiModel
import com.ncert7.aitutorandlab.ui.screens.plan.components.TrialMomentHost
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GardenCelebrationViewModel @Inject constructor(
    private val coordinator: GardenMomentCoordinator,
) : ViewModel() {
    val pending = coordinator.pending
    val suppressed = coordinator.suppressGlobalHost

    fun build(languageCode: String): MomentUiModel? = coordinator.buildMoment(languageCode)

    fun dismiss() = coordinator.clear()
}

/**
 * App-wide "you grew a plant / built a space module" celebration. Mounted once in [MainActivity]
 * over the whole app so a plant earned by *any* completed task (Sim, chat, revision, math, science)
 * — not just Plan-trial tasks — shows its full-screen moment. The Plan-trial screen runs its own
 * celebration chain and suppresses this host while it is on screen (see
 * [GardenMomentCoordinator.setGlobalHostSuppressed]) so the moment is never shown twice.
 */
@Composable
fun GardenCelebrationHost(
    viewModel: GardenCelebrationViewModel = hiltViewModel(),
) {
    val pending by viewModel.pending.collectAsState()
    val suppressed by viewModel.suppressed.collectAsState()
    val language = getCurrentLanguageCode()
    var moment by remember { mutableStateOf<MomentUiModel?>(null) }

    LaunchedEffect(pending, suppressed, language) {
        // While the Plan-trial screen is visible it sets suppressGlobalHost=true and runs its own
        // celebration chain. Do NOT also key off activeTrialItemId — that id often stays set after
        // leaving Plan, which blocked the app-wide host (showingMoment=false) and dropped the popup.
        moment = if (pending != null && !suppressed) viewModel.build(language) else null
        if (pending != null) {
            android.util.Log.i(
                "GardenPlant",
                "HOST pending=true suppressed=$suppressed showingMoment=${moment != null}",
            )
        }
    }

    TrialMomentHost(
        moment = moment,
        onPrimary = {
            moment = null
            viewModel.dismiss()
        },
        onSecondary = {
            moment = null
            viewModel.dismiss()
        },
    )
}
