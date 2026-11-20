package web.module

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import web.route.authJwtRoutes
import web.route.gameRoutes
import web.route.userRoutes
import web.socket.GameSocketHandler
import web.socket.TimerSocketHandler
import web.socket.gameSocketRoutes
import web.socket.timerSocketRoutes
import domain.service.AuthService
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val authServiceJwt by inject<AuthService>()
    val gameSocketHandler by inject<GameSocketHandler>()
    val timerSocketHandler by inject<TimerSocketHandler>()

    routing {

        authJwtRoutes(authServiceJwt)

        gameSocketRoutes(gameSocketHandler)
        timerSocketRoutes(timerSocketHandler)

        authenticate("auth-jwt") {
            userRoutes()
            gameRoutes()
        }
    }
}
