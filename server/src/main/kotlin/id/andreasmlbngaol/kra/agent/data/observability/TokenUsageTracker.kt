package id.andreasmlbngaol.kra.agent.data.observability

import id.andreasmlbngaol.kra.agent.domain.model.TokenUsageMinute
import id.andreasmlbngaol.kra.agent.domain.repository.TokenUsageRepository
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Real-time tracker for LLM token usage and request counts.
 *
 * How it works:
 *  - The koog EventHandler (installed in KrsAgent) calls [recordLlmCall] whenever an
 *    LLM call completes and [recordToolCall] whenever a tool is invoked.
 *  - A background scheduler PRINTS a report every 60 seconds and persists that minute's
 *    aggregate to PostgreSQL via [repository] (for the dashboard).
 *  - After each report the window counters are reset, so each PostgreSQL row is exactly
 *    one minute of activity.
 *
 * All counters use Atomic / ConcurrentHashMap so they are safe under concurrency.
 */
@Component
class TokenUsageTracker(
    private val repository: TokenUsageRepository
) {
    private val log = LoggerFactory.getLogger("TokenUsage")

    // --- Rolling per-minute window counters (reset on each report) ---
    private val windowInputTokens = AtomicLong(0)
    private val windowOutputTokens = AtomicLong(0)
    private val windowTotalTokens = AtomicLong(0)
    private val windowLlmRequests = AtomicLong(0)
    private val windowToolCalls = AtomicLong(0)
    private val windowToolBreakdown = ConcurrentHashMap<String, AtomicLong>()

    // --- Process-lifetime accumulators (never reset, for context) ---
    private val cumulativeTotalTokens = AtomicLong(0)
    private val cumulativeLlmRequests = AtomicLong(0)

    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "token-usage-reporter").apply { isDaemon = true }
        }

    @jakarta.annotation.PostConstruct
    fun start() {
        log.info(
            "TokenUsageTracker aktif — laporan tiap 60 detik (persisten PostgreSQL: {}).",
            if (repository.isEnabled) "ya" else "tidak"
        )
        scheduler.scheduleAtFixedRate(::report, 60, 60, TimeUnit.SECONDS)
    }

    @PreDestroy
    fun stop() {
        scheduler.shutdownNow()
    }

    /** Called by the EventHandler when an LLM call completes. */
    fun recordLlmCall(inputTokens: Int, outputTokens: Int, totalTokens: Int) {
        val safeTotal = if (totalTokens > 0) totalTokens else inputTokens + outputTokens
        windowInputTokens.addAndGet(inputTokens.toLong())
        windowOutputTokens.addAndGet(outputTokens.toLong())
        windowTotalTokens.addAndGet(safeTotal.toLong())
        windowLlmRequests.incrementAndGet()
        cumulativeTotalTokens.addAndGet(safeTotal.toLong())
        cumulativeLlmRequests.incrementAndGet()
    }

    /** Called by the EventHandler whenever the model invokes a tool. */
    fun recordToolCall(toolName: String) {
        windowToolCalls.incrementAndGet()
        windowToolBreakdown.computeIfAbsent(toolName) { AtomicLong(0) }.incrementAndGet()
    }

    /** Snapshots and resets the window counters, logs a report, and persists the minute. */
    private fun report() {
        val input = windowInputTokens.getAndSet(0)
        val output = windowOutputTokens.getAndSet(0)
        val total = windowTotalTokens.getAndSet(0)
        val requests = windowLlmRequests.getAndSet(0)
        val toolCalls = windowToolCalls.getAndSet(0)
        val breakdown = windowToolBreakdown.entries
            .map { it.key to it.value.getAndSet(0) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

        if (requests == 0L && toolCalls == 0L) {
            log.debug("[TOKEN REPORT] idle — tidak ada aktivitas LLM pada menit ini.")
            return
        }

        val avgPerRequest = if (requests > 0) total / requests else 0
        val toolStr = breakdown.joinToString(", ") { "${it.first}=${it.second}" }.ifEmpty { "-" }

        log.info(
            buildString {
                appendLine("===== TOKEN REPORT (≈ 1 menit terakhir) =====")
                appendLine("LLM requests : $requests  (≈ $avgPerRequest token/request)")
                appendLine("Tokens       : input=$input  output=$output  total=$total")
                appendLine("Tool calls   : $toolCalls  [$toolStr]")
                append("Kumulatif    : ${cumulativeLlmRequests.get()} requests, ${cumulativeTotalTokens.get()} token sejak start")
            }
        )

        try {
            repository.record(
                TokenUsageMinute(
                    minute = truncateToMinute(Instant.now()),
                    llmRequests = requests,
                    inputTokens = input,
                    outputTokens = output,
                    totalTokens = total,
                    toolCalls = toolCalls,
                    toolBreakdown = breakdown.toMap()
                )
            )
        } catch (e: Exception) {
            log.warn("Gagal menyimpan metrik menit ke PostgreSQL: {}", e.message)
        }
    }

    /** Truncates an Instant to minute precision. */
    private fun truncateToMinute(instant: Instant): Instant =
        Instant.ofEpochSecond((instant.epochSecond / 60) * 60)
}
