package com.example.tiktak_app.domain.model

data class Game(
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
        get() = status == STATUS_FINISHED || status == STATUS_DRAW

    val isBotGame: Boolean
        get() = mode == MODE_BOT

    fun cell(row: Int, col: Int): Int =
        board.getOrNull(row)?.getOrNull(col) ?: EMPTY_CELL

    fun isCellEmpty(row: Int, col: Int): Boolean = cell(row, col) == EMPTY_CELL

    fun isMyTurn(userId: String?): Boolean =
        userId != null &&
            status == STATUS_IN_PROGRESS &&
            !isFinished &&
            currentPlayer == userId

    fun symbolOf(userId: String?): Int = when (userId) {
        playerX -> SYMBOL_X
        playerO -> SYMBOL_O
        else -> EMPTY_CELL
    }

    companion object {
        const val BOT_ID = "00000000-0000-0000-0000-000000000001"

        const val STATUS_WAITING = "WAITING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_FINISHED = "FINISHED"
        const val STATUS_DRAW = "DRAW"

        const val MODE_BOT = "BOT"
        const val MODE_HUMAN = "HUMAN"

        const val EMPTY_CELL = 0
        const val SYMBOL_X = 1
        const val SYMBOL_O = 2
    }
}
