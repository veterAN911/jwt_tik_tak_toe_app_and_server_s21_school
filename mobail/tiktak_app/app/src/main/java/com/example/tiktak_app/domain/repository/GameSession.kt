package com.example.tiktak_app.domain.repository

import com.example.tiktak_app.domain.model.Game
import kotlinx.coroutines.flow.Flow

interface GameSession {
    val events: Flow<GameEvent>
    fun makeMove(board: List<List<Int>>)
    fun close()
}

sealed interface GameEvent {
    data class GameUpdated(val game: Game) : GameEvent
    data class Error(val message: String) : GameEvent
}
