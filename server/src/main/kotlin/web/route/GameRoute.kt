package web.route

import domain.service.UserService
import domain.service.GameService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import web.mapper.toWeb

fun Route.gameRoutes() {

    val gameService by inject<GameService>()
    val userService by inject<UserService>()

    get("/game/waiting") {
        val waiting = gameService.getWaitingGames()
        call.respond(waiting.map { it.toWeb() })
    }

    get("/leaderboard") {
        @Serializable
        data class LeaderboardItem(
            val userId: String,
            val login: String,
            val ratio: Double
        )

        val nParam = call.request.queryParameters["n"] ?: "10"
        val n = nParam.toIntOrNull() ?: return@get call.respond(
            HttpStatusCode.BadRequest,
            mapOf("error" to "Некорректное значение n")
        )

        val top = gameService.getTopPlayers(n)

        val result = top.map { entry ->
            val user = userService.getUserById(entry.userId)
            LeaderboardItem(
                userId = entry.userId.toString(),
                login = user?.login ?: "unknown",
                ratio = entry.winsRatio
            )
        }

        call.respond(result)
    }
}
