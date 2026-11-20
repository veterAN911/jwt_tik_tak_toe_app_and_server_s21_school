package com.example.tiktak_app.presentation.model

import com.example.tiktak_app.domain.model.Game

data class GameViewData(
    val id: String,
    val board: List<List<Int>>,
    val mode: String,
    val status: String,
    val currentPlayer: String?,
    val winner: String?,
    val playerX: String,
    val playerO: String?
) {
    val isFinished: Boolean
        get() = status == Game.STATUS_FINISHED || status == Game.STATUS_DRAW

    fun cell(row: Int, col: Int): Int =
        board.getOrNull(row)?.getOrNull(col) ?: Game.EMPTY_CELL

    fun isCellEmpty(row: Int, col: Int): Boolean = cell(row, col) == Game.EMPTY_CELL

    fun isMyTurn(userId: String?): Boolean =
        userId != null &&
            status == Game.STATUS_IN_PROGRESS &&
            !isFinished &&
            currentPlayer == userId

    fun symbolAt(row: Int, col: Int): String = when (cell(row, col)) {
        Game.SYMBOL_X -> "X"
        Game.SYMBOL_O -> "O"
        else -> ""
    }

    fun playerLabel(userId: String?, currentUserId: String?): String = when {
        userId == null -> "Ожидание игрока"
        userId == currentUserId -> "Вы"
        userId == Game.BOT_ID -> "Компьютер"
        else -> "Соперник"
    }

    fun statusText(currentUserId: String?): String = when {
        status == Game.STATUS_WAITING -> "Ожидание игроков"
        status == Game.STATUS_DRAW -> "Ничья"
        isFinished && winner == currentUserId -> "Победа"
        isFinished -> "Поражение"
        isMyTurn(currentUserId) -> "Ваш ход"
        else -> "Ходит соперник"
    }
}
