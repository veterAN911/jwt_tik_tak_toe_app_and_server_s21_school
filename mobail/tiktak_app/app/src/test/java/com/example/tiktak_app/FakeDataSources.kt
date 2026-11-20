package com.example.tiktak_app

import com.example.tiktak_app.datasource.database.dao.GameDao
import com.example.tiktak_app.datasource.database.dao.UserDao
import com.example.tiktak_app.datasource.database.entity.HistoryEntity
import com.example.tiktak_app.datasource.database.entity.LeaderboardEntity
import com.example.tiktak_app.datasource.database.entity.TokenEntity
import com.example.tiktak_app.datasource.database.entity.UserEntity
import com.example.tiktak_app.datasource.network.api.ApiService
import com.example.tiktak_app.datasource.network.api.AuthApi
import com.example.tiktak_app.datasource.network.dto.FinishedGameDto
import com.example.tiktak_app.datasource.network.dto.GameDto
import com.example.tiktak_app.datasource.network.dto.LeaderboardDto
import com.example.tiktak_app.datasource.network.dto.LoginRequestDto
import com.example.tiktak_app.datasource.network.dto.RefreshRequestDto
import com.example.tiktak_app.datasource.network.dto.TokenResponseDto
import com.example.tiktak_app.datasource.network.dto.UserDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeUserDao : UserDao {

    var user: UserEntity? = null
    var tokens: TokenEntity? = null

    override suspend fun saveUser(user: UserEntity) {
        this.user = user
    }

    override suspend fun getUser(): UserEntity? = user

    override suspend fun clearUsers() {
        user = null
    }

    override suspend fun saveTokens(tokens: TokenEntity) {
        this.tokens = tokens
    }

    override suspend fun getTokens(): TokenEntity? = tokens

    override suspend fun clearTokens() {
        tokens = null
    }
}

class FakeGameDao : GameDao {

    var history: List<HistoryEntity> = emptyList()
    var leaderboard: List<LeaderboardEntity> = emptyList()

    override suspend fun saveHistory(items: List<HistoryEntity>) {
        history = items
    }

    override suspend fun getHistory(): List<HistoryEntity> =
        history.sortedByDescending { it.createdAt }

    override suspend fun clearHistory() {
        history = emptyList()
    }

    override suspend fun saveLeaderboard(items: List<LeaderboardEntity>) {
        leaderboard = items
    }

    override suspend fun getLeaderboard(): List<LeaderboardEntity> =
        leaderboard.sortedBy { it.position }

    override suspend fun clearLeaderboard() {
        leaderboard = emptyList()
    }
}

class FakeAuthApi : AuthApi {

    var loginResponse: Response<TokenResponseDto> =
        Response.success(TokenResponseDto(testJwt(), testJwt(expiresInSeconds = 86_400)))
    var signUpResponse: Response<Unit> = Response.success(Unit)
    var refreshResponse: Response<TokenResponseDto> =
        Response.success(TokenResponseDto(testJwt(), testJwt(expiresInSeconds = 86_400)))

    var loginCalls = 0
    var refreshCalls = 0
    var lastLogin: LoginRequestDto? = null
    var lastRefresh: RefreshRequestDto? = null

    override suspend fun login(body: LoginRequestDto): Response<TokenResponseDto> {
        loginCalls++
        lastLogin = body
        return loginResponse
    }

    override suspend fun signUp(body: LoginRequestDto): Response<Unit> {
        lastLogin = body
        return signUpResponse
    }

    override suspend fun refreshAccess(body: RefreshRequestDto): Response<TokenResponseDto> {
        refreshCalls++
        lastRefresh = body
        return refreshResponse
    }
}

class FakeApiService : ApiService {

    var userResponse: Response<UserDto> = Response.success(UserDto(USER_ID, "player"))
    var finishedGamesResponse: Response<List<FinishedGameDto>> = Response.success(emptyList())
    var leaderboardResponse: Response<List<LeaderboardDto>> = Response.success(emptyList())
    var waitingGamesResponse: Response<List<GameDto>> = Response.success(emptyList())

    var lastUserId: String? = null
    var lastLeaderboardLimit: Int? = null

    override suspend fun getUser(id: String): Response<UserDto> {
        lastUserId = id
        return userResponse
    }

    override suspend fun getFinishedGames(): Response<List<FinishedGameDto>> =
        finishedGamesResponse

    override suspend fun getLeaderboard(limit: Int): Response<List<LeaderboardDto>> {
        lastLeaderboardLimit = limit
        return leaderboardResponse
    }

    override suspend fun getWaitingGames(): Response<List<GameDto>> = waitingGamesResponse
}

fun <T> errorResponse(code: Int, message: String): Response<T> =
    Response.error(
        code,
        """{"error":"$message"}""".toResponseBody("application/json".toMediaType())
    )
