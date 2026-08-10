package com.ncert7.aitutorandlab.ui.screens.conceptscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ncert7.aitutorandlab.R
import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus
import com.ncert7.aitutorandlab.ui.models.ConceptUiModel
import com.ncert7.aitutorandlab.ui.theme.AccentBlue
import com.ncert7.aitutorandlab.ui.theme.CardBackground
import com.ncert7.aitutorandlab.ui.theme.CompleteTextColor
import com.ncert7.aitutorandlab.ui.theme.InProgressTextColor
import com.ncert7.aitutorandlab.ui.theme.LocalDimensions
import com.ncert7.aitutorandlab.ui.theme.NotStartedTextColor
import com.ncert7.aitutorandlab.ui.theme.TextPrimary
import com.ncert7.aitutorandlab.ui.theme.TextSecondary
import com.ncert7.aitutorandlab.ui.theme.White

/**
 * Composable function to display a Concept Card with status badge, title, concept completion status, and action buttons.
 *
 * The buttons displayed depend on the concept type:
 * - STUDY: Single clickable card (navigates to chat screen)
 * - MATH PROBLEM: Shows "Start Problem" button (navigates to math agent with problemId)
 * - SIMULATION: Shows "Agent" button (if simulationId exists) and "Simulation" button (if simulationUrl exists)
 *
 * @param concept The Concept data to display.
 * @param serialNumber The serial number (1, 2, 3, ...) to display in the badge.
 * @param isTrial When true (the merged Trial list), a type icon marks each row as study,
 *   simulation, or study+simulation so the mixed list is scannable.
 * @param onClick Lambda function to handle main card click (STUDY type).
 * @param onSimulationAgentClick Lambda to handle simulation agent button click.
 * @param onSimulationClick Lambda to handle simulation URL button click.
 */
@Composable
fun ConceptCard(
    concept: ConceptUiModel,
    serialNumber: Int = 1,
    isTrial: Boolean = false,
    onClick: (conceptId: String, problemId: String, conceptType: String) -> Unit = { _, _, _ -> },
    onSimulationAgentClick: (String, String) -> Unit = { _, _ -> },
    onSimulationClick: (title: String, url: String, conceptId: String) -> Unit = { _, _, _ -> },
) {
    val dimens = LocalDimensions.current

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = CardDefaults.shape,
        colors = CardDefaults.cardColors(
            containerColor = CardBackground,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = dimens.cardElevation,
        )
    ){
        // Left side: Badge + Content
        Row(
            modifier = Modifier
                .padding(dimens.spaceMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
        ) {
            // Status badge (Circle with icon/order)
            ConceptStatusBadge(
                conceptOrder = serialNumber.toString(),
                status = concept.status
            )

            // Trial list is a merged view of lessons and simulations — mark each row's type.
            if (isTrial) {
                ConceptTypeIcon(concept = concept)
            }

            // Content (Title + Status)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = dimens.inputHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
            ) {
                Text(
                    text = concept.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Text(
                    text = getStatus(concept.status),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = getStatusColor(concept.status)
                )

                when {
                    concept.type.equals("STUDY", ignoreCase = true) -> {
                        StudyConceptButtons(
                            conceptId = concept.id,
                            onClick = onClick
                        )
                        // Trial view: if this lesson also has a simulation, offer it right
                        // here so concepts and their simulations sit together.
                        val hasSimAgent = concept.simulationId
                            ?.let { it.isNotBlank() && !it.equals("null", ignoreCase = true) } == true
                        val hasSimUrl = concept.simulationUrl
                            ?.let { it.isNotBlank() && !it.equals("null", ignoreCase = true) } == true
                        if (hasSimAgent || hasSimUrl) {
                            SimulationConceptButtons(
                                concept = concept,
                                onSimulationAgentClick = onSimulationAgentClick,
                                onSimulationClick = onSimulationClick
                            )
                        }
                    }
                    concept.type.equals("MATH PROBLEM", ignoreCase = true) -> {
                        MathProblemButtons(
                            conceptId = concept.id,
                            problemId = concept.problemId,
                            onClick = onClick
                        )
                    }
                    concept.type.equals("SIMULATION", ignoreCase = true) -> {
                        SimulationConceptButtons(
                            concept = concept,
                            onSimulationAgentClick = onSimulationAgentClick,
                            onSimulationClick = onSimulationClick
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.open_concept),
                tint = TextSecondary,
                modifier = Modifier.size(dimens.iconLarge)
            )
        }
    }
}

/**
 * Type marker for the merged Trial list: a book for a lesson, a flask for a simulation, and both
 * together when a lesson also ships a simulation. Purely a visual hint — the buttons are unchanged.
 */
@Composable
private fun ConceptTypeIcon(concept: ConceptUiModel) {
    val dimens = LocalDimensions.current
    val hasSim = concept.simulationId?.let { it.isNotBlank() && it != "null" } == true ||
        concept.simulationUrl?.let { it.isNotBlank() && it != "null" } == true
    val isSimType = concept.type.equals("SIMULATION", ignoreCase = true)
    val isStudyType = concept.type.equals("STUDY", ignoreCase = true)

    when {
        // Study + Simulation → both icons.
        isStudyType && hasSim -> {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(dimens.iconMedium)
                )
                Icon(
                    imageVector = Icons.Filled.Science,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(dimens.iconMedium)
                )
            }
        }
        // Simulation only.
        isSimType -> Icon(
            imageVector = Icons.Filled.Science,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(dimens.iconMedium)
        )
        // Study only.
        isStudyType -> Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(dimens.iconMedium)
        )
    }
}


