package com.example.tiktak_app.datasource

import com.example.tiktak_app.datasource.network.apiErrorMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class ResponseErrorsTest {

    private fun errorResponse(code: Int, body: String = ""): Response<Unit> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    @Test
    fun `unauthorized message contains code and server response`() {
        assertEquals(
            "Обработка ошибки 401, сервер вернул:\nНеверный логин или пароль",
            errorResponse(401, """{"error":"Неверный логин или пароль"}""")
                .apiErrorMessage("fallback")
        )
    }

    @Test
    fun `unauthorized without body contains code and fallback`() {
        assertEquals(
            "Обработка ошибки 401, сервер вернул:\nfallback",
            errorResponse(401).apiErrorMessage("fallback")
        )
    }

    @Test
    fun `server error message is parsed from body`() {
        assertEquals(
            "Неверный пароль",
            errorResponse(400, """{"error":"Неверный пароль"}""")
                .apiErrorMessage("fallback")
        )
    }

    @Test
    fun `fallback is used for empty or broken body`() {
        assertEquals("fallback", errorResponse(500).apiErrorMessage("fallback"))
        assertEquals(
            "fallback",
            errorResponse(500, "not json").apiErrorMessage("fallback")
        )
        assertEquals(
            "fallback",
            errorResponse(500, """{"message":"no error field"}""")
                .apiErrorMessage("fallback")
        )
    }
}
