package id.andreasmlbngaol.kra.academic.data.entity

import id.andreasmlbngaol.kra.academic.domain.model.csp.CspObjective
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** Persisted solver telemetry (one row per solve). */
@Entity
@Table(name = "csp_solve_log")
class CspSolveLogEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "ts", nullable = false)
    var ts: Instant = Instant.EPOCH,

    @Column(name = "nim", nullable = false, length = 9)
    var nim: String = "",

    @Column(name = "semester", nullable = false)
    var semester: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: CspStatus = CspStatus.INFEASIBLE,

    @Enumerated(EnumType.STRING)
    @Column(name = "objective", nullable = false, length = 24)
    var objective: CspObjective = CspObjective.FEASIBILITY_ONLY,

    @Column(name = "variables", nullable = false)
    var variables: Int = 0,

    @Column(name = "constraints", nullable = false)
    var constraints: Int = 0,

    @Column(name = "domain_values", nullable = false)
    var domainValues: Int = 0,

    @Column(name = "target_sks", nullable = false)
    var targetSks: Int = 0,

    @Column(name = "result_sks", nullable = false)
    var resultSks: Int = 0,

    @Column(name = "nodes", nullable = false)
    var nodes: Int = 0,

    @Column(name = "backtracks", nullable = false)
    var backtracks: Int = 0,

    @Column(name = "ac3_revisions", nullable = false)
    var ac3Revisions: Int = 0,

    @Column(name = "forward_checks", nullable = false)
    var forwardChecks: Int = 0,

    @Column(name = "duration_ms", nullable = false)
    var durationMs: Long = 0,

    @Column(name = "alternatives", nullable = false)
    var alternatives: Int = 0,

    @Column(name = "mbkm_mode", nullable = false)
    var mbkmMode: Boolean = false,

    @Column(name = "chat_id")
    var chatId: UUID? = null
)
