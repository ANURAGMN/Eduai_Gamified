package com.ncert7.aitutorandlab.config

import com.ncert7.aitutorandlab.BuildConfig

/**
 * Reels / video-lessons feature gate. Off by default so it never affects a release until the
 * backend sync + Play compliance (youtube-nocookie, Made-for-kids, no share/links) are ready.
 * Enable per build via `REELS_ENABLED=true` in local.properties.
 */
object ReelsFeatureFlags {
    fun isReelsEnabled(): Boolean = BuildConfig.REELS_ENABLED
}
