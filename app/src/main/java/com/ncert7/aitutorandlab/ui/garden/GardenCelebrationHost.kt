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
import com.ncert7.aitutorandlab.data.local.SharedPreferenceUtils
import com.ncert7.aitutorandlab.domain.garden.GardenMomentCoordinator
import com.ncert7.aitutorandlab.domain.moment.MomentUiModel
import com.ncert7.aitutorandlab.ui.screens.plan.components.TrialMomentHost
import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GardenCelebrationViewModel @Inject constructor(
    private val coordinator: GardenMomentCoordinator,
    private val sharedPrefs: SharedPreferenceUtils,
) : ViewModel() {
    init {
        // Seed the coordinator's in-memory dedup guard from persisted state, so a plant already
        // celebrated before an app restart is not shown again.
        val studentId = sharedPrefs.getUserId().orEmpty()
        if (studentId.isNotBlank()) {
            coordinator.syncCelebratedTotal(sharedPrefs.getLastGardenCelebrationPlantTotal(studentId))
        }
    }

    val pending = coordinator.pending
    val suppressed = coordinator.suppressGlobalHost

    fun build(languageCode: String): MomentUiModel? = coordinator.buildMoment(languageCode)

    /** Mark this plant total as shown so Plan won't re-queue the same moment. */
    fun markCelebrationShown() {
        val celebration = coordinator.pending.value ?: return
        val studentId = sharedPrefs.getUserId().orEmpty()
        if (studentId.isBlank()) return
        sharedPrefs.setLastGardenCelebrationPlantTotal(studentId, celebration.totalPlanted)
    }

    fun dismiss() {
        markCelebrationShown()
        coordinator.clear()
    }
}

/**
 * App-wide "you grew a plant / built a space module" celebration. Mounted once in [MainActivity]
 * over the whole app so a plant earned by *any* completed task (Sim, chat, revision, math, science)
 * — not just Plan-trial tasks — shows its full-screen moment. The Plan-trial screen runs its own
 * celebration chain and suppresses this host while it is on screen (see
 * [GardenMomentCoordinator.setGlobalHostSuppressed]) so the moment is never shown twice.
 *
 * Plan-trial sims defer notifying this host (plant row is written; Plan queues the moment on
 * return) so learners don't see a plant popup mid-sim and again on back.
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
        val show = pending != null && !suppressed
        if (show) {
            viewModel.markCelebrationShown()
            moment = viewModel.build(language)
        } else {
            moment = null
        }
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
