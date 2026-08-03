package com.ncert7.aitutorandlab.domain.chatbot.usecase

import com.ncert7.aitutorandlab.config.ConceptMapFeatureAvailability
import com.ncert7.aitutorandlab.data.remote.SessionMetadata
import com.ncert7.aitutorandlab.debug.DebugLogger
import com.ncert7.aitutorandlab.domain.chatbot.model.ResourceDecision
import javax.inject.Inject

class ResourceDecisionUseCase @Inject constructor() {

    fun decide(metadata: SessionMetadata): ResourceDecision {
        try {
            val transition = metadata.nodeTransitions.last()
            val from = transition["from_node"] as? String
            val to = transition["to_node"] as? String

            DebugLogger.debugLog("ResourceDecisionUseCase", "Transition: $from → $to")

            return when {
                from == "APK" && to == "CI" && !metadata.imageUrl.isNullOrBlank() -> {
                    val processedUrl = processImageUrl(metadata.imageUrl)
                    DebugLogger.debugLog("ResourceDecisionUseCase", "Image URL: $processedUrl")
                    ResourceDecision.ShowImage(
                        url = processedUrl,
                        description = metadata.imageDescription
                    )
                }

                from == "CI" && to == "SIM_CC" && ConceptMapFeatureAvailability.isEnabled() -> {
                    ResourceDecision.ShowConceptMap(triggerText = "")
                }

                else -> ResourceDecision.None
            }
        } catch (e: Exception) {
            DebugLogger.errorLog("ResourceDecisionUseCase", "Error: ${e.message}")
            return ResourceDecision.None
        }
    }

    fun processImageUrl(url: String): String {
        return when {
            url.contains("github.com") && url.contains("/blob/") -> {
                url.replace("github.com", "raw.githubusercontent.com")
                    .replace("/blob/", "/")
            }
            else -> url
        }
    }
}

