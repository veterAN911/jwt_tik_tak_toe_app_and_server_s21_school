package com.example.tiktak_app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class JwtUtilTest {

    private val jwtUtil = JwtUtil()

    private fun token(payload: String): String {
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.toByteArray())
        return "eyJhbGciOiJIUzI1NiJ9.$encoded.signature"
    }

    private fun now(): Long = System.currentTimeMillis() / 1000

    @Test
    fun `token with future expiration is not expired`() {
        val token = token("""{"uuid":"user-1","exp":${now() + 3600}}""")

        assertFalse(jwtUtil.isExpired(token, clockSkewSeconds = 0))
    }

    @Test
    fun `token with past expiration is expired`() {
        val token = token("""{"uuid":"user-1","exp":${now() - 3600}}""")

        assertTrue(jwtUtil.isExpired(token, clockSkewSeconds = 0))
    }

    @Test
    fun `token expiring inside clock skew is expired`() {
        val token = token("""{"uuid":"user-1","exp":${now() + 5}}""")

        assertTrue(jwtUtil.isExpired(token, clockSkewSeconds = 15))
    }

    @Test
    fun `malformed token is treated as expired`() {
        assertTrue(jwtUtil.isExpired("not-a-token"))
        assertTrue(jwtUtil.isExpired(""))
        assertTrue(jwtUtil.isExpired("header.$$$.signature"))
    }

    @Test
    fun `token without expiration is treated as expired`() {
        assertTrue(jwtUtil.isExpired(token("""{"uuid":"user-1"}""")))
    }

    @Test
    fun `user id is read from uuid claim`() {
        val token = token("""{"uuid":"user-42","exp":${now() + 3600}}""")

        assertEquals("user-42", jwtUtil.userId(token))
    }

    @Test
    fun `user id is null for missing uuid`() {
        assertNull(jwtUtil.userId(token("""{"exp":${now() + 3600}}""")))
        assertNull(jwtUtil.userId(token("""{"uuid":"","exp":${now() + 3600}}""")))
        assertNull(jwtUtil.userId("broken"))
    }
}
