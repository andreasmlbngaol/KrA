package id.andreasmlbngaol.kra.agent.presentation.dto

import kotlinx.serialization.Serializable

/**
 * One recorded agent chat turn for the monitoring history view. [status] is one of
 * "final", "error", or "cancelled"; [streamed] is true when the turn came from the
 * streaming endpoint; [steps] lists the tool/step labels emitted while answering, in order.
 */
@Serializable
data class ChatHistoryEntry(
    val id: String,
    val timestamp: String,
    val nim: String,
    val sessionId: String,
    val request: String,
    val response: String,
    val status: String,
    val streamed: Boolean,
    val durationMs: Int,
    val steps: List<String>
)

/** A page of recent chat turns (newest first) for the monitoring history view. */
@Serializable
data class ChatHistoryResponse(
    val enabled: Boolean,
    val count: Int,
    val entries: List<ChatHistoryEntry>
)

/** A single per-minute data point for the token-usage dashboard chart. */
@Serializable
data class TokenUsagePoint(
    val minute: String,
    val llmRequests: Int,
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val toolCalls: Int,
    val avgTokensPerRequest: Int
)

/** Number of calls to a single tool within the range. */
@Serializable
data class TokenUsageToolCount(
    val name: String,
    val count: Int
)

/** Full report for one time range (e.g. the last 1/6/24 hours). */
@Serializable
data class TokenUsageReport(
    val rangeMinutes: Int,
    val enabled: Boolean,
    val fromMinute: String,
    val toMinute: String,
    val totalLlmRequests: Int,
    val totalInputTokens: Int,
    val totalOutputTokens: Int,
    val totalTokens: Int,
    val totalToolCalls: Int,
    val avgTokensPerRequest: Int,
    val toolBreakdown: List<TokenUsageToolCount>,
    val series: List<TokenUsagePoint>
)
