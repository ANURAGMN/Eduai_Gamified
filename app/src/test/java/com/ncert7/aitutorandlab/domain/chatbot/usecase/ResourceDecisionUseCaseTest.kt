package com.ncert7.aitutorandlab.domain.chatbot.usecase

import com.ncert7.aitutorandlab.data.remote.SessionMetadata
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceDecisionUseCaseTest {

    private val useCase = ResourceDecisionUseCase()

    @Test
    fun decide_skipsConceptMapWhenGeminiKeyUnavailable() {
        val metadata =
            SessionMetadata(
                nodeTransitions =
                    listOf(
                        mapOf("from_node" to "CI", "to_node" to "SIM_CC"),
                    ),
            )

        val decision = useCase.decide(metadata)

        // Release builds without GEMINI_API_KEY must not surface a broken concept map.
        assertTrue(decision is com.ncert7.aitutorandlab.domain.chatbot.model.ResourceDecision.None)
    }
}
