package test

import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import datasource.configureDatabase
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.koin.core.context.stopKoin
import support.GameSocketClient
import web.module.configureFeatures
import web.module.configureRouting

class FinishedGamesTest {

    @Test
    fun testFinishedGamesFlow() = testApplication {

        environment { config = MapApplicationConfig() }

        application {
            stopKoin()
            configureDatabase()
            configureFeatures()
            configureRouting()
        }

        val wsClient = createClient { install(ClientWebSockets) }

        suspend fun signupAndLogin(login: String): String {
            client.post("/auth/signup") {
                contentType(ContentType.Application.Json)
                setBody("""{"login":"$login","password":"123456"}""")
            }

            val loginResponse = client.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"login":"$login","password":"123456"}""")
            }

            assertEquals(HttpStatusCode.OK, loginResponse.status)

            return Json.parseToJsonElement(loginResponse.bodyAsText())
                .jsonObject["accessToken"]!!.jsonPrimitive.content
        }

        println("\n=== ТЕСТ ИСТОРИИ ЗАВЕРШЁННЫХ ИГР (WEB SOCKET) ===")

        val tokenX = signupAndLogin("history-x")
        val tokenO = signupAndLogin("history-o")

        val socketX = GameSocketClient.connect(wsClient, tokenX)
        val socketO = GameSocketClient.connect(wsClient, tokenO)

        try {
            val created = socketX.create("HUMAN")
            val gameId = created["id"]!!.jsonPrimitive.content

            socketO.join(gameId)

            val cells = MutableList(3) { MutableList(3) { 0 } }

            suspend fun move(socket: GameSocketClient, x: Int, y: Int, sign: Int): JsonObject {
                cells[x][y] = sign

                val jsonCells = buildJsonArray {
                    cells.forEach { row ->
                        add(buildJsonArray { row.forEach { add(it) } })
                    }
                }

                return socket.move(gameId, jsonCells)
            }

            move(socketX, 0, 0, 1)
            move(socketO, 1, 0, 2)
            move(socketX, 0, 1, 1)
            move(socketO, 1, 1, 2)
            val finished = move(socketX, 0, 2, 1)

            assertEquals("FINISHED", finished["status"]!!.jsonPrimitive.content)
            assertEquals(
                created["playerX"]!!.jsonPrimitive.content,
                finished["winner"]!!.jsonPrimitive.content
            )

            val historyResponse = client.get("/user/games/finished") {
                header("Authorization", "Bearer $tokenX")
            }

            assertEquals(HttpStatusCode.OK, historyResponse.status)

            val history = Json.parseToJsonElement(historyResponse.bodyAsText()).jsonArray
            val item = history.firstOrNull {
                it.jsonObject["gameId"]!!.jsonPrimitive.content == gameId
            }

            assertTrue(item != null, "Завершённая игра должна попасть в историю")
            assertEquals("WIN", item!!.jsonObject["result"]!!.jsonPrimitive.content)

            println("✔ История завершённых игр работает")
        } finally {
            socketX.close()
            socketO.close()
        }
    }

    @Test
    fun testBotGameAppearsInHistory() = testApplication {

        environment { config = MapApplicationConfig() }

        application {
            stopKoin()
            configureDatabase()
            configureFeatures()
            configureRouting()
        }

        val wsClient = createClient { install(ClientWebSockets) }

        suspend fun signupAndLogin(login: String): String {
            client.post("/auth/signup") {
                contentType(ContentType.Application.Json)
                setBody("""{"login":"$login","password":"123456"}""")
            }

            val loginResponse = client.post("/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("""{"login":"$login","password":"123456"}""")
            }

            assertEquals(HttpStatusCode.OK, loginResponse.status)

            return Json.parseToJsonElement(loginResponse.bodyAsText())
                .jsonObject["accessToken"]!!.jsonPrimitive.content
        }

        val token = signupAndLogin("history-bot")
        val socket = GameSocketClient.connect(wsClient, token)

        try {
            var game = socket.create("BOT")
            val gameId = game["id"]!!.jsonPrimitive.content

            var moves = 0
            while (game["status"]!!.jsonPrimitive.content == "IN_PROGRESS" && moves < 9) {
                val cells = game["board"]!!.jsonObject["cells"]!!.jsonArray
                var target: Pair<Int, Int>? = null

                for (row in 0..2) {
                    for (col in 0..2) {
                        if (cells[row].jsonArray[col].jsonPrimitive.int == 0) {
                            target = row to col
                            break
                        }
                    }
                    if (target != null) break
                }

                val (row, col) = target ?: break
                val board = MutableList(3) { r ->
                    MutableList(3) { c -> cells[r].jsonArray[c].jsonPrimitive.int }
                }
                board[row][col] = 1

                val jsonCells = buildJsonArray {
                    board.forEach { values ->
                        add(buildJsonArray { values.forEach { add(it) } })
                    }
                }

                game = socket.move(gameId, jsonCells)
                moves++
            }

            val status = game["status"]!!.jsonPrimitive.content
            assertTrue(
                status == "FINISHED" || status == "DRAW",
                "Игра с ботом должна завершиться"
            )

            val historyResponse = client.get("/user/games/finished") {
                header("Authorization", "Bearer $token")
            }

            assertEquals(HttpStatusCode.OK, historyResponse.status)

            val history = Json.parseToJsonElement(historyResponse.bodyAsText()).jsonArray
            val item = history.firstOrNull {
                it.jsonObject["gameId"]!!.jsonPrimitive.content == gameId
            }

            assertTrue(item != null, "Игра с ботом должна попасть в историю")
            assertEquals("Бот", item!!.jsonObject["opponentLogin"]!!.jsonPrimitive.content)

            println("✔ История игр с ботом работает")
        } finally {
            socket.close()
        }
    }
}
