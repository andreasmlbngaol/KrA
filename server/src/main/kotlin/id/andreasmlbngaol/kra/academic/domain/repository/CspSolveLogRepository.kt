package id.andreasmlbngaol.kra.academic.domain.repository

import id.andreasmlbngaol.kra.academic.domain.model.CspSolveLog
import id.andreasmlbngaol.kra.academic.domain.model.CspSolveLogEntry
import id.andreasmlbngaol.kra.academic.domain.model.CspSolveSummary

/**
 * Telemetry sink for the CSP solver. Implementations must never throw into the solver
 * path: a failed write may not break a student's KRS request.
 */
interface CspSolveLogRepository {
    val isEnabled: Boolean

    fun record(log: CspSolveLog)

    fun queryRecent(limit: Int): List<CspSolveLogEntry>

    fun summary(): CspSolveSummary
}
