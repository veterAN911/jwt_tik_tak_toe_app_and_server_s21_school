package test

import io.ktor.client.plugins.websocket.WebSockets as ClientWebSockets
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import datasource.configureDatabase
import kotlinx.serialization.json.*
import org.koin.core.context.stopKoin
import support.GameSocketClient
import web.module.configureFeatures
import web.module.configureRouting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LeaderboardTest {

    @Test
    fun testLeaderboardFlow() = testApplication {

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

        println("\n=== ТЕСТ ЛИДЕРБОРДА (WEB SOCKET) ===")

        val token1 = signupAndLogin("lb-u1")
        val token2 = signupAndLogin("lb-u2")

        val socket1 = GameSocketClient.connect(wsClient, token1)
        val socket2 = GameSocketClient.connect(wsClient, token2)

        try {
            val created = socket1.create("HUMAN")
            val gameId = created["id"]!!.jsonPrimitive.content

            socket2.join(gameId)

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

            move(socket1, 0, 0, 1)
            move(socket2, 1, 0, 2)
            move(socket1, 0, 1, 1)
            move(socket2, 1, 1, 2)
            val finished = move(socket1, 0, 2, 1)

            assertEquals("FINISHED", finished["status"]!!.jsonPrimitive.content)

            val leaderboardResponse = client.get("/leaderboard?n=100") {
                header("Authorization", "Bearer $token1")
            }

            assertEquals(HttpStatusCode.OK, leaderboardResponse.status)

            val leaderboard = Json.parseToJsonElement(leaderboardResponse.bodyAsText()).jsonArray

            val winner = leaderboard.firstOrNull {
                it.jsonObject["login"]!!.jsonPrimitive.content == "lb-u1"
            }

            assertTrue(winner != null, "Победитель должен присутствовать в лидерборде")
            assertTrue(
                winner!!.jsonObject["ratio"]!!.jsonPrimitive.double > 0.0,
                "У победителя должен быть положительный рейтинг"
            )

            println("✔ Лидерборд работает")
        } finally {
            socket1.close()
            socket2.close()
        }
    }

    @Test
    fun testBotIsNotInLeaderboard() = testApplication {

        environment { config = MapApplicationConfig() }

        application {
            stopKoin()
            configureDatabase()
            configureFeatures()
            configureRouting()
        }

        val wsClient = createClient { install(ClientWebSockets) }

        client.post("/auth/signup") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"lb-bot","password":"123456"}""")
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"login":"lb-bot","password":"123456"}""")
        }

        assertEquals(HttpStatusCode.OK, loginResponse.status)

        val token = Json.parseToJsonElement(loginResponse.bodyAsText())
            .jsonObject["accessToken"]!!.jsonPrimitive.content

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

            val leaderboardResponse = client.get("/leaderboard?n=100") {
                header("Authorization", "Bearer $token")
            }

            assertEquals(HttpStatusCode.OK, leaderboardResponse.status)

            val leaderboard = Json.parseToJsonElement(leaderboardResponse.bodyAsText()).jsonArray

            assertTrue(
                leaderboard.none {
                    it.jsonObject["userId"]!!.jsonPrimitive.content ==
                        "00000000-0000-0000-0000-000000000001"
                },
                "Бот не должен попадать в лидерборд"
            )
            assertTrue(
                leaderboard.none {
                    it.jsonObject["login"]!!.jsonPrimitive.content == "unknown"
                },
                "В лидерборде не должно быть unknown"
            )

            println("✔ Бот исключён из лидерборда")
        } finally {
            socket.close()
        }
    }
}
