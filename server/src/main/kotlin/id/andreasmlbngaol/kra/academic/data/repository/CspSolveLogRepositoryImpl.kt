package id.andreasmlbngaol.kra.academic.data.repository

import id.andreasmlbngaol.kra.academic.data.entity.CspSolveLogEntity
import id.andreasmlbngaol.kra.academic.data.repository.jpa.CspSolveLogJpaRepository
import id.andreasmlbngaol.kra.academic.domain.model.CspSolveLog
import id.andreasmlbngaol.kra.academic.domain.model.CspSolveLogEntry
import id.andreasmlbngaol.kra.academic.domain.model.CspSolveSummary
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspStatus
import id.andreasmlbngaol.kra.academic.domain.repository.CspSolveLogRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * PostgreSQL-backed solver telemetry.
 *
 * Writes are fire-and-forget on a dedicated executor so a slow or unavailable database
 * can never delay or break a KRS request. Reads degrade to empty/zero values.
 */
@Repository
class CspSolveLogRepositoryImpl(
    private val repo: CspSolveLogJpaRepository
) : CspSolveLogRepository {

    private val logger = LoggerFactory.getLogger("CspSolveLogRepository")
    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "csp-solve-log-writer").apply { isDaemon = true }
    }

    override val isEnabled: Boolean get() = true

    override fun record(entry: CspSolveLog) {
        val entity = CspSolveLogEntity(
            id = UUID.randomUUID(),
            ts = Instant.now(),
            nim = entry.nim,
            semester = entry.semester,
            status = entry.status,
            objective = entry.objective,
            variables = entry.variables,
            constraints = entry.constraints,
            domainValues = entry.domainValues,
            targetSks = entry.targetSks,
            resultSks = entry.resultSks,
            nodes = entry.nodes,
            backtracks = entry.backtracks,
            ac3Revisions = entry.ac3Revisions,
            forwardChecks = entry.forwardChecks,
            durationMs = entry.durationMs,
            alternatives = entry.alternatives,
            mbkmMode = entry.mbkmMode,
            chatId = entry.chatId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        )
        executor.execute {
            runCatching { repo.save(entity) }
                .onFailure { logger.warn("Gagal menyimpan log solver CSP: {}", it.message) }
        }
    }

    @Transactional(readOnly = true)
    override fun queryRecent(limit: Int): List<CspSolveLogEntry> {
        val capped = limit.coerceIn(1, 500)
        return runCatching {
            repo.findAllByOrderByTsDesc(Limit.of(capped)).map {
                CspSolveLogEntry(
                    id = it.id.toString(),
                    timestamp = it.ts.toString(),
                    nim = it.nim,
                    semester = it.semester,
                    status = it.status,
                    objective = it.objective,
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
                    chatId = it.chatId?.toString()
                )
            }
        }.getOrElse {
            logger.warn("Gagal membaca log solver CSP: {}", it.message)
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    override fun summary(): CspSolveSummary = runCatching {
        val a = repo.aggregate(CspStatus.FEASIBLE, CspStatus.INFEASIBLE)
        CspSolveSummary(
            totalSolves = a.total,
            feasibleCount = a.feasible,
            infeasibleCount = a.infeasible,
            avgDurationMs = a.avgDuration,
            avgNodes = a.avgNodes,
            avgBacktracks = a.avgBacktracks,
            avgAc3Revisions = a.avgAc3,
            avgVariables = a.avgVariables,
            avgDomainValues = a.avgDomain
        )
    }.getOrElse {
        logger.warn("Gagal menghitung agregat solver CSP: {}", it.message)
        CspSolveSummary(0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
}
