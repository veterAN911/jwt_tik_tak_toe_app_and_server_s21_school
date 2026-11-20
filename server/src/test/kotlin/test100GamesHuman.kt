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

class GameHumanMassTest {

    @Test
    fun test100GamesHumanVsHuman() = testApplication {

        environment { config = MapApplicationConfig() }

        application {
            stopKoin()
            configureFeatures()
            configureRouting()
        }

        val wsClient = createClient { install(ClientWebSockets) }

        client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"p1","password":"123"}""")
        }
        client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"p2","password":"123"}""")
        }

        val loginP1 = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"p1","password":"123"}""")
        }
        val loginP2 = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"p2","password":"123"}""")
        }

        assertEquals(HttpStatusCode.OK, loginP1.status)
        assertEquals(HttpStatusCode.OK, loginP2.status)

        val tokenP1 = Json.parseToJsonElement(loginP1.bodyAsText())
            .jsonObject["accessToken"]!!.jsonPrimitive.content

        val tokenP2 = Json.parseToJsonElement(loginP2.bodyAsText())
            .jsonObject["accessToken"]!!.jsonPrimitive.content

        val socket1 = GameSocketClient.connect(wsClient, tokenP1)
        val socket2 = GameSocketClient.connect(wsClient, tokenP2)

        var winsX = 0
        var winsO = 0
        var draws = 0

        try {
            repeat(100) { gameIndex ->

                println("\n=== HUMAN GAME #${gameIndex + 1} START ===")

                val created = socket1.create("HUMAN")
                val gameId = created["id"]!!.jsonPrimitive.content

                val joined = socket2.join(gameId)

                val playerX = joined["playerX"]!!.jsonPrimitive.content
                val playerO = joined["playerO"]!!.jsonPrimitive.content

                var board = joined["board"]!!.jsonObject["cells"]!!.jsonArray
                var status = joined["status"]!!.jsonPrimitive.content
                var currentPlayer = joined["currentPlayer"]!!.jsonPrimitive.content

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
                    val sign = if (currentPlayer == playerX) 1 else 2
                    val socket = if (currentPlayer == playerX) socket1 else socket2

                    println("Player $currentPlayer moves to [$x,$y]")

                    val newBoard = buildJsonArray {
                        for (i in 0 until 3) {
                            add(
                                buildJsonArray {
                                    for (j in 0 until 3) {
                                        add(
                                            if (i == x && j == y) sign
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
                    currentPlayer = after["currentPlayer"]?.jsonPrimitive?.content ?: ""
                }

                println("Human Game #${gameIndex + 1} finished with status: $status")

                val finalJson = socket1.get(gameId)
                val winner = finalJson["winner"]?.jsonPrimitive?.content

                when {
                    status == "DRAW" -> draws++
                    winner == playerX -> winsX++
                    winner == playerO -> winsO++
                }
            }
        } finally {
            socket1.close()
            socket2.close()
        }

        println("\n=== RESULTS OF 100 HUMAN VS HUMAN GAMES ===")
        println("X wins: $winsX")
        println("O wins: $winsO")
        println("Draws:  $draws")
    }
}
