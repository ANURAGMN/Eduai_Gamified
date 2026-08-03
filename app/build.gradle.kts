import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("com.google.gms.google-services")
    alias(libs.plugins.firebase.crashlytics)
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.ncert7.aitutorandlab"
    compileSdk = 36

    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) load(f.inputStream())
    }
    fun prop(name: String, default: String = ""): String = localProps.getProperty(name, default)

    defaultConfig {
        applicationId = "com.ncert7.aitutorandlab"
        minSdk = 28
        targetSdk = 35
        versionCode = 13
        versionName = "1.0.11"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "AUTH_KEY", "\"${prop("AUTH_KEY")}\"")
        buildConfigField("String", "AGENTIC_AI_BASE_URL", "\"${prop("AGENTIC_AI_BASE_URL")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${prop("GEMINI_API_KEY")}\"")
        buildConfigField("String", "GROQ_API_KEY", "\"${prop("GROQ_API_KEY")}\"")
        buildConfigField("String", "SIMULATION_BASE_URL", "\"${prop("SIMULATION_BASE_URL")}\"")
        // API key and header name for backend authentication (from local.properties)
        buildConfigField("String", "API_KEYS", "\"${prop("API_KEYS")}\"")
        buildConfigField("String", "API_KEY_HEADER_NAME", "\"${prop("API_KEY_HEADER_NAME")}\"")
        buildConfigField("String", "ADMOB_APP_ID", "\"${prop("ADMOB_APP_ID")}\"")
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"${prop("BANNER_AD_UNIT_ID")}\"")
        buildConfigField(
            "String",
            "REWARDED_AD_UNIT_ID",
            "\"${prop("REWARDED_AD_UNIT_ID", "ca-app-pub-3940256099942544/5224354917")}\"",
        )
        buildConfigField("String", "PADAAMS_SIGNIN_PASSWORD", "\"${prop("PADAAMS_SIGNIN_PASSWORD")}\"")
        buildConfigField(
            "boolean",
            "GAMIFIED_HOME_ENABLED",
            prop("GAMIFIED_HOME_ENABLED", "false").equals("true", ignoreCase = true).toString()
        )
        buildConfigField(
            "boolean",
            "NATIVE_TUTOR_AVATAR_ENABLED",
            prop("NATIVE_TUTOR_AVATAR_ENABLED", "false").equals("true", ignoreCase = true).toString()
        )
        buildConfigField(
            "boolean",
            "GARDEN_ENABLED",
            prop("GARDEN_ENABLED", "false").equals("true", ignoreCase = true).toString()
        )
        // Manifest placeholders for runtime value substitution
        manifestPlaceholders["ADMOB_APP_ID"] = prop("ADMOB_APP_ID")

    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("KEYSTORE_PATH", "keystore.jks"))
            storePassword = localProps.getProperty("KEYSTORE_PASSWORD")
            keyAlias = localProps.getProperty("KEY_ALIAS")
            keyPassword = localProps.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["NATIVE_AD_DEBUGGER"] = "true"
            buildConfigField("String", "ADMOB_TEST_DEVICE_ID", "\"${prop("ADMOB_TEST_DEVICE_ID")}\"")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            manifestPlaceholders["NATIVE_AD_DEBUGGER"] = "false"
            // Never treat devices as test devices in Play release builds
            buildConfigField("String", "ADMOB_TEST_DEVICE_ID", "\"\"")
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.configureEach {
        if (buildType.name == "release") {
            val appId = prop("ADMOB_APP_ID")
            val bannerId = prop("BANNER_AD_UNIT_ID")
            val rewardedId = prop("REWARDED_AD_UNIT_ID", "ca-app-pub-3940256099942544/5224354917")
            if (appId.contains("3940256099942544") ||
                bannerId.contains("3940256099942544") ||
                rewardedId.contains("3940256099942544")
            ) {
                logger.warn(
                    "RELEASE uses Google sample AdMob IDs — set production ADMOB_APP_ID, BANNER_AD_UNIT_ID, and REWARDED_AD_UNIT_ID in local.properties"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

// KSP Configuration for Room Schema Export
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":ui-kit"))

// Compose BOM
    implementation(platform(libs.compose.bom))

// Jetpack Compose - Core
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    debugImplementation("androidx.compose.ui:ui-tooling")

// Material Design
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

// Activity & Navigation
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.navigation:navigation-compose:2.9.4")

// YouTube in-app playback (home video lessons; v13+ sets Referer/Origin for embed policy)
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:13.0.0")

// Media / ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.3.1")

// Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.bumptech.glide:compose:1.0.0-beta01")

// JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

// Google Sign in
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")

// Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation(libs.firebase.crashlytics)


// Legacy / AppCompat / AndroidX Libs from Version Catalog
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)

// room database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    //Retrofit for networking
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.squareup.moshi:moshi:1.15.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    implementation(libs.play.services.auth)       // for GoogleSignIn.getClient()
    implementation(libs.coroutines.play.services) // for .await() on Tasks
    //okhttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // JWT Decoding
    implementation("com.auth0:java-jwt:4.4.0")
    // Google Play In-App Update
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:review:2.0.2")
    implementation("com.google.android.play:review-ktx:2.0.2")

    // Google Mobile Ads SDK
    implementation("com.google.android.gms:play-services-ads:25.4.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}