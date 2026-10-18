package id.andreasmlbngaol.kra.agent.data.session

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Maps a short-lived opaque token to the authenticated student's NIM.
 *
 * The LLM only ever sees the token: it is injected into the system context once per chat
 * turn and passed back as a tool argument, so the NIM itself never enters the prompt or a
 * tool result. Tokens are random (24 bytes, base64url), bound to one chat session, and
 * expire after [ttl] so a leaked value stops being useful.
 *
 * In-memory is intentional: the prototype runs as a single instance. A multi-instance
 * deployment would need a shared store (Redis) instead.
 */
@Component
class SessionTokenRegistry(
    private val ttl: Duration = Duration.ofHours(2)
) {

    private data class Entry(val nim: String, val sessionKey: String, val expiresAt: Instant)

    private val byToken = ConcurrentHashMap<String, Entry>()
    private val bySession = ConcurrentHashMap<String, String>()
    private val random = SecureRandom()

    /** Returns the session's existing token when still valid, otherwise issues a new one. */
    fun issue(nim: String, sessionId: String): String {
        val sessionKey = "$nim|$sessionId"
        val now = Instant.now()
        bySession[sessionKey]?.let { token ->
            val entry = byToken[token]
            if (entry != null && entry.nim == nim && entry.expiresAt.isAfter(now)) return token
            bySession.remove(sessionKey, token)
        }
        val token = newToken()
        byToken[token] = Entry(nim, sessionKey, now.plus(ttl))
        bySession[sessionKey] = token
        purgeExpired(now)
        return token
    }

    /** Resolves a token to a NIM, or null when it is unknown or expired. */
    fun resolve(token: String): String? {
        val entry = byToken[token] ?: return null
        if (!entry.expiresAt.isAfter(Instant.now())) {
            byToken.remove(token)
            bySession.remove(entry.sessionKey, token)
            return null
        }
        return entry.nim
    }

    private fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun purgeExpired(now: Instant) {
        byToken.entries.removeIf { !it.value.expiresAt.isAfter(now) }
        bySession.entries.removeIf { byToken[it.value] == null }
    }

    private companion object {
        const val TOKEN_BYTES = 24
    }
}
