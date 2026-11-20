package domain.service

import domain.model.AuthTokens
import java.util.UUID

class AuthServiceJwt(
    private val userService: UserService,
    private val jwtProvider: JwtProvider
) : AuthService {

    override fun register(login: String, password: String): Boolean {
        val hash = password.hashCode().toString()
        return userService.createUser(login, hash)
    }

    override fun authorize(login: String, password: String): AuthTokens? {
        val user = userService.getUserByLogin(login) ?: return null
        val hash = password.hashCode().toString()

        if (user.passwordHash != hash) return null

        return AuthTokens(
            accessToken = jwtProvider.generateAccessToken(user),
            refreshToken = jwtProvider.generateRefreshToken(user)
        )
    }

    override fun refreshAccessToken(refreshToken: String): AuthTokens? {
        if (!jwtProvider.validateRefreshToken(refreshToken)) return null

        val claims = jwtProvider.getClaimsFromRefresh(refreshToken)
        val uuid = UUID.fromString(claims["uuid"].toString())

        val user = userService.getUserById(uuid) ?: return null

        return AuthTokens(
            accessToken = jwtProvider.generateAccessToken(user),
            refreshToken = refreshToken
        )
    }

    override fun refreshRefreshToken(refreshToken: String): AuthTokens? {
        if (!jwtProvider.validateRefreshToken(refreshToken)) return null

        val claims = jwtProvider.getClaimsFromRefresh(refreshToken)
        val uuid = UUID.fromString(claims["uuid"].toString())

        val user = userService.getUserById(uuid) ?: return null

        return AuthTokens(
            accessToken = jwtProvider.generateAccessToken(user),
            refreshToken = jwtProvider.generateRefreshToken(user)
        )
    }
}
