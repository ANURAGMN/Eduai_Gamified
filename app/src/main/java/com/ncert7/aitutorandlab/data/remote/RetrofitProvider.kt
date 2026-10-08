package com.ncert7.aitutorandlab.data.remote


import android.content.Context
import com.ncert7.aitutorandlab.BuildConfig
import com.ncert7.aitutorandlab.debug.DebugLogger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.google.gson.GsonBuilder

object RetrofitProvider {
    fun buildRetrofit(agenticAIBaseUrl: String, context: Context): Retrofit {
        val buildConfigUrl = BuildConfig.AGENTIC_AI_BASE_URL.trim()
        val base = buildConfigUrl.ifEmpty { agenticAIBaseUrl.trim() }
        val normalized = base.trimEnd('/').ifEmpty {
            DebugLogger.errorLog("RetrofitProvider", "API base URL is empty.")
            throw IllegalArgumentException("API base URL required")
        } + "/"

        val logging = HttpLoggingInterceptor { msg ->
            DebugLogger.debugLog("OkHttp", msg)
        }
        logging.level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }

        // Proactive token attach/refresh before API calls; Authenticator recovers from 401 once
        val proactiveTokenInterceptor = ProactiveTokenInterceptor(context)
        val tokenAuthenticator = TokenAuthenticator(context)
        DebugLogger.debugLog("RetrofitProvider", "Token interceptor + 401 authenticator configured")

        val errorLoggingInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)

            // Log HTTP status codes (debug only — avoid Firestore spam on routine 4xx/5xx)
            if (!response.isSuccessful) {
                DebugLogger.debugLog(
                    "HttpError",
                    "HTTP ${response.code} ${response.message} - ${request.url}"
                )
            }
            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(proactiveTokenInterceptor)
            .addInterceptor(logging)
            .addNetworkInterceptor(errorLoggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .serializeNulls()
            .create()

        DebugLogger.debugLog("RetrofitProvider", "Retrofit base url: $normalized")

        // After client built, log attached interceptors for easier troubleshooting
        try {
            val interceptorNames = client.interceptors.map { it.javaClass.simpleName }
            val networkInterceptorNames = client.networkInterceptors.map { it.javaClass.simpleName }
            DebugLogger.debugLog("RetrofitProvider", "OkHttp interceptors=${interceptorNames}, networkInterceptors=${networkInterceptorNames}")
        } catch (e: Exception) {
            DebugLogger.errorLog("RetrofitProvider","Error enumerating interceptors: ${e.message}")
        }

        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}