/**
 * Buttons for STUDY type concepts
 * Single button that navigates to chat/tutoring screen
 */
@Composable
private fun StudyConceptButtons(
    conceptId: String,
    onClick: (conceptId: String, problemId: String, conceptType: String) -> Unit
) {
    val dimens = LocalDimensions.current

    Button(
        onClick = { onClick(conceptId, "", "STUDY") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.spaceSmall),
        contentPadding = PaddingValues(horizontal = dimens.spaceExtraSmall),
        shape = MaterialTheme.shapes.small,
        colors = buttonColors(
            containerColor = AccentBlue,
            contentColor = White
        )
    ) {
        Text(
            text = stringResource(R.string.start_learning),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = White
        )
    }
}

/**
 * Buttons for MATH PROBLEM type concepts
 * Single button labeled "Start Problem" that opens math agent with problemId
 */
@Composable
private fun MathProblemButtons(
    conceptId: String,
    problemId: String,
    onClick: (conceptId: String, problemId: String, conceptType: String) -> Unit
) {
    val dimens = LocalDimensions.current

    Button(
        onClick = { onClick(conceptId, problemId, "MATH PROBLEM") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.spaceSmall),
        contentPadding = PaddingValues(horizontal = dimens.spaceExtraSmall),
        shape = MaterialTheme.shapes.small,
        colors = buttonColors(
            containerColor = AccentBlue,
            contentColor = White
        )
    ) {
        Text(
            text = stringResource(R.string.problem_to_solve),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = White
        )
    }
}

/**
 * Buttons for SIMULATION type concepts
 * Shows "Agent" button (if simulationId exists) and "Simulation" button (if simulationUrl exists)
 * Filters null/empty values before passing to callbacks to prevent crashes
 */
@Composable
private fun SimulationConceptButtons(
    concept: ConceptUiModel,
    onSimulationAgentClick: (String, String) -> Unit,
    onSimulationClick: (title: String, url: String, conceptId: String) -> Unit
) {
    val dimens = LocalDimensions.current

    // Filter null/empty/invalid values - NEVER pass null or "null" string
    val validSimulationId = concept.simulationId?.takeIf { it.isNotBlank() && it != "null" }
    val validSimulationUrl = concept.simulationUrl?.takeIf { it.isNotBlank() && it != "null" }

    val hasAgent = validSimulationId != null
    val hasUrl = validSimulationUrl != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimens.spaceSmall),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        // Row for Agent and Simulation buttons if both exist
        if (hasAgent && hasUrl) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
            ) {
                // Agent button - SAFE: only called if validSimulationId is not null
                Button(
                    onClick = {
                        validSimulationId?.let {
                            onSimulationAgentClick(it, concept.id)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = dimens.spaceExtraSmall),
                    shape = MaterialTheme.shapes.small,
                    colors = buttonColors(
                        containerColor = AccentBlue,
                        contentColor = White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.agent),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = White
                    )
                }

                // Simulation button - SAFE: only called if validSimulationUrl is not null
                OutlinedButton(
                    onClick = {
                        validSimulationUrl?.let {
                            onSimulationClick(concept.name, it, concept.id)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = dimens.spaceExtraSmall),
                    shape = MaterialTheme.shapes.small,
                    colors = outlinedButtonColors(
                        containerColor = White,
                        contentColor = TextPrimary,
                    )
                ) {
                    Text(
                        text = stringResource(R.string.simulation),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        } else if (hasAgent) {
            // Only Agent button
            Button(
                onClick = {
                    validSimulationId?.let {
                        onSimulationAgentClick(it, concept.id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = dimens.spaceExtraSmall),
                shape = MaterialTheme.shapes.small,
                colors = buttonColors(
                    containerColor = AccentBlue,
                    contentColor = White
                )
            ) {
                Text(
                    text = stringResource(R.string.agent),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = White
                )
            }
        } else if (hasUrl) {
            // Only Simulation button
            OutlinedButton(
                onClick = {
                    validSimulationUrl?.let {
                        onSimulationClick(concept.name, it, concept.id)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = dimens.spaceExtraSmall),
                shape = MaterialTheme.shapes.small,
                colors = outlinedButtonColors(
                    containerColor = White,
                    contentColor = TextPrimary,
                )
            ) {
                Text(
                    text = stringResource(R.string.simulation),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

// Helper Functions for Status Texts and Colors
@Composable
private fun getStatus(status: ProgressStatus): String = when (status) {
    ProgressStatus.COMPLETED -> stringResource(R.string.completed)
    ProgressStatus.IN_PROGRESS -> stringResource(R.string.in_progress_continue_learning)
    ProgressStatus.NOT_STARTED -> stringResource(R.string.complete_previous_concepts)
    ProgressStatus.LOCKED -> stringResource(R.string.locked)
}

private fun getStatusColor(status: ProgressStatus): Color = when (status) {
    ProgressStatus.COMPLETED -> CompleteTextColor
    ProgressStatus.IN_PROGRESS -> InProgressTextColor
    ProgressStatus.NOT_STARTED -> NotStartedTextColor
    ProgressStatus.LOCKED -> NotStartedTextColor
}
