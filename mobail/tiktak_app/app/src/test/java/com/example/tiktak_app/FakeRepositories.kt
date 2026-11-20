package com.example.tiktak_app

import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.model.GameMode
import com.example.tiktak_app.domain.model.LeaderboardPlayer
import com.example.tiktak_app.domain.model.User
import com.example.tiktak_app.domain.repository.AuthRepository
import com.example.tiktak_app.domain.repository.GameEvent
import com.example.tiktak_app.domain.repository.GameRepository
import com.example.tiktak_app.domain.repository.GameSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeAuthRepository : AuthRepository {

    var loginResult: Result<User> = Result.success(testUser())
    var registerResult: Result<Unit> = Result.success(Unit)
    var autoLoginResult: Result<User> = Result.success(testUser())
    var currentUser: User? = testUser()

    var logoutCalled = false
    var loginCalls = 0
    var registerCalls = 0
    var autoLoginCalls = 0
    var lastLogin: Pair<String, String>? = null

    override suspend fun login(login: String, password: String): Result<User> {
        loginCalls++
        lastLogin = login to password
        return loginResult
    }

    override suspend fun register(login: String, password: String): Result<Unit> {
        registerCalls++
        return registerResult
    }

    override suspend fun autoLogin(): Result<User> {
        autoLoginCalls++
        return autoLoginResult
    }

    override suspend fun getCurrentUser(): User? = currentUser

    override suspend fun logout() {
        logoutCalled = true
        currentUser = null
    }
}

class FakeGameRepository : GameRepository {

    var waitingGamesResult: Result<List<Game>> = Result.success(emptyList())
    var createGameResult: Result<Game> = Result.success(testGame())
    var openGameResult: Result<GameSession> = Result.success(FakeGameSession())
    var finishedGamesResult: Result<List<FinishedGame>> = Result.success(emptyList())
    var leaderboardResult: Result<List<LeaderboardPlayer>> = Result.success(emptyList())

    var lastCreatedMode: GameMode? = null
    var lastOpenArgs: Pair<String, Boolean>? = null
    var leaderboardCalls = 0
    var createGameGate: CompletableDeferred<Unit>? = null

    override suspend fun getWaitingGames(): Result<List<Game>> = waitingGamesResult

    override suspend fun createGame(mode: GameMode): Result<Game> {
        lastCreatedMode = mode
        createGameGate?.await()
        return createGameResult
    }

    override suspend fun openGame(gameId: String, join: Boolean): Result<GameSession> {
        lastOpenArgs = gameId to join
        return openGameResult
    }

    override suspend fun getFinishedGames(): Result<List<FinishedGame>> =
        finishedGamesResult

    override suspend fun getLeaderboard(): Result<List<LeaderboardPlayer>> {
        leaderboardCalls++
        return leaderboardResult
    }
}

class FakeGameSession : GameSession {

    private val mutableEvents = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)

    override val events: Flow<GameEvent> = mutableEvents.asSharedFlow()

    var lastBoard: List<List<Int>>? = null
    var closed = false

    fun emit(event: GameEvent) {
        mutableEvents.tryEmit(event)
    }

    override fun makeMove(board: List<List<Int>>) {
        lastBoard = board
    }

    override fun close() {
        closed = true
    }
}
