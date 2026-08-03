package com.ncert7.aitutorandlab.ui.models

import com.ncert7.aitutorandlab.domain.progress.model.ProgressStatus

/**
 * UI Model for Concept data
 */
data class ConceptUiModel(
    val id: String,
    val name: String,
    /** English concept title — session/thread map key for the study chatbot API. */
    val sessionKey: String = name,
    val order: Int,
    val status: ProgressStatus,
    val type: String = "STUDY",
    val simulationId: String? = null,
    val simulationIdKannada: String? = null,
    val simulationUrl: String? =null,
    val simulationUrlKannada: String? = null,
    val problemTopicName:String?=null,
    val problemTopicNameKannada:String?=null,
    val problemId: String = ""
)


