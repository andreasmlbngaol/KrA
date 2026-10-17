package id.andreasmlbngaol.kra.agent.domain.model

import java.time.Instant

/** Persisted record of one tool invocation (evaluation input for tool accuracy). */
data class AgentToolCallLog(
    val chatId: String,
    val nim: String,
    val sessionId: String,
    val seq: Int,
    val toolName: String,
    val arguments: String,
    val status: ToolCallStatus,
    val durationMs: Long,
    val resultSize: Int,
    val timestamp: Instant
)

/** Aggregated usage of one tool, for the monitoring page. */
data class ToolUsageStat(
    val toolName: String,
    val calls: Long,
    val errors: Long,
    val avgDurationMs: Double
)
