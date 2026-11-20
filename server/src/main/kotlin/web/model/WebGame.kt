package web.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class WebGame(
    val id: String,
    val board: WebBoard,
    val mode: String,
    val status: String,
    val currentPlayer: String?,
    val winner: String?,
    val playerX: String,
    val playerO: String?,
    val createdAt: Long,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val botDifficulty: String? = null
)
