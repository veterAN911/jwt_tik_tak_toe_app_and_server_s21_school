package web.socket

import kotlinx.serialization.Serializable
import web.model.WebBoard
import web.model.WebGame

@Serializable
data class WsRequest(
    val type: String,
    val mode: String? = null,
    val gameId: String? = null,
    val board: WebBoard? = null
)

@Serializable
data class WsGameMessage(
    val type: String = "game",
    val game: WebGame
)

@Serializable
data class WsGamesMessage(
    val type: String = "games",
    val games: List<WebGame>
)

@Serializable
data class WsErrorMessage(
    val type: String = "error",
    val message: String
)
