package com.ncert7.aitutorandlab.ui.screens.simulation_agent.components



import androidx.compose.foundation.background

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.width

import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color.Companion.White

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.Dp

import androidx.compose.ui.unit.dp

import com.ncert7.aitutorandlab.R

import com.ncert7.aitutorandlab.ui.components.LoadingInsightPanel

import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AgentMessage

import com.ncert7.aitutorandlab.config.LocalNativeTutorAvatarEnabled
import com.ncert7.aitutorandlab.ui.screens.chatbotscreen.components.AgentTutorAvatarBubble

import com.ncert7.aitutorandlab.ui.theme.LocalDimensions

import com.ncert7.aitutorandlab.ui.viewModel.TextToSpeech

import com.ncert7.aitutorandlab.utils.getCurrentLanguageCode



/** Conversation view with agent message (25%) and single simulation (75%) */

@Composable

fun SimulationConversationView(

    avatarSize: Dp,

    currentMessage: String,

    isLoading: Boolean,

    ttsController: TextToSpeech,

    modifier: Modifier = Modifier,

    simulationUrl: String? = null,

    languageCode: String = getCurrentLanguageCode(),

    useNativeAvatar: Boolean = LocalNativeTutorAvatarEnabled.current,

    ttsState: TextToSpeech.TTSState = TextToSpeech.TTSState(),

    wordBoundaryIndex: Int = -1,

    isListening: Boolean = false,

    onParamsChanged: (Map<String, Any>) -> Unit = {},

    onPageFinished: () -> Unit = {},

    onKeyConceptReported: (String) -> Unit = {},

    onSimulationIntroReported: (String) -> Unit = onKeyConceptReported,

    onSimulationFooterReported: (String) -> Unit = {},

) {

    val dimens = LocalDimensions.current

    val compactAvatarSize = minOf(avatarSize.value, 80f).dp

    val isThinking = isLoading && !ttsState.isSpeaking



    Column(modifier = modifier.fillMaxSize()) {

        // TOP SECTION (25%): Agent avatar + message

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .weight(0.25f)

                .background(White)

                .padding(horizontal = dimens.spaceMedium, vertical = 0.dp)

        ) {

            Row(

                modifier = Modifier.fillMaxSize(),

                verticalAlignment = Alignment.Top,

            ) {

                if (useNativeAvatar) {

                    AgentTutorAvatarBubble(

                        avatarSize = compactAvatarSize,

                        ttsState = ttsState,

                        wordBoundaryIndex = wordBoundaryIndex,

                        isListening = isListening,

                        isThinking = isThinking,

                        fullBody = false,

                    )

                    Spacer(Modifier.width(dimens.spaceSmall))

                }



                Box(modifier = Modifier.weight(1f).fillMaxSize()) {

                    if (isLoading) {

                        LoadingInsightPanel(

                            statusText = stringResource(R.string.sim_teacher_thinking),

                            languageCode = languageCode,

                            centered = !useNativeAvatar,

                            rotateThinking = true,

                            modifier = Modifier

                                .fillMaxSize()

                                .padding(dimens.spaceMedium),

                        )

                    } else {

                        AgentMessage(

                            text = currentMessage,

                            isTyping = false,

                            fullText = currentMessage,

                            ttsController = ttsController,

                            modifier = Modifier.fillMaxSize(),

                            reduceTextSize = true

                        )

                    }

                }

            }

        }



        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp)



        // BOTTOM SECTION (75%): Single Simulation

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .weight(0.75f)

                .background(White)

        ) {

            if (isLoading) {

                LoadingInsightPanel(

                    statusText = stringResource(R.string.sim_loading_simulation),

                    languageCode = languageCode,

                    centered = true,

                    modifier = Modifier

                        .fillMaxSize()

                        .padding(dimens.spaceMedium),

                )

            } else if (simulationUrl == null) {

                Column(

                    modifier = Modifier

                        .fillMaxSize()

                        .padding(dimens.spaceMedium),

                    horizontalAlignment = Alignment.CenterHorizontally,

                    verticalArrangement = Arrangement.Center

                ) {

                    Text(

                        text = stringResource(R.string.sim_no_simulation),

                        style = MaterialTheme.typography.headlineSmall,

                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),

                        textAlign = TextAlign.Center

                    )

                }

            } else {

                SimulationWebView(

                    url = simulationUrl,

                    onParamsChanged = onParamsChanged,

                    onPageFinished = onPageFinished,

                    onSimulationIntroReported = onSimulationIntroReported,

                    onSimulationFooterReported = onSimulationFooterReported,

                    modifier = Modifier.fillMaxSize()

                )

            }

        }

    }

}


