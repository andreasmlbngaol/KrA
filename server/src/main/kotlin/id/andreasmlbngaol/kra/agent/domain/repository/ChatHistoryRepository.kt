package id.andreasmlbngaol.kra.agent.domain.repository

import id.andreasmlbngaol.kra.agent.domain.model.ChatLogEntry
import id.andreasmlbngaol.kra.agent.domain.model.TurnMetrics

/**
 * Storage for full agent chat turns (observability): every request/response pair,
 * who sent it (NIM), the session, the streamed step labels, and timing.
 *
 * Every operation must be safe when the backend is unavailable: writes become no-ops
 * and reads return empty.
 */
interface ChatHistoryRepository {
    /** true when the storage backend is ready to use. */
    val isEnabled: Boolean

    /**
     * Persists one chat turn. Implementations write asynchronously and must never block
     * or throw into the caller (the request path), even when storage fails.
     */
    fun record(
        nim: String,
        sessionId: String,
        request: String,
        response: String,
        status: String,
        streamed: Boolean,
        durationMs: Long,
        steps: List<String>,
        metrics: TurnMetrics,
        model: String?
    ): String

    /** Returns the most recent [limit] turns across all users, newest first. */
    fun queryRecent(limit: Int): List<ChatLogEntry>
}
