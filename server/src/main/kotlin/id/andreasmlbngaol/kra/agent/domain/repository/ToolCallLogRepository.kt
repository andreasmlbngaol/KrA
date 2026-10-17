package id.andreasmlbngaol.kra.agent.domain.repository

import id.andreasmlbngaol.kra.agent.domain.model.AgentToolCallLog
import id.andreasmlbngaol.kra.agent.domain.model.ToolUsageStat

/**
 * Storage for per-tool-call telemetry. Writes must never throw into the agent loop.
 */
interface ToolCallLogRepository {
    val isEnabled: Boolean

    fun recordAll(logs: List<AgentToolCallLog>)

    fun usageByTool(): List<ToolUsageStat>

    fun recent(limit: Int): List<AgentToolCallLog>
}
