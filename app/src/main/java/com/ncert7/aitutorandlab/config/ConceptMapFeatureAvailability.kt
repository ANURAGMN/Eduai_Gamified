package com.ncert7.aitutorandlab.config

import com.ncert7.aitutorandlab.BuildConfig

object ConceptMapFeatureAvailability {

    /** Concept maps call Gemini directly; hide the flow when no API key is baked into release. */
    fun isEnabled(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()
}
