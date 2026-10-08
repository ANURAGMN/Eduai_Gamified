package com.ncert7.aitutorandlab.service.analytics

/**
 * Pure helpers for the GA4 / Google Ads signup conversion.
 *
 * Ads imports Firebase Key Event [EVENT_NAME] (`sign_up`). Fire only after a **new**
 * `users/{uid}` write ([com.ncert7.aitutorandlab.repository.CreateUserResult.Created]).
 *
 * String literals match `FirebaseAnalytics.Event.SIGN_UP` / `Param.METHOD` so JVM unit tests
 * do not need the Android Firebase Analytics runtime.
 */
object SignUpAnalytics {
    const val EVENT_NAME: String = "sign_up"
    const val PARAM_METHOD: String = "method"

    /**
     * Maps Firebase Auth `providerId` list to GA4 `method`.
     * Skips the synthetic `firebase` provider; defaults to `unknown`.
     */
    fun methodFromProviderIds(providerIds: List<String>?): String {
        val provider = providerIds
            ?.firstOrNull { it.isNotBlank() && it != "firebase" }
            ?: return "unknown"
        return when (provider) {
            "google.com" -> "google"
            "password" -> "email"
            else -> provider
        }
    }
}
