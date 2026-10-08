package com.ncert7.aitutorandlab.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Pure ErrorHandler paths used by chat / agent clients (no Android Context).
 */
class ErrorHandlerAuthTest {

    @Test
    fun `extractStatusCode prefers HTTP prefix`() {
        assertEquals(500, ErrorHandler.extractStatusCode("HTTP 500: upstream boom"))
        assertEquals(401, ErrorHandler.extractStatusCode("HTTP 401 Unauthorized"))
        assertEquals(503, ErrorHandler.extractStatusCode("SERVER_ERROR_503"))
    }

    @Test
    fun `extractStatusCode falls back to first three digits`() {
        assertEquals(429, ErrorHandler.extractStatusCode("rate limited 429 retry later"))
        assertEquals(0, ErrorHandler.extractStatusCode("no digits here"))
    }

    @Test
    fun `httpStatusFrom reads throwable message`() {
        assertEquals(500, ErrorHandler.httpStatusFrom(IOException("HTTP 500: Server error")))
        assertEquals(401, ErrorHandler.httpStatusFrom(IOException("HTTP 401")))
        assertNull(ErrorHandler.httpStatusFrom(null))
        assertNull(ErrorHandler.httpStatusFrom(IOException("")))
        assertNull(ErrorHandler.httpStatusFrom(IOException("no status")))
    }

    @Test
    fun `isRetryable treats 5xx as retryable and 4xx as not`() {
        assertTrue(ErrorHandler.isRetryable(500))
        assertTrue(ErrorHandler.isRetryable(503))
        assertFalse(ErrorHandler.isRetryable(401))
        assertFalse(ErrorHandler.isRetryable(404))
        assertTrue(ErrorHandler.isRetryable(-1))
    }

    @Test
    fun `shouldRetryException for network and selective 5xx`() {
        assertTrue(ErrorHandler.shouldRetryException(SocketTimeoutException("timeout")))
        assertTrue(ErrorHandler.shouldRetryException(UnknownHostException("dns")))
        assertTrue(ErrorHandler.shouldRetryException(java.net.ConnectException("refused")))
        // Hard 500 (quota-style) — do not immediate-retry
        assertFalse(ErrorHandler.shouldRetryException(IOException("HTTP 500: quota")))
        // Other 5xx still retryable
        assertTrue(ErrorHandler.shouldRetryException(IOException("HTTP 503: unavailable")))
        assertFalse(ErrorHandler.shouldRetryException(IOException("HTTP 401")))
        assertFalse(ErrorHandler.shouldRetryException(null))
        assertFalse(ErrorHandler.shouldRetryException(IllegalStateException("x")))
    }
}
