package id.andreasmlbngaol.kra.core.error

import org.springframework.http.HttpStatus

/**
 * Safe, user-friendly messages to display to the user.
 * Technical details (stack traces, provider error bodies, etc.) must NOT appear here -
 * they are only recorded in the server log.
 */
object ErrorMessages {
    const val GENERIC =
        "Maaf, terjadi kesalahan di server. Silakan coba lagi beberapa saat lagi."
    const val AI_UNAVAILABLE =
        "Asisten AI sedang sibuk atau tidak tersedia untuk sementara. Silakan coba lagi beberapa saat lagi."
    const val AI_TIMEOUT =
        "Asisten AI terlalu lama merespons. Silakan coba lagi."
    const val BAD_REQUEST =
        "Permintaan tidak valid."
}

/**
 * Detects whether an agent reply is actually a raw error dump from the LLM provider
 * (e.g. Koog/GoogleLLMClient) that should NOT reach the user.
 *
 * Some providers return an error as the reply text (instead of throwing), so it must be
 * detected explicitly. The markers are deliberately strict to avoid flagging a normal reply.
 */
fun looksLikeRawProviderError(text: String): Boolean {
    val t = text.trim()
    if (t.isEmpty()) return false

    val strongMarkers = listOf(
        "Error from client",
        "LLMClient",
        "\"status\": \"UNAVAILABLE\"",
        "\"status\":\"UNAVAILABLE\"",
    )
    if (strongMarkers.any { t.contains(it) }) return true

    // Typical combination found in an HTTP error dump from a provider.
    return t.contains("Status code:") && t.contains("Error body:")
}

/**
 * Maps any throwable to an (HTTP status, friendly message) pair for the user.
 * Never leaks the original exception message to the client.
 */
fun friendlyError(cause: Throwable): Pair<HttpStatus, String> {
    val raw = buildString {
        append(cause.message ?: "")
        append(' ')
        append(cause.cause?.message ?: "")
    }.lowercase()

    return when {
        raw.contains("503") || raw.contains("unavailable") || raw.contains("overload") ||
            raw.contains("high demand") || raw.contains("rate limit") || raw.contains("429") ->
            HttpStatus.SERVICE_UNAVAILABLE to ErrorMessages.AI_UNAVAILABLE

        raw.contains("timeout") || raw.contains("timed out") ->
            HttpStatus.GATEWAY_TIMEOUT to ErrorMessages.AI_TIMEOUT

        else ->
            HttpStatus.INTERNAL_SERVER_ERROR to ErrorMessages.GENERIC
    }
}
