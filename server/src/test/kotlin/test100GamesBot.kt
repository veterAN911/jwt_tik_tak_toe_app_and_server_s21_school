package test

import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import org.koin.core.context.stopKoin
import support.GameSocketClient
import web.module.configureFeatures
import web.module.configureRouting

class GameBotMassTest {

    @Test
    fun test100GamesBot() = testApplication {

        environment { config = MapApplicationConfig() }

        application {
            stopKoin()
            configureFeatures()
            configureRouting()
        }

        val wsClient = createClient { install(ClientWebSockets) }

        client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"test","password":"123"}""")
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"test","password":"123"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val tokensJson = Json.parseToJsonElement(loginResponse.bodyAsText()).jsonObject
        val accessToken = tokensJson["accessToken"]!!.jsonPrimitive.content

        val socket = GameSocketClient.connect(wsClient, accessToken)

        var winsX = 0
        var winsO = 0
        var draws = 0

        try {
            repeat(1000) { gameIndex ->

                val gameJson = socket.create("BOT")
                val gameId = gameJson["id"]!!.jsonPrimitive.content

                var status = gameJson["status"]!!.jsonPrimitive.content
                var board = gameJson["board"]!!.jsonObject["cells"]!!.jsonArray

                while (status == "IN_PROGRESS") {

                    val empty = mutableListOf<Pair<Int, Int>>()

                    for (i in 0 until 3) {
                        for (j in 0 until 3) {
                            if (board[i].jsonArray[j].jsonPrimitive.int == 0) {
                                empty += i to j
                            }
                        }
                    }

                    if (empty.isEmpty()) break

                    val (x, y) = empty.random()

                    val newBoard = buildJsonArray {
                        for (i in 0 until 3) {
                            add(
                                buildJsonArray {
                                    for (j in 0 until 3) {
                                        add(
                                            if (i == x && j == y) 1
                                            else board[i].jsonArray[j].jsonPrimitive.int
                                        )
                                    }
                                }
                            )
                        }
                    }

                    val after = socket.move(gameId, newBoard)
                    board = after["board"]!!.jsonObject["cells"]!!.jsonArray
                    status = after["status"]!!.jsonPrimitive.content
                }

                val finalJson = socket.get(gameId)
                val winner = finalJson["winner"]?.jsonPrimitive?.content

                when (status) {
                    "DRAW" -> {
                        draws++
                        println("Game #${gameIndex + 1}: DRAW")
                    }

                    "FINISHED" -> {
                        if (winner != null) {
                            val px = finalJson["playerX"]!!.jsonPrimitive.content

                            if (winner == px) {
                                winsX++
                                println("Game #${gameIndex + 1}: WIN_X (winning combo)")
                            } else {
                                winsO++
                                println("Game #${gameIndex + 1}: WIN_O (winning combo)")
                            }
                        } else {
                            println("Game #${gameIndex + 1}: FINISHED (no winner?)")
                        }
                    }

                    else -> println("Game #${gameIndex + 1}: unexpected status = $status")
                }
            }
        } finally {
            socket.close()
        }

        println("\n=== RESULTS OF 1000 GAMES ===")
        println("X wins: $winsX")
        println("O wins: $winsO")
        println("Draws:  $draws")
    }
}
