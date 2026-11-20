package web.socket

import domain.service.JwtProvider
import io.ktor.server.websocket.WebSocketServerSession
import java.util.UUID

internal fun WebSocketServerSession.userIdOrNull(jwtProvider: JwtProvider): UUID? {
    val token = call.request.queryParameters["token"] ?: return null
    if (!jwtProvider.validateAccessToken(token)) return null

    val rawUuid = jwtProvider.getClaimsFromAccess(token)["uuid"]?.toString() ?: return null
    return runCatching { UUID.fromString(rawUuid) }.getOrNull()
}

internal fun parseGameId(raw: String?): UUID? =
    raw?.let { runCatching { UUID.fromString(it) }.getOrNull() }
