package com.example.tiktak_app.domain.repository

import com.example.tiktak_app.FakeApiService
import com.example.tiktak_app.FakeAuthApi
import com.example.tiktak_app.FakeGameDao
import com.example.tiktak_app.FakeUserDao
import com.example.tiktak_app.OPPONENT_ID
import com.example.tiktak_app.datasource.TokenManager
import com.example.tiktak_app.datasource.database.DatabaseDataSource
import com.example.tiktak_app.datasource.mapper.toDomain
import com.example.tiktak_app.datasource.network.NetworkDataSource
import com.example.tiktak_app.datasource.network.dto.BoardDto
import com.example.tiktak_app.datasource.network.dto.FinishedGameDto
import com.example.tiktak_app.datasource.network.dto.GameDto
import com.example.tiktak_app.datasource.network.dto.LeaderboardDto
import com.example.tiktak_app.datasource.network.socket.GameSocketClient
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.model.LeaderboardPlayer
import com.example.tiktak_app.emptyBoard
import com.example.tiktak_app.errorResponse
import com.example.tiktak_app.utils.JwtUtil
import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class GameRepositoryImplTest {

    private lateinit var gameDao: FakeGameDao
    private lateinit var apiService: FakeApiService
    private lateinit var repository: GameRepositoryImpl

    @Before
    fun setUp() {
        val userDao = FakeUserDao()
        gameDao = FakeGameDao()
        val authApi = FakeAuthApi()
        apiService = FakeApiService()
        val jwtUtil = JwtUtil()
        val tokenManager = TokenManager(userDao, authApi, jwtUtil)
        val network = NetworkDataSource(authApi, apiService)
        val database = DatabaseDataSource(userDao, gameDao)
        val socketClient = GameSocketClient(OkHttpClient(), tokenManager, Gson())
        repository = GameRepositoryImpl(network, database, socketClient)
    }

    private fun gameDto(
        id: String,
        status: String = Game.STATUS_WAITING
    ): GameDto = GameDto(
        id = id,
        board = BoardDto(emptyBoard()),
        mode = Game.MODE_HUMAN,
        status = status,
        currentPlayer = null,
        winner = null,
        playerX = OPPONENT_ID,
        playerO = null
    )

    @Test
    fun `waiting games are mapped from response`() = runTest {
        apiService.waitingGamesResponse = Response.success(
            listOf(gameDto("g1"), gameDto("g2"))
        )

        val result = repository.getWaitingGames().getOrThrow()

        assertEquals(listOf("g1", "g2"), result.map { it.id })
        assertEquals(Game.STATUS_WAITING, result.first().status)
    }

    @Test
    fun `waiting games error returns api exception`() = runTest {
        apiService.waitingGamesResponse = errorResponse(401, "unauthorized")

        val error = repository.getWaitingGames().exceptionOrNull() as ApiException

        assertTrue(error.isUnauthorized)
        assertEquals("Обработка ошибки 401, сервер вернул:\nunauthorized", error.message)
    }

    @Test
    fun `finished games are sorted newest first and saved to database`() = runTest {
        apiService.finishedGamesResponse = Response.success(
            listOf(
                FinishedGameDto("old", 10L, "a", "WIN"),
                FinishedGameDto("new", 30L, "b", "LOSS"),
                FinishedGameDto("middle", 20L, "c", "DRAW")
            )
        )

        val result = repository.getFinishedGames().getOrThrow()

        assertEquals(listOf("new", "middle", "old"), result.map { it.gameId })
        assertEquals(3, gameDao.history.size)
        assertEquals(
            listOf("new", "middle", "old"),
            gameDao.getHistory().map { it.gameId }
        )
    }

    @Test
    fun `finished games error is returned`() = runTest {
        apiService.finishedGamesResponse = errorResponse(500, "История недоступна")

        val error = repository.getFinishedGames().exceptionOrNull() as ApiException

        assertEquals(500, error.code)
        assertEquals("История недоступна", error.message)
        assertEquals(0, gameDao.history.size)
    }

    @Test
    fun `leaderboard is sorted by ratio and saved with positions`() = runTest {
        apiService.leaderboardResponse = Response.success(
            listOf(
                LeaderboardDto("u1", "low", 1.0),
                LeaderboardDto("u2", "top", 5.0),
                LeaderboardDto("u3", "middle", 3.0)
            )
        )

        val result = repository.getLeaderboard().getOrThrow()

        assertEquals(
            listOf(
                LeaderboardPlayer("u2", "top", 5.0),
                LeaderboardPlayer("u3", "middle", 3.0),
                LeaderboardPlayer("u1", "low", 1.0)
            ),
            result
        )
        assertEquals(20, apiService.lastLeaderboardLimit)
        assertEquals(listOf(0, 1, 2), gameDao.leaderboard.map { it.position })
        assertEquals(
            listOf("u2", "u3", "u1"),
            gameDao.getLeaderboard().map { it.userId }
        )
    }

    @Test
    fun `leaderboard keeps at most twenty players`() = runTest {
        apiService.leaderboardResponse = Response.success(
            (1..25).map { index ->
                LeaderboardDto("u$index", "player$index", index.toDouble())
            }
        )

        val result = repository.getLeaderboard().getOrThrow()

        assertEquals(20, result.size)
        assertEquals(25.0, result.first().winsRatio, 0.0)
        assertEquals(20, gameDao.leaderboard.size)
    }

    @Test
    fun `leaderboard error is returned`() = runTest {
        apiService.leaderboardResponse = errorResponse(401, "unauthorized")

        val error = repository.getLeaderboard().exceptionOrNull() as ApiException

        assertTrue(error.isUnauthorized)
        assertEquals(0, gameDao.leaderboard.size)
    }

    @Test
    fun `game dto used by waiting list maps to domain`() {
        val game = gameDto("g1").toDomain()

        assertEquals("g1", game.id)
        assertEquals(Game.MODE_HUMAN, game.mode)
        assertEquals(OPPONENT_ID, game.playerX)
    }
}
