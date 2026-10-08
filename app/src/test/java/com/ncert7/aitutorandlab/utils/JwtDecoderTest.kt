package com.ncert7.aitutorandlab.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

/**
 * Thorough coverage for the Gson/Base64 JWT decoder that replaced auth0 java-jwt.
 */
class JwtDecoderTest {

    private fun jwt(
        expSeconds: Long? = System.currentTimeMillis() / 1000 + 3600,
        iatSeconds: Long? = null,
        email: String? = "student@example.com",
        name: String? = "Test Student",
        extraJson: String = "",
        padPayload: Boolean = false,
    ): String {
        val header = encode("""{"alg":"RS256","typ":"JWT"}""", padded = true)
        val iat = iatSeconds ?: ((expSeconds ?: 0L) - 3600)
        val fields = buildList {
            if (expSeconds != null) add("\"exp\":$expSeconds")
            add("\"iat\":$iat")
            if (email != null) add("\"email\":\"$email\"")
            if (name != null) add("\"name\":\"$name\"")
            if (extraJson.isNotBlank()) add(extraJson.trim().trimStart(','))
        }.joinToString(",")
        val payload = encode("{$fields}", padded = padPayload)
        return "$header.$payload.sig"
    }

    private fun encode(raw: String, padded: Boolean): String {
        val encoder = Base64.getUrlEncoder()
        return if (padded) {
            encoder.encodeToString(raw.toByteArray(Charsets.UTF_8))
        } else {
            encoder.withoutPadding().encodeToString(raw.toByteArray(Charsets.UTF_8))
        }
    }

    // ── Happy path ──────────────────────────────────────────────────────────

    @Test
    fun `reads exp iat email and name from unpadded payload`() {
        val exp = System.currentTimeMillis() / 1000 + 7200
        val iat = exp - 3600
        val token = jwt(expSeconds = exp, iatSeconds = iat, padPayload = false)

        assertTrue(JwtDecoder.hasExpClaim(token))
        assertEquals(exp, JwtDecoder.getExpiryTimeInSeconds(token))
        assertEquals(exp * 1000, JwtDecoder.getExpiryTimeInMillis(token))
        assertEquals(iat, JwtDecoder.getIssuedAtInSeconds(token))
        assertEquals("student@example.com", JwtDecoder.getEmailFromToken(token))
        assertEquals("Test Student", JwtDecoder.getNameFromToken(token))
        assertFalse(JwtDecoder.isTokenExpired(token))
        assertFalse(JwtDecoder.isTokenExpiringWithinBuffer(token, bufferSeconds = 600))
        val remaining = JwtDecoder.getSecondsUntilExpiry(token)!!
        assertTrue(remaining in 7100..7200)
    }

    @Test
    fun `reads claims when payload has standard base64 padding`() {
        val exp = System.currentTimeMillis() / 1000 + 1800
        val token = jwt(expSeconds = exp, padPayload = true)
        assertTrue(JwtDecoder.hasExpClaim(token))
        assertEquals(exp, JwtDecoder.getExpiryTimeInSeconds(token))
        assertFalse(JwtDecoder.isTokenExpired(token))
    }

    // ── Expiry / buffer ─────────────────────────────────────────────────────

    @Test
    fun `detects expired token`() {
        val exp = System.currentTimeMillis() / 1000 - 30
        val token = jwt(expSeconds = exp)
        assertTrue(JwtDecoder.isTokenExpired(token))
        assertTrue(JwtDecoder.isTokenExpiringWithinBuffer(token, bufferSeconds = 600))
        val remaining = JwtDecoder.getSecondsUntilExpiry(token)!!
        assertTrue(remaining < 0)
    }

    @Test
    fun `token inside buffer is not expired but is expiring`() {
        val exp = System.currentTimeMillis() / 1000 + 120 // 2 minutes left
        val token = jwt(expSeconds = exp)
        assertFalse(JwtDecoder.isTokenExpired(token))
        assertTrue(JwtDecoder.isTokenExpiringWithinBuffer(token, bufferSeconds = 600))
        assertFalse(JwtDecoder.isTokenExpiringWithinBuffer(token, bufferSeconds = 60))
    }

    @Test
    fun `token just outside buffer is neither expired nor expiring`() {
        val exp = System.currentTimeMillis() / 1000 + 900 // 15 minutes
        val token = jwt(expSeconds = exp)
        assertFalse(JwtDecoder.isTokenExpired(token))
        assertFalse(JwtDecoder.isTokenExpiringWithinBuffer(token, bufferSeconds = 600))
    }

