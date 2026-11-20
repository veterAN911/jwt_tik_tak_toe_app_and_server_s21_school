package domain.model

import java.util.UUID

data class GameTimer(
    val gameId: UUID,
    val status: GameStatus,
    val remaining: Long,
    val total: Long
)
