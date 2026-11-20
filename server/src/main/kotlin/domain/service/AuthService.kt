package domain.service

import domain.model.AuthTokens

interface AuthService {
    fun register(login: String, password: String): Boolean

    fun authorize(login: String, password: String): AuthTokens?

    fun refreshAccessToken(refreshToken: String): AuthTokens?

    fun refreshRefreshToken(refreshToken: String): AuthTokens?
}
