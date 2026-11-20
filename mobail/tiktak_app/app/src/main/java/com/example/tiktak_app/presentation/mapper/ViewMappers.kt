package com.example.tiktak_app.presentation.mapper

import com.example.tiktak_app.domain.model.FinishedGame
import com.example.tiktak_app.domain.model.Game
import com.example.tiktak_app.domain.model.LeaderboardPlayer
import com.example.tiktak_app.domain.model.User
import com.example.tiktak_app.presentation.model.GameViewData
import com.example.tiktak_app.presentation.model.HistoryItemViewData
import com.example.tiktak_app.presentation.model.LeaderboardItemViewData
import com.example.tiktak_app.presentation.model.UserViewData
import java.util.Locale

fun User.toViewData() = UserViewData(
    id = id,
    login = login
)

fun Game.toViewData() = GameViewData(
    id = id,
    board = board,
    mode = mode,
    status = status,
    currentPlayer = currentPlayer,
    winner = winner,
    playerX = playerX,
    playerO = playerO
)

fun FinishedGame.toViewData() = HistoryItemViewData(
    gameId = gameId,
    opponentLogin = opponentLogin,
    result = result,
    resultText = when (result) {
        FinishedGame.RESULT_WIN -> "Победа"
        FinishedGame.RESULT_LOSS -> "Поражение"
        FinishedGame.RESULT_DRAW -> "Ничья"
        else -> result
    }
)

fun LeaderboardPlayer.toViewData(position: Int) = LeaderboardItemViewData(
    position = position,
    login = login,
    winsRatio = String.format(Locale.getDefault(), "%.2f", winsRatio)
)
