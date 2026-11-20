package web.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class WebTimer(
    val type: String = "timer",
    val gameId: String,
    val status: String,
    val remaining: Long,
    val total: Long,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val winner: String? = null
)
