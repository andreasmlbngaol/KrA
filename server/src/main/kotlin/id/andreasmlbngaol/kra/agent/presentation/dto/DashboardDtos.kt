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
    val steps: List<String>,
    // No defaults: Jackson 3 omits properties equal to their Kotlin default, which made
    // a zero tool-call turn arrive as `undefined` in the browser and render as NaN.
    val llmCalls: Int,
    val toolCalls: Int,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val model: String? = null
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


/** Aggregate solver statistics (empirical CSP cost) for the monitoring page. */
@Serializable
data class CspSummaryDto(
    val totalSolves: Long,
    val feasibleCount: Long,
    val infeasibleCount: Long,
    val avgDurationMs: Double,
    val avgNodes: Double,
    val avgBacktracks: Double,
    val avgAc3Revisions: Double,
    val avgVariables: Double,
    val avgDomainValues: Double
)

/** One solver run, newest first. */
@Serializable
data class CspSolveEntryDto(
    val id: String,
    val timestamp: String,
    val nim: String,
    val semester: Int,
    val status: String,
    val objective: String,
    val variables: Int,
    val constraints: Int,
    val domainValues: Int,
    val targetSks: Int,
    val resultSks: Int,
    val nodes: Int,
    val backtracks: Int,
    val ac3Revisions: Int,
    val forwardChecks: Int,
    val durationMs: Long,
    val alternatives: Int,
    val mbkmMode: Boolean,
    val chatId: String? = null
)

@Serializable
data class CspStatsResponse(
    val enabled: Boolean,
    val summary: CspSummaryDto,
    val recent: List<CspSolveEntryDto>
)

/** One persisted tool invocation, for the monitoring drill-down. */
@Serializable
data class ToolCallEntryDto(
    val chatId: String,
    val nim: String,
    val sessionId: String,
    val seq: Int,
    val toolName: String,
    val arguments: String,
    val status: String,
    val durationMs: Long,
    val resultSize: Int,
    val timestamp: String
)

/** Newest tool invocations across all users. */
@Serializable
data class ToolCallListResponse(
    val enabled: Boolean,
    val count: Int,
    val entries: List<ToolCallEntryDto>
)

/** Per-tool usage with error rate and average duration. */
@Serializable
data class ToolUsageEntryDto(
    val toolName: String,
    val calls: Long,
    val errors: Long,
    val avgDurationMs: Double
)

@Serializable
data class ToolUsageResponse(
    val enabled: Boolean,
    val count: Int,
    val tools: List<ToolUsageEntryDto>
)
