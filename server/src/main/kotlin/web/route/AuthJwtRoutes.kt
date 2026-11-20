package web.route

import domain.model.AuthTokens
import domain.service.AuthService
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import web.model.JwtRequest
import web.model.JwtResponse
import web.model.RefreshJwtRequest

fun Route.authJwtRoutes(authService: AuthService) {

    post("/auth/login") {
        val req = call.receive<JwtRequest>()
        val tokens = authService.authorize(req.login, req.password)
            ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Неверный логин или пароль")
            )

        call.respond(tokens.toWeb())
    }

    post("/auth/refresh/access") {
        val req = call.receive<RefreshJwtRequest>()
        val tokens = authService.refreshAccessToken(req.refreshToken)
            ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Сессия истекла, войдите заново")
            )

        call.respond(tokens.toWeb())
    }

    post("/auth/refresh/refresh") {
        val req = call.receive<RefreshJwtRequest>()
        val tokens = authService.refreshRefreshToken(req.refreshToken)
            ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Сессия истекла, войдите заново")
            )

        call.respond(tokens.toWeb())
    }

    post("/auth/signup") {
        val req = call.receive<JwtRequest>()

        if (req.login.isBlank() || req.password.isBlank()) {
            return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Введите логин и пароль")
            )
        }

        val ok = authService.register(req.login, req.password)
        if (!ok) {
            return@post call.respond(
                HttpStatusCode.Conflict,
                mapOf("error" to "Пользователь с таким логином уже существует")
            )
        }

        call.respond(HttpStatusCode.OK)
    }

}

private fun AuthTokens.toWeb(): JwtResponse =
    JwtResponse(
        accessToken = accessToken,
        refreshToken = refreshToken
    )
