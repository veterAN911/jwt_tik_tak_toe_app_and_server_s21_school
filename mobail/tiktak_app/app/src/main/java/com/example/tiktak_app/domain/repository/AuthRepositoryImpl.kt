package com.example.tiktak_app.domain.repository

import com.example.tiktak_app.datasource.TokenManager
import com.example.tiktak_app.datasource.database.DatabaseDataSource
import com.example.tiktak_app.datasource.mapper.toDomain
import com.example.tiktak_app.datasource.mapper.toEntity
import com.example.tiktak_app.datasource.network.NetworkDataSource
import com.example.tiktak_app.datasource.network.apiErrorMessage
import com.example.tiktak_app.domain.model.User
import com.example.tiktak_app.utils.JwtUtil
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val network: NetworkDataSource,
    private val database: DatabaseDataSource,
    private val tokenManager: TokenManager,
    private val jwtUtil: JwtUtil
) : AuthRepository {

    override suspend fun login(login: String, password: String): Result<User> {
        return try {
            val response = network.login(login, password)
            val body = response.body()

            if (!response.isSuccessful || body == null) {
                Result.failure(
                    ApiException(response.code(), response.apiErrorMessage("Ошибка авторизации"))
                )
            } else {
                val userId = jwtUtil.userId(body.accessToken)
                    ?: return Result.failure(
                        IllegalStateException("Сервер вернул некорректный токен")
                    )

                tokenManager.saveTokens(body.toDomain())

                val user = User(id = userId, login = login)
                database.saveUser(user.toEntity())

                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(login: String, password: String): Result<Unit> {
        return try {
            val response = network.signUp(login, password)

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(
                    ApiException(response.code(), response.apiErrorMessage("Ошибка регистрации"))
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun autoLogin(): Result<User> {
        return try {
            val tokens = tokenManager.getTokens()
                ?: return Result.failure(NoSessionException())

            if (jwtUtil.isExpired(tokens.refreshToken)) {
                tokenManager.clearTokens()
                return Result.failure(
                    ApiException(401, "Сессия истекла, войдите заново")
                )
            }

            val accessToken = tokenManager.getValidAccessToken()
                ?: return Result.failure(
                    ApiException(401, "Сессия истекла, войдите заново")
                )

            val userId = jwtUtil.userId(accessToken)
                ?: return Result.failure(
                    IllegalStateException("Сервер вернул некорректный токен")
                )

            val response = network.getUser(userId)
            val body = response.body()

            if (!response.isSuccessful || body == null) {
                Result.failure(
                    ApiException(
                        response.code(),
                        response.apiErrorMessage("Не удалось получить данные пользователя")
                    )
                )
            } else {
                val user = body.toDomain()
                database.saveUser(user.toEntity())
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? =
        database.getUser()?.toDomain()

    override suspend fun logout() {
        database.clearAll()
    }
}
