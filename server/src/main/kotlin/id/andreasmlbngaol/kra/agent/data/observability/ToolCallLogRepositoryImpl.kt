package id.andreasmlbngaol.kra.agent.data.observability

import id.andreasmlbngaol.kra.agent.data.entity.AgentToolCallEntity
import id.andreasmlbngaol.kra.agent.data.repository.jpa.AgentToolCallJpaRepository
import id.andreasmlbngaol.kra.agent.domain.model.AgentToolCallLog
import id.andreasmlbngaol.kra.agent.domain.model.ToolCallStatus
import id.andreasmlbngaol.kra.agent.domain.model.ToolUsageStat
import id.andreasmlbngaol.kra.agent.domain.repository.ToolCallLogRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneOffset
import java.util.UUID

/**
 * PostgreSQL-backed tool-call telemetry.
 *
 * Writes happen on a dedicated executor after the turn is finished, so the agent loop is
 * never blocked and a database outage never breaks a student request.
 */
@Repository
class ToolCallLogRepositoryImpl(
    private val repo: AgentToolCallJpaRepository
) : ToolCallLogRepository {

    private val logger = LoggerFactory.getLogger("ToolCallLogRepository")
    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "tool-call-log-writer").apply { isDaemon = true }
    }

    override val isEnabled: Boolean get() = true

    override fun recordAll(logs: List<AgentToolCallLog>) {
        if (logs.isEmpty()) return
        val entities = logs.map { entry ->
            AgentToolCallEntity(
                id = UUID.randomUUID(),
                day = entry.timestamp.atZone(ZoneOffset.UTC).toLocalDate().toString(),
                chatId = runCatching { UUID.fromString(entry.chatId) }.getOrElse { UUID(0, 0) },
                ts = entry.timestamp,
                nim = entry.nim,
                sessionId = entry.sessionId,
                seq = entry.seq,
                toolName = entry.toolName,
                arguments = entry.arguments,
                status = entry.status,
                durationMs = entry.durationMs,
                resultSize = entry.resultSize
            )
        }
        executor.execute {
            runCatching { repo.saveAll(entities) }
                .onFailure { logger.warn("Gagal menyimpan log tool call: {}", it.message) }
        }
    }

    @Transactional(readOnly = true)
    override fun usageByTool(): List<ToolUsageStat> = runCatching {
        repo.usageByTool(ToolCallStatus.ERROR).map {
            ToolUsageStat(
                toolName = it.toolName,
                calls = it.calls,
                errors = it.errors,
                avgDurationMs = it.avgDuration
            )
        }
    }.getOrElse {
        logger.warn("Gagal membaca agregat tool call: {}", it.message)
        emptyList()
    }

    @Transactional(readOnly = true)
    override fun recent(limit: Int): List<AgentToolCallLog> = runCatching {
        repo.findAllByOrderByTsDesc(Limit.of(limit.coerceIn(1, 500))).map {
            AgentToolCallLog(
                chatId = it.chatId.toString(),
                nim = it.nim,
                sessionId = it.sessionId,
                seq = it.seq,
                toolName = it.toolName,
                arguments = it.arguments,
                status = it.status,
                durationMs = it.durationMs,
                resultSize = it.resultSize,
                timestamp = it.ts
            )
        }
    }.getOrElse {
        logger.warn("Gagal membaca log tool call: {}", it.message)
        emptyList()
    }
}
