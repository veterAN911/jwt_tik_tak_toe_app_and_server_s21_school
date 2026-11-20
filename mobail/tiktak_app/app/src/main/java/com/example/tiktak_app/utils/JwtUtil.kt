package com.example.tiktak_app.utils

import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtUtil @Inject constructor() {

    fun isExpired(token: String, clockSkewSeconds: Long = 15): Boolean {
        val expiration = expiration(token) ?: return true
        val now = System.currentTimeMillis() / 1000
        return expiration <= now + clockSkewSeconds
    }

    fun userId(token: String): String? =
        payload(token)?.optString("uuid")?.takeIf { it.isNotBlank() }

    private fun expiration(token: String): Long? =
        payload(token)
            ?.takeIf { it.has("exp") }
            ?.optLong("exp")

    private fun payload(token: String): JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            val bytes = decodeBase64Url(parts[1]) ?: return null
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeBase64Url(value: String): ByteArray? {
        val output = ByteArray((value.length * 3) / 4)
        var buffer = 0
        var bits = 0
        var index = 0

        for (char in value) {
            if (char == '=') break

            val digit = when (char) {
                in 'A'..'Z' -> char - 'A'
                in 'a'..'z' -> char - 'a' + 26
                in '0'..'9' -> char - '0' + 52
                '-' -> 62
                '_' -> 63
                else -> return null
            }

            buffer = (buffer shl 6) or digit
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output[index++] = (buffer shr bits).toByte()
            }
        }

        return output.copyOf(index)
    }
}
