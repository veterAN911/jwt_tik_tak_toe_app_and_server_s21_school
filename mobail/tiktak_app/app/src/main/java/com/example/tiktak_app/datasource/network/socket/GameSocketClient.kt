package com.example.tiktak_app.datasource.network.socket

import com.example.tiktak_app.datasource.ApiConfig
import com.example.tiktak_app.datasource.TokenManager
import com.example.tiktak_app.datasource.network.dto.BoardDto
import com.example.tiktak_app.datasource.network.dto.GameDto
import com.example.tiktak_app.datasource.network.dto.WsMessageDto
import com.example.tiktak_app.datasource.network.dto.WsRequestDto
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SocketEvent {
    data class Game(val game: GameDto) : SocketEvent
    data class Error(val message: String) : SocketEvent
    data class Closed(val reason: String?) : SocketEvent
}

class GameSocket internal constructor(
    private val webSocket: WebSocket,
    val events: Flow<SocketEvent>,
    private val gson: Gson,
    private val gameId: String
) {
    fun send(request: WsRequestDto) {
        webSocket.send(gson.toJson(request))
    }

    fun makeMove(board: List<List<Int>>) {
        send(
            WsRequestDto(
                type = "move",
                gameId = gameId,
                board = BoardDto(board)
            )
        )
    }

    fun close() {
        webSocket.close(NORMAL_CLOSURE, null)
    }

    companion object {
        private const val NORMAL_CLOSURE = 1000
    }
}

@Singleton
class GameSocketClient @Inject constructor(
    private val client: OkHttpClient,
    private val tokenManager: TokenManager,
    private val gson: Gson
) {

    suspend fun createGame(mode: String): Result<GameDto> = runCatching {
        val socket = connect().getOrThrow()

        try {
            socket.send(WsRequestDto(type = "create", mode = mode))

            val event = withTimeout(TIMEOUT_MS) {
                socket.events.first { it is SocketEvent.Game || it is SocketEvent.Error }
            }

            when (event) {
                is SocketEvent.Game -> event.game
                is SocketEvent.Error -> throw IllegalStateException(event.message)
                else -> throw IllegalStateException("Сервер не вернул игру")
            }
        } finally {
            socket.close()
        }
    }

    suspend fun watchGame(gameId: String, join: Boolean): Result<GameSocket> = runCatching {
        val socket = connect(gameId).getOrThrow()
        socket.send(
            WsRequestDto(
                type = if (join) "join" else "get",
                gameId = gameId
            )
        )
        socket
    }

    private suspend fun connect(gameId: String = ""): Result<GameSocket> {
        val token = tokenManager.getValidAccessToken()
            ?: return Result.failure(IllegalStateException("Сессия истекла, войдите заново"))

        val events = MutableSharedFlow<SocketEvent>(
            replay = 8,
            extraBufferCapacity = 8
        )
        val opened = CompletableDeferred<Result<WebSocket>>()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                opened.complete(Result.success(webSocket))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = runCatching {
                    gson.fromJson(text, WsMessageDto::class.java)
                }.getOrNull() ?: return

                when (message.type) {
                    "game" -> message.game?.let { events.tryEmit(SocketEvent.Game(it)) }
                    "error" -> events.tryEmit(
                        SocketEvent.Error(message.message ?: "Ошибка игры")
                    )
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!opened.isCompleted) {
                    opened.complete(Result.failure(t))
                }
                events.tryEmit(SocketEvent.Closed(t.message))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                events.tryEmit(SocketEvent.Closed(reason))
            }
        }

        val request = Request.Builder()
            .url("${ApiConfig.WS_URL}?token=$token")
            .build()

        client.newWebSocket(request, listener)

        return try {
            withTimeout(TIMEOUT_MS) { opened.await() }
                .map { webSocket ->
                    GameSocket(
                        webSocket = webSocket,
                        events = events.asSharedFlow(),
                        gson = gson,
                        gameId = gameId
                    )
                }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TIMEOUT_MS = 10_000L
    }
}
