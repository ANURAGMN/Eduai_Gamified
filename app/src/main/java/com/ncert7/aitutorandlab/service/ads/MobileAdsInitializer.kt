package com.ncert7.aitutorandlab.service.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.debug.DebugLogger

/**
 * Initializes Google Mobile Ads with child-safe settings for NCERT Class 7 (Play Families / IARC).
 *
 * Required for Play policy: ads must match the app's content rating (Everyone / ages 12+).
 * See RequestConfiguration max content rating G + child-directed treatment.
 */
object MobileAdsInitializer {

    private const val TAG = "MobileAdsInitializer"

    /** Google sample IDs — safe for debug only. */
    private val TEST_APP_ID_SUFFIX = "3940256099942544"

    fun initialize(context: Context) {
        MobileAds.setRequestConfiguration(buildRequestConfiguration())
        MobileAds.initialize(context)
        logAdConfiguration()
    }

    internal fun buildRequestConfiguration(): RequestConfiguration {
        val testDeviceIds = buildList {
            if (BuildConfig.ADMOB_TEST_DEVICE_ID.isNotBlank()) {
                add(BuildConfig.ADMOB_TEST_DEVICE_ID)
            }
        }.distinct()

        val builder = RequestConfiguration.Builder()
            .setTagForChildDirectedTreatment(
                RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
            )
            .setTagForUnderAgeOfConsent(
                RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
            )
            .setMaxAdContentRating(RequestConfiguration.MAX_AD_CONTENT_RATING_G)

        if (testDeviceIds.isNotEmpty()) {
            builder.setTestDeviceIds(testDeviceIds)
            DebugLogger.debugLog(TAG, "Test device IDs configured: $testDeviceIds")
        }

        return builder.build()
    }

    private fun logAdConfiguration() {
        val usingTestIds = BuildConfig.ADMOB_APP_ID.contains(TEST_APP_ID_SUFFIX)
        DebugLogger.debugLog(
            TAG,
            "Mobile Ads initialized | childDirected=true | maxRating=G | testIds=$usingTestIds | debug=${BuildConfig.DEBUG}"
        )
        if (!BuildConfig.DEBUG && usingTestIds) {
            DebugLogger.errorLog(
                TAG,
                "Release build is using Google sample AdMob IDs — replace ADMOB_APP_ID, BANNER_AD_UNIT_ID, and REWARDED_AD_UNIT_ID in local.properties"
            )
        }
    }

    fun isUsingTestAdIds(): Boolean =
        BuildConfig.ADMOB_APP_ID.contains(TEST_APP_ID_SUFFIX) ||
            BuildConfig.BANNER_AD_UNIT_ID.contains(TEST_APP_ID_SUFFIX) ||
            BuildConfig.REWARDED_AD_UNIT_ID.contains(TEST_APP_ID_SUFFIX)
}
