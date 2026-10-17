package id.andreasmlbngaol.kra.agent.presentation

import id.andreasmlbngaol.kra.academic.domain.repository.CspSolveLogRepository
import id.andreasmlbngaol.kra.agent.domain.repository.ChatHistoryRepository
import id.andreasmlbngaol.kra.agent.domain.repository.ToolCallLogRepository
import id.andreasmlbngaol.kra.agent.domain.repository.TokenUsageRepository
import id.andreasmlbngaol.kra.agent.presentation.dto.ChatHistoryEntry
import id.andreasmlbngaol.kra.agent.presentation.dto.ChatHistoryResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.CspSolveEntryDto
import id.andreasmlbngaol.kra.agent.presentation.dto.CspStatsResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.CspSummaryDto
import id.andreasmlbngaol.kra.agent.presentation.dto.ToolCallEntryDto
import id.andreasmlbngaol.kra.agent.presentation.dto.ToolCallListResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.ToolUsageEntryDto
import id.andreasmlbngaol.kra.agent.presentation.dto.ToolUsageResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.TokenUsagePoint
import id.andreasmlbngaol.kra.agent.presentation.dto.TokenUsageReport
import id.andreasmlbngaol.kra.agent.presentation.dto.TokenUsageToolCount
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Observability endpoints (INTENTIONALLY WITHOUT AUTH for now - used internally):
 * recent chat history and a per-minute token-usage series plus aggregate summary.
 */
