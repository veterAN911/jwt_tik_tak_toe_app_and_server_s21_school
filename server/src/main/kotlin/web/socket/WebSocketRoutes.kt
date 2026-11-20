package web.socket

import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket

fun Route.gameSocketRoutes(handler: GameSocketHandler) {
    webSocket("/ws/game") {
        handler.handle(this)
    }
}

fun Route.timerSocketRoutes(handler: TimerSocketHandler) {
    webSocket("/ws/timer") {
        handler.handle(this)
    }
}
