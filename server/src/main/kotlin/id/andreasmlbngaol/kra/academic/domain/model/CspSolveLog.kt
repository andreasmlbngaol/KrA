package id.andreasmlbngaol.kra.academic.domain.model

import id.andreasmlbngaol.kra.academic.domain.model.csp.CspObjective
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspStatus

/**
 * One row of solver telemetry. Written on every solve so the thesis can report the
 * empirical cost of the CSP search (nodes, backtracks, AC-3 revisions, time) instead of
 * only claiming that a solver was used.
 */
data class CspSolveLog(
    val nim: String,
    val semester: Int,
    val status: CspStatus,
    val objective: CspObjective,
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

/** A persisted solve log entry, read back for the monitoring dashboard. */
data class CspSolveLogEntry(
    val id: String,
    val timestamp: String,
    val nim: String,
    val semester: Int,
    val status: CspStatus,
    val objective: CspObjective,
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

/** Aggregate solver statistics for the evaluation chapter. */
data class CspSolveSummary(
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
