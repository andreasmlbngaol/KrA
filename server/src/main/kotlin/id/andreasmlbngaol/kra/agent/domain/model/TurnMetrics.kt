package id.andreasmlbngaol.kra.agent.domain.model

import java.time.Instant

/** Status of one tool invocation as seen by the agent runtime. */
enum class ToolCallStatus {
    OK,
    ERROR
}

/** One tool invocation inside a chat turn. */
data class ToolCallRecord(
    val seq: Int,
    val toolName: String,
    val arguments: String,
    val status: ToolCallStatus,
    val durationMs: Long,
    val resultSize: Int,
    val timestamp: Instant
)

/**
 * Mutable per-turn accumulator fed by the Koog event handler.
 *
 * A fresh instance is created for every request, so token counts and tool calls are
 * attributed to exactly one chat turn instead of only to a global per-minute window.
 * Access is confined to the request coroutine, hence plain vars.
 */
class TurnMetrics {
    var llmCalls: Int = 0
    var inputTokens: Long = 0
    var outputTokens: Long = 0
    var totalTokens: Long = 0

    val toolCalls: MutableList<ToolCallRecord> = mutableListOf()

    private val pending: MutableMap<String, PendingTool> = mutableMapOf()
    private var sequence: Int = 0

    fun recordLlmCall(input: Int, output: Int, total: Int) {
        val safeTotal = if (total > 0) total else input + output
        llmCalls++
        inputTokens += input
        outputTokens += output
        totalTokens += safeTotal
    }

    /** Registers the start of a tool call (keyed by the runtime toolCallId when present). */
    fun startTool(toolCallId: String?, toolName: String, arguments: String) {
        val key = toolCallId ?: "$toolName#${sequence}"
        pending[key] = PendingTool(
            seq = sequence++,
            toolName = toolName,
            arguments = arguments,
            startedAt = System.currentTimeMillis()
        )
    }

    /** Registers the end of a tool call. Unknown starts are ignored defensively. */
    fun finishTool(toolCallId: String?, toolName: String, status: ToolCallStatus, resultSize: Int) {
        val key = toolCallId ?: pending.entries.lastOrNull { it.value.toolName == toolName }?.key ?: return
        val started = pending.remove(key) ?: return
        toolCalls += ToolCallRecord(
            seq = started.seq,
            toolName = started.toolName,
            arguments = started.arguments.take(MAX_ARGUMENT_LENGTH),
            status = status,
            durationMs = (System.currentTimeMillis() - started.startedAt).coerceAtLeast(0),
            resultSize = resultSize,
            timestamp = Instant.now()
        )
    }

    private data class PendingTool(
        val seq: Int,
        val toolName: String,
        val arguments: String,
        val startedAt: Long
    )

    companion object {
        private const val MAX_ARGUMENT_LENGTH = 2_000
    }
}