    @Test
    fun `default buffer is ten minutes`() {
        val inside = jwt(expSeconds = System.currentTimeMillis() / 1000 + 599)
        val outside = jwt(expSeconds = System.currentTimeMillis() / 1000 + 601)
        assertTrue(JwtDecoder.isTokenExpiringWithinBuffer(inside))
        assertFalse(JwtDecoder.isTokenExpiringWithinBuffer(outside))
    }

    // ── Missing / blank claims ──────────────────────────────────────────────

    @Test
    fun `payload without exp has no exp claim and is treated expired`() {
        val header = encode("""{"alg":"RS256"}""", padded = false)
        val payload = encode("""{"email":"a@b.com","iat":100}""", padded = false)
        val token = "$header.$payload.sig"
        assertFalse(JwtDecoder.hasExpClaim(token))
        assertNull(JwtDecoder.getExpiryTimeInSeconds(token))
        assertNull(JwtDecoder.getExpiryTimeInMillis(token))
        assertNull(JwtDecoder.getSecondsUntilExpiry(token))
        assertTrue(JwtDecoder.isTokenExpired(token))
        assertTrue(JwtDecoder.isTokenExpiringWithinBuffer(token))
        assertEquals("a@b.com", JwtDecoder.getEmailFromToken(token))
    }

    @Test
    fun `blank email and name become null`() {
        val exp = System.currentTimeMillis() / 1000 + 1000
        val header = encode("""{"alg":"RS256"}""", padded = false)
        val payload = encode("""{"exp":$exp,"email":"","name":"   "}""", padded = false)
        // name with spaces isNotBlank() — "   " is blank after? takeIf { it.isNotBlank() } — spaces are blank in Kotlin? 
        // "   ".isNotBlank() is false in Kotlin. Good.
        val token = "$header.$payload.sig"
        assertNull(JwtDecoder.getEmailFromToken(token))
        assertNull(JwtDecoder.getNameFromToken(token))
        assertTrue(JwtDecoder.hasExpClaim(token))
    }

    @Test
    fun `null claim fields are ignored`() {
        val exp = System.currentTimeMillis() / 1000 + 1000
        val header = encode("""{"alg":"RS256"}""", padded = false)
        val payload = encode("""{"exp":$exp,"email":null,"name":null}""", padded = false)
        val token = "$header.$payload.sig"
        assertNull(JwtDecoder.getEmailFromToken(token))
        assertNull(JwtDecoder.getNameFromToken(token))
        assertEquals(exp, JwtDecoder.getExpiryTimeInSeconds(token))
    }

    // ── Malformed input ─────────────────────────────────────────────────────

    @Test
    fun `blank and malformed tokens have no exp and are expired`() {
        val bad = listOf("", " ", "not-a-jwt", "a.b", "onlyone", "...", "a.!!!.c")
        for (token in bad) {
            assertFalse("hasExpClaim($token)", JwtDecoder.hasExpClaim(token))
            assertTrue("isTokenExpired($token)", JwtDecoder.isTokenExpired(token))
            assertTrue("isTokenExpiringWithinBuffer($token)", JwtDecoder.isTokenExpiringWithinBuffer(token))
            assertNull(JwtDecoder.getSecondsUntilExpiry(token))
            assertNull(JwtDecoder.getEmailFromToken(token))
            assertNull(JwtDecoder.getNameFromToken(token))
            assertNull(JwtDecoder.getIssuedAtInSeconds(token))
        }
    }

    @Test
    fun `invalid json payload does not throw`() {
        val header = encode("""{"alg":"none"}""", padded = false)
        val payload = encode("not-json", padded = false)
        val token = "$header.$payload.sig"
        assertFalse(JwtDecoder.hasExpClaim(token))
        assertTrue(JwtDecoder.isTokenExpired(token))
    }

    @Test
    fun `two-segment token still parses payload`() {
        // Some callers may pass header.payload without signature
        val exp = System.currentTimeMillis() / 1000 + 500
        val header = encode("""{"alg":"RS256"}""", padded = false)
        val payload = encode("""{"exp":$exp,"email":"x@y.z"}""", padded = false)
        val token = "$header.$payload"
        assertTrue(JwtDecoder.hasExpClaim(token))
        assertEquals("x@y.z", JwtDecoder.getEmailFromToken(token))
    }
}
