# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# ========== RETROFIT & OKHTTP ==========
# Keep attributes needed for Retrofit reflection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# Retain Retrofit service method parameters
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Retrofit interfaces (created with Proxy at runtime)
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep Kotlin Continuation for suspend functions
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Keep Retrofit Response class
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Suppress warnings for optional dependencies
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ========== GSON ==========
# Keep Gson classes needed for reflection
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class com.google.gson.internal.** { *; }

# Application classes that will be serialized/deserialized with Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Keep generic type information for Gson
-keepattributes *Annotation*

# Keep Gson TypeAdapters
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all fields with @SerializedName annotation (critical for API models)
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep generic signatures for Gson
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ========== API DATA CLASSES ==========
# Keep all API request/response models
-keep class com.ncert7.aitutorandlab.data.remote.** { *; }
-keep class com.ncert7.aitutorandlab.data.model.** { *; }
-keep class com.ncert7.aitutorandlab.data.firebase.** { *; }

# ========== KOTLINX SERIALIZATION ==========
# Keep serializers for kotlinx.serialization (used for other purposes, not Retrofit)
-keepattributes InnerClasses
-keep,includedescriptorclasses class com.ncert7.aitutorandlab.**$$serializer { *; }
-keepclassmembers class com.ncert7.aitutorandlab.** {
    *** Companion;
}
-keepclasseswithmembers class com.ncert7.aitutorandlab.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ========== COROUTINES ==========
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ========== BUILD CONFIG (gamification flags read at runtime) ==========
-keepclassmembers class com.ncert7.aitutorandlab.BuildConfig {
    public static final boolean GAMIFIED_HOME_ENABLED;
    public static final boolean NATIVE_TUTOR_AVATAR_ENABLED;
    public static final boolean DEBUG;
}
# Keep custom exceptions
-keep public class * extends java.lang.Exception

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ========== HILT / DAGGER ==========
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.internal.Factory
-keep class * extends dagger.internal.ModuleAdapter
-keepclasseswithmembers class * {
    @dagger.* <methods>;
    @javax.inject.* <fields>;
    @javax.inject.* <methods>;
}

# ========== FIREBASE / FIRESTORE ==========
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

# ========== GOOGLE PLAY REVIEW KTX ==========
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# ========== GOOGLE SIGN-IN / CREDENTIAL MANAGER ==========
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# ========== ROOM ==========
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep setters in Views for animations
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

