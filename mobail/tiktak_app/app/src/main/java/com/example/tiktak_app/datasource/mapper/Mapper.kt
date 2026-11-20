package com.example.tiktak_app.datasource.mapper

import com.example.tiktak_app.datasource.database.entity.HistoryEntity
import com.example.tiktak_app.datasource.database.entity.LeaderboardEntity
import com.example.tiktak_app.datasource.database.entity.TokenEntity
import com.example.tiktak_app.datasource.database.entity.UserEntity
import com.example.tiktak_app.datasource.network.dto.FinishedGameDto
import com.example.tiktak_app.datasource.network.dto.GameDto
import com.example.tiktak_app.datasource.network.dto.LeaderboardDto
import com.example.tiktak_app.datasource.network.dto.TokenResponseDto
import com.example.tiktak_app.datasource.network.dto.UserDto
import com.example.tiktak_app.domain.model.AuthTokens
import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.model.LeaderboardPlayer
import com.example.tiktak_app.domain.model.User

fun TokenResponseDto.toDomain() = AuthTokens(
    accessToken = accessToken,
    refreshToken = refreshToken
)

fun TokenEntity.toDomain() = AuthTokens(
    accessToken = accessToken,
    refreshToken = refreshToken
)

fun UserDto.toDomain() = User(id = id, login = login)

fun UserEntity.toDomain() = User(id = id, login = login)

fun User.toEntity() = UserEntity(id = id, login = login)

fun FinishedGameDto.toDomain() = FinishedGame(
    gameId = gameId,
    createdAt = createdAt,
    opponentLogin = opponentLogin,
    result = result
)

fun FinishedGame.toEntity() = HistoryEntity(
    gameId = gameId,
    createdAt = createdAt,
    opponentLogin = opponentLogin,
    result = result
)

fun HistoryEntity.toDomain() = FinishedGame(
    gameId = gameId,
    createdAt = createdAt,
    opponentLogin = opponentLogin,
    result = result
)

fun LeaderboardDto.toDomain() = LeaderboardPlayer(
    userId = userId,
    login = login,
    winsRatio = ratio
)

fun LeaderboardEntity.toDomain() = LeaderboardPlayer(
    userId = userId,
    login = login,
    winsRatio = ratio
)

fun GameDto.toDomain() = Game(
    id = id,
    board = board.cells,
    mode = mode,
    status = status,
    currentPlayer = currentPlayer,
    winner = winner,
    playerX = playerX,
    playerO = playerO
)
