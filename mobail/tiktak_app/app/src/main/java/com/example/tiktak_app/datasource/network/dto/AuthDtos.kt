package com.example.tiktak_app.datasource.network.dto

data class LoginRequestDto(
    val login: String,
    val password: String
)

data class RefreshRequestDto(
    val refreshToken: String
)

data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String
)

data class ErrorResponseDto(
    val error: String?
)
