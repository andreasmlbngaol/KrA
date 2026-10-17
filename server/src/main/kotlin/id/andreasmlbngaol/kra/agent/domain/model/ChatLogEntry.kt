package id.andreasmlbngaol.kra.agent.domain.model

import java.time.Instant

/**
 * One recorded agent chat turn (observability), as read back from storage.
 *
 * "request" is the raw student message and "response" is the agent's final reply
 * (Markdown) or the user-facing error text. "steps" lists the tool/step labels that
 * were emitted while answering, in order. "status" is one of "final", "error", or
 * "cancelled" (the client closed the connection before the answer finished).
 */
data class ChatLogEntry(
    val id: String,
    val timestamp: Instant,
    val nim: String,
    val sessionId: String,
    val request: String,
    val response: String,
    val status: String,
    val streamed: Boolean,
    val durationMs: Long,
    val steps: List<String>,
    val llmCalls: Int = 0,
    val toolCalls: Int = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val totalTokens: Long = 0,
    val model: String? = null
)
