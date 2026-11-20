package support

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class GameSocketClient private constructor(
    private val session: DefaultClientWebSocketSession
) {

    companion object {
        suspend fun connect(client: HttpClient, token: String): GameSocketClient =
            GameSocketClient(client.webSocketSession("/ws/game?token=$token"))
    }

    suspend fun create(mode: String): JsonObject {
        drain()
        send(
            buildJsonObject {
                put("type", "create")
                put("mode", mode)
            }
        )
        return receiveGame()
    }

    suspend fun join(gameId: String): JsonObject {
        drain()
        send(
            buildJsonObject {
                put("type", "join")
                put("gameId", gameId)
            }
        )
        return receiveGame()
    }

    suspend fun move(gameId: String, cells: JsonArray): JsonObject {
        drain()
        send(
            buildJsonObject {
                put("type", "move")
                put("gameId", gameId)
                put("board", buildJsonObject { put("cells", cells) })
            }
        )
        return receiveGame()
    }

    suspend fun get(gameId: String): JsonObject {
        drain()
        send(
            buildJsonObject {
                put("type", "get")
                put("gameId", gameId)
            }
        )
        return receiveGame()
    }

    suspend fun close() {
        session.close()
    }

    private suspend fun receiveGame(): JsonObject {
        while (true) {
            val frame = session.incoming.receive()
            if (frame !is Frame.Text) continue

            val body = Json.parseToJsonElement(frame.readText()).jsonObject

            when (body["type"]?.jsonPrimitive?.content) {
                "game" -> return body["game"]!!.jsonObject
                "error" -> error("WS error: ${body["message"]}")
            }
        }
    }

    private suspend fun send(message: JsonObject) {
        session.send(Frame.Text(message.toString()))
    }

    private suspend fun drain() {
        while (session.incoming.tryReceive().isSuccess) {

        }
    }
}
