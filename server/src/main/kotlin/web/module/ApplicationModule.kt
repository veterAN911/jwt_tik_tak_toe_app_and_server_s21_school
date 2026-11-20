package web.module

import di.appModule
import domain.service.AuthService
import domain.service.GameService
import domain.service.GameTimeoutMonitor
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.websocket.WebSockets
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import java.util.*
import domain.service.JwtProvider

fun Application.configureFeatures() {

    install(Koin) {
        modules(appModule)
    }

    GameTimeoutMonitor.start(getKoin().get<GameService>())

    install(ContentNegotiation) {
        json()
    }

    install(WebSockets) {
        pingPeriodMillis = 30_000
        timeoutMillis = 60_000
    }

    configureAuth()
}

fun Application.configureAuth() {
    val jwtProvider by inject<JwtProvider>()

    install(Authentication) {
        bearer("auth-jwt") {
            authenticate { tokenCredential ->

                val token = tokenCredential.token

                if (!jwtProvider.validateAccessToken(token)) {
                    return@authenticate null
                }

                val claims = jwtProvider.getClaimsFromAccess(token)
                val uuid = claims["uuid"]?.toString() ?: return@authenticate null

                UserIdPrincipal(uuid)
            }
        }
    }
}
