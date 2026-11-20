package com.example.tiktak_app.domain.repository

import com.example.tiktak_app.FakeApiService
import com.example.tiktak_app.FakeAuthApi
import com.example.tiktak_app.FakeGameDao
import com.example.tiktak_app.FakeUserDao
import com.example.tiktak_app.USER_ID
import com.example.tiktak_app.datasource.TokenManager
import com.example.tiktak_app.datasource.database.DatabaseDataSource
import com.example.tiktak_app.datasource.database.entity.HistoryEntity
import com.example.tiktak_app.datasource.database.entity.LeaderboardEntity
import com.example.tiktak_app.datasource.database.entity.TokenEntity
import com.example.tiktak_app.datasource.database.entity.UserEntity
import com.example.tiktak_app.datasource.network.NetworkDataSource
import com.example.tiktak_app.datasource.network.dto.LoginRequestDto
import com.example.tiktak_app.datasource.network.dto.TokenResponseDto
import com.example.tiktak_app.datasource.network.dto.UserDto
import com.example.tiktak_app.domain.model.AuthTokens
import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.domain.model.User
import com.example.tiktak_app.errorResponse
import com.example.tiktak_app.testJwt
import com.example.tiktak_app.utils.JwtUtil
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AuthRepositoryImplTest {

    private lateinit var userDao: FakeUserDao
    private lateinit var gameDao: FakeGameDao
    private lateinit var authApi: FakeAuthApi
    private lateinit var apiService: FakeApiService
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        userDao = FakeUserDao()
        gameDao = FakeGameDao()
        authApi = FakeAuthApi()
        apiService = FakeApiService()
        val jwtUtil = JwtUtil()
        val database = DatabaseDataSource(userDao, gameDao)
        val network = NetworkDataSource(authApi, apiService)
        tokenManager = TokenManager(userDao, authApi, jwtUtil)
        repository = AuthRepositoryImpl(network, database, tokenManager, jwtUtil)
    }

    @Test
    fun `login saves tokens and user id from access token`() = runTest {
        val accessToken = testJwt(uuid = "id-1")
        authApi.loginResponse = Response.success(
            TokenResponseDto(accessToken, testJwt(expiresInSeconds = 86_400))
        )

        val user = repository.login("player", "secret").getOrThrow()

        assertEquals(User("id-1", "player"), user)
        assertEquals(LoginRequestDto("player", "secret"), authApi.lastLogin)
        assertEquals(accessToken, userDao.tokens?.accessToken)
        assertEquals(UserEntity("id-1", "player"), userDao.user)
    }

    @Test
    fun `login with invalid token fails and saves nothing`() = runTest {
        authApi.loginResponse = Response.success(TokenResponseDto("broken", "broken"))

        val result = repository.login("player", "secret")

        assertTrue(result.exceptionOrNull() is IllegalStateException)
        assertNull(userDao.tokens)
        assertNull(userDao.user)
    }

    @Test
    fun `login error returns code and server message`() = runTest {
        authApi.loginResponse = errorResponse(401, "Неверный логин или пароль")

        val result = repository.login("player", "wrong")

        val error = result.exceptionOrNull() as ApiException
        assertEquals(401, error.code)
        assertEquals(
            "Обработка ошибки 401, сервер вернул:\nНеверный логин или пароль",
            error.message
        )
        assertEquals(1, authApi.loginCalls)
    }

    @Test
    fun `server login error message is returned`() = runTest {
        authApi.loginResponse = errorResponse(400, "Неверный пароль")

        val error = repository.login("player", "wrong").exceptionOrNull() as ApiException

        assertEquals(400, error.code)
        assertEquals("Неверный пароль", error.message)
    }

    @Test
    fun `register returns success`() = runTest {
        val result = repository.register("player", "secret")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `register error returns api exception`() = runTest {
        authApi.signUpResponse = errorResponse(409, "Логин занят")

        val error = repository.register("player", "secret").exceptionOrNull() as ApiException

        assertEquals(409, error.code)
        assertEquals("Логин занят", error.message)
    }

    @Test
    fun `auto login without tokens fails with no session`() = runTest {
        val result = repository.autoLogin()

        assertTrue(result.exceptionOrNull() is NoSessionException)
    }

    @Test
    fun `auto login with valid tokens loads current user`() = runTest {
        userDao.saveTokens(
            TokenEntity(
                accessToken = testJwt(uuid = "id-7"),
                refreshToken = testJwt(expiresInSeconds = 86_400)
            )
        )
        apiService.userResponse = Response.success(UserDto("id-7", "saved"))

        val user = repository.autoLogin().getOrThrow()

        assertEquals(User("id-7", "saved"), user)
        assertEquals("id-7", apiService.lastUserId)
        assertEquals(0, authApi.refreshCalls)
        assertEquals(UserEntity("id-7", "saved"), userDao.user)
    }

    @Test
    fun `auto login refreshes expired access token`() = runTest {
        val expiredAccess = testJwt(expiresInSeconds = -3600)
        val validRefresh = testJwt(expiresInSeconds = 86_400)
        val newAccess = testJwt(uuid = "id-8")
        val newRefresh = testJwt(uuid = "id-8", expiresInSeconds = 86_400)
        userDao.saveTokens(
            TokenEntity(accessToken = expiredAccess, refreshToken = validRefresh)
        )
        authApi.refreshResponse = Response.success(TokenResponseDto(newAccess, newRefresh))
        apiService.userResponse = Response.success(UserDto("id-8", "refreshed"))

        val user = repository.autoLogin().getOrThrow()

        assertEquals("id-8", user.id)
        assertEquals(1, authApi.refreshCalls)
        assertEquals(validRefresh, authApi.lastRefresh?.refreshToken)
        assertEquals(AuthTokens(newAccess, newRefresh), tokenManager.getTokens())
        assertEquals("id-8", apiService.lastUserId)
    }

    @Test
    fun `auto login with expired refresh token clears session`() = runTest {
        userDao.saveTokens(
            TokenEntity(
                accessToken = testJwt(expiresInSeconds = -3600),
                refreshToken = testJwt(expiresInSeconds = -3600)
            )
        )

        val error = repository.autoLogin().exceptionOrNull() as ApiException

        assertEquals(401, error.code)
        assertEquals(0, authApi.refreshCalls)
        assertNull(userDao.tokens)
    }

    @Test
    fun `auto login with failed refresh clears session`() = runTest {
        userDao.saveTokens(
            TokenEntity(
                accessToken = testJwt(expiresInSeconds = -3600),
                refreshToken = testJwt(expiresInSeconds = 86_400)
            )
        )
        authApi.refreshResponse = errorResponse(401, "expired")

        val error = repository.autoLogin().exceptionOrNull() as ApiException

        assertEquals(401, error.code)
        assertEquals(1, authApi.refreshCalls)
        assertNull(userDao.tokens)
    }

    @Test
    fun `auto login user request failure is returned`() = runTest {
        userDao.saveTokens(
            TokenEntity(
                accessToken = testJwt(),
                refreshToken = testJwt(expiresInSeconds = 86_400)
            )
        )
        apiService.userResponse = errorResponse(500, "Пользователь не найден")

        val error = repository.autoLogin().exceptionOrNull() as ApiException

        assertEquals(500, error.code)
        assertEquals("Пользователь не найден", error.message)
        assertNull(userDao.user)
    }

    @Test
    fun `current user is read from database`() = runTest {
        userDao.saveUser(UserEntity(USER_ID, "local"))

        assertEquals(User(USER_ID, "local"), repository.getCurrentUser())
    }

    @Test
    fun `logout clears all stored data`() = runTest {
        userDao.saveUser(UserEntity(USER_ID, "local"))
        userDao.saveTokens(
            TokenEntity(
                accessToken = testJwt(),
                refreshToken = testJwt(expiresInSeconds = 86_400)
            )
        )
        gameDao.saveHistory(
            listOf(HistoryEntity("g", 1L, "bot", FinishedGame.RESULT_WIN))
        )
        gameDao.saveLeaderboard(
            listOf(LeaderboardEntity("u", "top", 1.0, 0))
        )

        repository.logout()

        assertNull(userDao.user)
        assertNull(userDao.tokens)
        assertEquals(0, gameDao.history.size)
        assertEquals(0, gameDao.leaderboard.size)
    }
}
