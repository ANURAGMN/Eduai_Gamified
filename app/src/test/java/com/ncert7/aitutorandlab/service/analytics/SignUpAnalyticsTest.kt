package com.ncert7.aitutorandlab.service.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class SignUpAnalyticsTest {

    @Test
    fun eventName_isStandardGa4SignUp() {
        assertEquals("sign_up", SignUpAnalytics.EVENT_NAME)
    }

    @Test
    fun paramMethod_isStandardGa4Method() {
        assertEquals("method", SignUpAnalytics.PARAM_METHOD)
    }

    @Test
    fun methodFromProviderIds_mapsGoogle() {
        assertEquals("google", SignUpAnalytics.methodFromProviderIds(listOf("firebase", "google.com")))
    }

    @Test
    fun methodFromProviderIds_mapsPasswordAsEmail() {
        assertEquals("email", SignUpAnalytics.methodFromProviderIds(listOf("password")))
    }

    @Test
    fun methodFromProviderIds_skipsFirebaseOnly() {
        assertEquals("unknown", SignUpAnalytics.methodFromProviderIds(listOf("firebase")))
    }

    @Test
    fun methodFromProviderIds_nullOrEmpty_isUnknown() {
        assertEquals("unknown", SignUpAnalytics.methodFromProviderIds(null))
        assertEquals("unknown", SignUpAnalytics.methodFromProviderIds(emptyList()))
    }

    @Test
    fun methodFromProviderIds_passesThroughOtherProviders() {
        assertEquals("phone", SignUpAnalytics.methodFromProviderIds(listOf("phone")))
    }
}
