package id.andreasmlbngaol.kra.agent.domain.model

import java.time.Instant

/** Aggregated agent token-usage metrics for a single minute (UTC). */
data class TokenUsageMinute(
    val minute: Instant,
    val llmRequests: Long,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val toolCalls: Long,
    val toolBreakdown: Map<String, Long>
)
