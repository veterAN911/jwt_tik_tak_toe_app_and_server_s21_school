package com.example.tiktak_app.datasource.network

import com.google.gson.JsonParser
import retrofit2.Response

fun Response<*>.apiErrorMessage(fallback: String): String {
    val raw = runCatching { errorBody()?.string() }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }

    val serverMessage = raw?.let {
        runCatching {
            JsonParser.parseString(it).asJsonObject.get("error")?.asString
        }.getOrNull()
    }?.takeIf { it.isNotBlank() } ?: fallback

    return if (code() == 401) {
        "Обработка ошибки 401, сервер вернул:\n$serverMessage"
    } else {
        serverMessage
    }
}
