package com.example.tiktak_app.domain.repository

class ApiException(
    val code: Int,
    message: String
) : Exception(message) {
    val isUnauthorized: Boolean
        get() = code == 401
}

class NoSessionException : Exception()
