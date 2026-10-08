package com.ncert7.aitutorandlab.utils

import com.google.gson.JsonParser
import java.util.Base64

/**
 * Decode Google ID tokens without auth0/java-jwt.
 *
 * java-jwt pulls Jackson [TypeReference] paths that R8 minify strips on release,
 * which caused every token to look expired and broke chat auth refresh.
 *
 * Intentionally silent: runs on every OkHttp request — do not log to Firestore here.
 */
object JwtDecoder {

    private const val DEFAULT_BUFFER_SECONDS = 600L // 10 minutes

    private data class Claims(
        val expSeconds: Long?,
        val iatSeconds: Long?,
        val email: String?,
        val name: String?,
    )

    private fun parseClaims(token: String): Claims? {
        if (token.isBlank()) return null
        val parts = token.split('.')
        if (parts.size < 2) return null
        return try {
            val payloadJson = String(base64UrlDecode(parts[1]), Charsets.UTF_8)
            val obj = JsonParser.parseString(payloadJson).asJsonObject
            Claims(
                expSeconds = obj.get("exp")?.takeUnless { it.isJsonNull }?.asLong,
                iatSeconds = obj.get("iat")?.takeUnless { it.isJsonNull }?.asLong,
                email = obj.get("email")?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() },
                name = obj.get("name")?.takeUnless { it.isJsonNull }?.asString?.takeIf { it.isNotBlank() },
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun base64UrlDecode(segment: String): ByteArray {
        // URL-safe decoder; add padding if stripped (common in JWTs).
        var padded = segment
        val pad = (4 - padded.length % 4) % 4
        if (pad > 0) padded += "====".substring(0, pad)
        return Base64.getUrlDecoder().decode(padded)
    }

    /** True when the payload has a readable `exp` claim. */
    fun hasExpClaim(token: String): Boolean = parseClaims(token)?.expSeconds != null

    fun getExpiryTimeInSeconds(token: String): Long? = parseClaims(token)?.expSeconds

    fun getExpiryTimeInMillis(token: String): Long? {
        val expirySeconds = getExpiryTimeInSeconds(token) ?: return null
        return expirySeconds * 1000
    }

    /**
     * @return true if expired, or if [token] cannot be parsed (no usable `exp`).
     * Callers that have a stored expiry fallback should check [hasExpClaim] first.
     */
    fun isTokenExpired(token: String): Boolean {
        val expSeconds = parseClaims(token)?.expSeconds ?: return true
        return System.currentTimeMillis() >= expSeconds * 1000
    }

    fun isTokenExpiringWithinBuffer(token: String, bufferSeconds: Long = DEFAULT_BUFFER_SECONDS): Boolean {
        val expSeconds = parseClaims(token)?.expSeconds ?: return true
        val secondsUntilExpiry = expSeconds - (System.currentTimeMillis() / 1000)
        return secondsUntilExpiry <= bufferSeconds
    }

    fun getSecondsUntilExpiry(token: String): Long? {
        val expSeconds = parseClaims(token)?.expSeconds ?: return null
        return expSeconds - (System.currentTimeMillis() / 1000)
    }

    fun getEmailFromToken(token: String): String? = parseClaims(token)?.email

    fun getNameFromToken(token: String): String? = parseClaims(token)?.name

    fun getIssuedAtInSeconds(token: String): Long? = parseClaims(token)?.iatSeconds
}
