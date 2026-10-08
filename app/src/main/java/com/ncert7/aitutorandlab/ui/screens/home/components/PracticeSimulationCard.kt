package com.ncert7.aitutorandlab.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.data.local.entities.ConceptEntity
import com.ncert7.aitutorandlab.data.local.entities.ProgressEntity
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus
import com.ncert7.aitutorandlab.ui.models.ConceptUiModel
import com.ncert7.aitutorandlab.ui.screens.conceptscreen.components.ConceptCard
import com.ncert7.aitutorandlab.ui.theme.BackgroundPrimary
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.utils.getLocalizedName
import com.ncert7.aitutorandlab.utils.isKannadaLanguage
import com.ncert7.aitutorandlab.utils.SimulationLanguageUrl

@Composable
fun PracticeSimulationCard(
    progressSimulations: List<Pair<ProgressEntity?, ConceptEntity?>>,
    languageCode: String,
    onSimulationClick: (String, String) -> Unit, // Click agent button - navigates to simulation agent
    onSimulationUrlClick: (String, String, String) -> Unit = { _, _, _ -> } // Click simulation button - opens URL viewer
) {
    val dimes = LocalDimensions.current

    // Don't show card if no simulations
    if (progressSimulations.isEmpty()) return

    // Limit to top 4 simulations - NO INFINITE SCROLL
    val limitedSimulations = progressSimulations.take(4)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BackgroundPrimary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = dimes.cardElevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(dimes.screenPadding)) {
            Text(
                text = stringResource(R.string.practice_simulation),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(0.dp, dimes.spaceExtraSmall)
            )
            Spacer(modifier = Modifier.height(dimes.screenPadding))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(dimes.spaceSmall)
            ) {
                limitedSimulations.forEachIndexed { index, (progress, concept) ->
                    concept?.let { sim ->
                        val conceptUiModel = ConceptUiModel(
                            id = sim.conceptId,
                            name = sim.getLocalizedName(languageCode),
                            order = sim.orderIndex,
                            status = when (progress?.status) {
                                "COMPLETED" -> ProgressStatus.COMPLETED
                                "IN_PROGRESS" -> ProgressStatus.IN_PROGRESS
                                else -> ProgressStatus.NOT_STARTED
                            },
                            type = sim.type,
                            simulationUrl = SimulationLanguageUrl.resolve(
                                languageCode = languageCode,
                                englishUrl = sim.simulationUrl,
                                kannadaUrl = sim.simulationUrlKannada,
                                allowEnglishFallback = false,
                            ) ?: if (!isKannadaLanguage(languageCode)) sim.simulationUrl else null,
                            simulationId = if (isKannadaLanguage(languageCode)) sim.simulationIdKannada else sim.simulationId
                        )

                        ConceptCard(
                            concept = conceptUiModel,
                            serialNumber = index + 1,
                            onClick = { conceptId, problemId, conceptType ->
                                onSimulationClick(conceptUiModel.simulationId ?: "", sim.conceptId)
                            },
                            onSimulationAgentClick = { simulationId, conceptId ->
                                // Clicking "Agent" button opens simulation agent
                                onSimulationClick(simulationId, conceptId)
                            },
                            onSimulationClick = { title, url, conceptId ->
                                // Clicking "Simulation" button opens URL viewer
                                onSimulationUrlClick(title, url, conceptId)
                            }
                        )
                    }
                }
            }
        }
    }
}