@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val tokenUsageRepository: TokenUsageRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val cspSolveLogRepository: CspSolveLogRepository,
    private val toolCallLogRepository: ToolCallLogRepository
) {

    /** Most recent chat turns across ALL users (newest first). */
    @GetMapping("/chat-history")
    fun chatHistory(@RequestParam(defaultValue = "100") limit: Int): ChatHistoryResponse {
        val capped = limit.coerceIn(1, 500)
        val entries = chatHistoryRepository.queryRecent(capped)
        return ChatHistoryResponse(
            enabled = chatHistoryRepository.isEnabled,
            count = entries.size,
            entries = entries.map { e ->
                ChatHistoryEntry(
                    id = e.id,
                    timestamp = e.timestamp.toString(),
                    nim = e.nim,
                    sessionId = e.sessionId,
                    request = e.request,
                    response = e.response,
                    status = e.status,
                    streamed = e.streamed,
                    durationMs = e.durationMs.toInt(),
                    steps = e.steps,
                    llmCalls = e.llmCalls,
                    toolCalls = e.toolCalls,
                    inputTokens = e.inputTokens,
                    outputTokens = e.outputTokens,
                    totalTokens = e.totalTokens,
                    model = e.model
                )
            }
        )
    }

    /** Aggregate CSP solver statistics plus the most recent solves. */
    @GetMapping("/csp-stats")
    fun cspStats(@RequestParam(defaultValue = "50") limit: Int): CspStatsResponse {
        val summary = cspSolveLogRepository.summary()
        val recent = cspSolveLogRepository.queryRecent(limit.coerceIn(1, 200))
        return CspStatsResponse(
            enabled = cspSolveLogRepository.isEnabled,
            summary = CspSummaryDto(
                totalSolves = summary.totalSolves,
                feasibleCount = summary.feasibleCount,
                infeasibleCount = summary.infeasibleCount,
                avgDurationMs = summary.avgDurationMs,
                avgNodes = summary.avgNodes,
                avgBacktracks = summary.avgBacktracks,
                avgAc3Revisions = summary.avgAc3Revisions,
                avgVariables = summary.avgVariables,
                avgDomainValues = summary.avgDomainValues
            ),
            recent = recent.map {
                CspSolveEntryDto(
                    id = it.id,
                    timestamp = it.timestamp,
                    nim = it.nim,
                    semester = it.semester,
                    status = it.status.name,
                    objective = it.objective.name,
                    variables = it.variables,
                    constraints = it.constraints,
                    domainValues = it.domainValues,
                    targetSks = it.targetSks,
                    resultSks = it.resultSks,
                    nodes = it.nodes,
                    backtracks = it.backtracks,
                    ac3Revisions = it.ac3Revisions,
                    forwardChecks = it.forwardChecks,
                    durationMs = it.durationMs,
                    alternatives = it.alternatives,
                    mbkmMode = it.mbkmMode,
                    chatId = it.chatId
                )
            }
        )
    }

    /** Per-tool call counts, error counts, and average duration. */
    @GetMapping("/tool-usage")
    fun toolUsage(): ToolUsageResponse {
        val tools = toolCallLogRepository.usageByTool()
        return ToolUsageResponse(
            enabled = toolCallLogRepository.isEnabled,
            count = tools.size,
            tools = tools.map { ToolUsageEntryDto(it.toolName, it.calls, it.errors, it.avgDurationMs) }
        )
    }

    /** Newest individual tool invocations, with arguments and duration. */
    @GetMapping("/tool-calls")
    fun toolCalls(@RequestParam(defaultValue = "100") limit: Int): ToolCallListResponse {
        val entries = toolCallLogRepository.recent(limit.coerceIn(1, 500))
        return ToolCallListResponse(
            enabled = toolCallLogRepository.isEnabled,
            count = entries.size,
            entries = entries.map {
                ToolCallEntryDto(
                    chatId = it.chatId,
                    nim = it.nim,
                    sessionId = it.sessionId,
                    seq = it.seq,
                    toolName = it.toolName,
                    arguments = it.arguments,
                    status = it.status.name,
                    durationMs = it.durationMs,
                    resultSize = it.resultSize,
                    timestamp = it.timestamp.toString()
                )
            }
        )
    }

    /** Per-minute token-usage series (zero-filled) plus an aggregate summary. */
    @GetMapping("/token-usage")
    fun tokenUsage(@RequestParam(defaultValue = "15m") range: String): TokenUsageReport {
        val rangeMinutes = parseRangeMinutes(range)
        val end = truncateToMinute(Instant.now())
        // Inclusive window of exactly "minutes" points (e.g. 5 minutes -> 5 points).
        val start = end.minusSeconds((rangeMinutes - 1) * 60L)

        val rows = tokenUsageRepository.queryRange(start, end).associateBy { truncateToMinute(it.minute) }

        val series = ArrayList<TokenUsagePoint>()
        var totReq = 0L
        var totIn = 0L
        var totOut = 0L
        var totTot = 0L
        var totTool = 0L
        val toolAgg = HashMap<String, Long>()

        var m = start
        while (m <= end) {
            val r = rows[m]
            val req = r?.llmRequests ?: 0L
            val inp = r?.inputTokens ?: 0L
            val out = r?.outputTokens ?: 0L
            val tot = r?.totalTokens ?: 0L
            val tc = r?.toolCalls ?: 0L

            series.add(
                TokenUsagePoint(
                    minute = m.toString(),
                    llmRequests = req.toInt(),
                    inputTokens = inp.toInt(),
                    outputTokens = out.toInt(),
                    totalTokens = tot.toInt(),
                    toolCalls = tc.toInt(),
                    avgTokensPerRequest = (if (req > 0L) tot / req else 0L).toInt()
                )
            )
            totReq += req
            totIn += inp
            totOut += out
            totTot += tot
            totTool += tc
            r?.toolBreakdown?.forEach { (k, v) -> toolAgg[k] = (toolAgg[k] ?: 0L) + v }
            m = m.plusSeconds(60)
        }

        return TokenUsageReport(
            rangeMinutes = rangeMinutes,
            enabled = tokenUsageRepository.isEnabled,
            fromMinute = start.toString(),
            toMinute = end.toString(),
            totalLlmRequests = totReq.toInt(),
            totalInputTokens = totIn.toInt(),
            totalOutputTokens = totOut.toInt(),
            totalTokens = totTot.toInt(),
            totalToolCalls = totTool.toInt(),
            avgTokensPerRequest = (if (totReq > 0L) totTot / totReq else 0L).toInt(),
            toolBreakdown = toolAgg.entries
                .sortedByDescending { it.value }
                .map { TokenUsageToolCount(it.key, it.value.toInt()) },
            series = series
        )
    }
}

/** Truncates an Instant to minute precision (seconds and nanos set to zero). */
private fun truncateToMinute(instant: Instant): Instant =
    Instant.ofEpochSecond((instant.epochSecond / 60) * 60)

/** Parses a range string like "15m", "6h", or a raw minute count into minutes (1..4320). */
private fun parseRangeMinutes(raw: String?): Int {
    val cleaned = (raw ?: "15m").trim().lowercase()
    val minutes = when {
        cleaned.endsWith("h") -> (cleaned.removeSuffix("h").trim().toIntOrNull() ?: 1) * 60
        cleaned.endsWith("m") -> cleaned.removeSuffix("m").trim().toIntOrNull() ?: 15
        else -> cleaned.toIntOrNull() ?: 15
    }
    return minutes.coerceIn(1, 4320) // maximum 72 hours
}
