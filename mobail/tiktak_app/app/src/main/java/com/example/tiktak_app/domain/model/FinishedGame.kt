package com.example.tiktak_app.domain.model

data class FinishedGame(
    val gameId: String,
    val createdAt: Long,
    val opponentLogin: String,
    val result: String
) {
    companion object {
        const val RESULT_WIN = "WIN"
        const val RESULT_LOSS = "LOSS"
        const val RESULT_DRAW = "DRAW"
    }
}
