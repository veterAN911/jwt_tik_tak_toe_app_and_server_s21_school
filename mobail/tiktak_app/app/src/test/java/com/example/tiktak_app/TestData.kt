package com.example.tiktak_app

import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.model.LeaderboardPlayer
import com.example.tiktak_app.domain.model.User
import java.util.Base64

const val USER_ID = "user-1"
const val OPPONENT_ID = "user-2"

fun testJwt(
    uuid: String = USER_ID,
    expiresInSeconds: Long = 3600
): String {
    val expiration = System.currentTimeMillis() / 1000 + expiresInSeconds
    val payload = """{"uuid":"$uuid","exp":$expiration}"""
    val encoded = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(payload.toByteArray())
    return "eyJhbGciOiJIUzI1NiJ9.$encoded.signature"
}

fun emptyBoard(): List<List<Int>> = List(3) { List(3) { Game.EMPTY_CELL } }

fun testUser(
    id: String = USER_ID,
    login: String = "player"
): User = User(id = id, login = login)

fun testGame(
    id: String = "game-1",
    board: List<List<Int>> = emptyBoard(),
    mode: String = Game.MODE_HUMAN,
    status: String = Game.STATUS_IN_PROGRESS,
    currentPlayer: String? = USER_ID,
    winner: String? = null,
    playerX: String = USER_ID,
    playerO: String? = OPPONENT_ID
): Game = Game(
    id = id,
    board = board,
    mode = mode,
    status = status,
    currentPlayer = currentPlayer,
    winner = winner,
    playerX = playerX,
    playerO = playerO
)

fun testFinishedGame(
    gameId: String = "game-1",
    createdAt: Long = 100L,
    opponentLogin: String = "enemy",
    result: String = FinishedGame.RESULT_WIN
): FinishedGame = FinishedGame(
    gameId = gameId,
    createdAt = createdAt,
    opponentLogin = opponentLogin,
    result = result
)

fun testLeaderboardPlayer(
    userId: String = USER_ID,
    login: String = "player",
    winsRatio: Double = 1.5
): LeaderboardPlayer = LeaderboardPlayer(
    userId = userId,
    login = login,
    winsRatio = winsRatio
)
