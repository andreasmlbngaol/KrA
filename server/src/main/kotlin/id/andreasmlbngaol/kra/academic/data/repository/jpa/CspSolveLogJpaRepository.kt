package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.CspSolveLogEntity
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspStatus
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

/** Projection for the aggregate solver statistics shown on the monitoring page. */
interface CspSolveAggregate {
    val total: Long
    val feasible: Long
    val infeasible: Long
    val avgDuration: Double
    val avgNodes: Double
    val avgBacktracks: Double
    val avgAc3: Double
    val avgVariables: Double
    val avgDomain: Double
}

interface CspSolveLogJpaRepository : JpaRepository<CspSolveLogEntity, UUID> {

    fun findAllByOrderByTsDesc(limit: Limit): List<CspSolveLogEntity>

    @Query(
        """
        select count(l) as total,
               coalesce(sum(case when l.status = :feasible then 1 else 0 end), 0) as feasible,
               coalesce(sum(case when l.status = :infeasible then 1 else 0 end), 0) as infeasible,
               coalesce(avg(l.durationMs), 0) as avgDuration,
               coalesce(avg(l.nodes), 0) as avgNodes,
               coalesce(avg(l.backtracks), 0) as avgBacktracks,
               coalesce(avg(l.ac3Revisions), 0) as avgAc3,
               coalesce(avg(l.variables), 0) as avgVariables,
               coalesce(avg(l.domainValues), 0) as avgDomain
        from CspSolveLogEntity l
        """
    )
    fun aggregate(
        @Param("feasible") feasible: CspStatus,
        @Param("infeasible") infeasible: CspStatus
    ): CspSolveAggregate
}
