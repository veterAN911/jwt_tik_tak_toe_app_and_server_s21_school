package com.example.tiktak_app.presentation

import com.example.tiktak_app.domain.repository.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ErrorMapperTest {

    @Test
    fun `api exception message is shown to user`() {
        assertEquals(
            "Сервер недоступен",
            ApiException(500, "Сервер недоступен").toUserMessage("fallback")
        )
    }

    @Test
    fun `network error shows connection message`() {
        assertEquals(
            "Нет соединения с сервером",
            IOException("timeout").toUserMessage("fallback")
        )
    }

    @Test
    fun `empty message uses fallback`() {
        assertEquals("fallback", IllegalStateException().toUserMessage("fallback"))
        assertEquals("fallback", ApiException(500, "").toUserMessage("fallback"))
    }

    @Test
    fun `unauthorized api exception is a session error`() {
        assertTrue(ApiException(401, "unauthorized").isSessionError())
        assertFalse(ApiException(500, "server").isSessionError())
        assertFalse(IOException("timeout").isSessionError())
    }

    @Test
    fun `expired session messages are detected`() {
        assertTrue(IllegalStateException("Сессия истекла, войдите заново").isSessionError())
        assertTrue(RuntimeException("Некорректный токен").isSessionError())
        assertFalse(RuntimeException("Ошибка игры").isSessionError())
    }
}
