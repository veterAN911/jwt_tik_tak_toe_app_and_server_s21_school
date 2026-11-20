package com.example.tiktak_app.presentation

import com.example.tiktak_app.domain.repository.ApiException
import java.io.IOException

fun Throwable.toUserMessage(fallback: String): String = when (this) {
    is ApiException -> message?.takeIf { it.isNotBlank() } ?: fallback
    is IOException -> "Нет соединения с сервером"
    else -> message?.takeIf { it.isNotBlank() } ?: fallback
}

fun Throwable.isSessionError(): Boolean =
    (this is ApiException && isUnauthorized) ||
        message?.contains("Сессия истекла", ignoreCase = true) == true ||
        message?.contains("токен", ignoreCase = true) == true
