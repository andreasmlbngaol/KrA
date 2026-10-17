package id.andreasmlbngaol.kra.agent.data.repository.jpa

import id.andreasmlbngaol.kra.agent.data.entity.EvaluationCaseEntity
import id.andreasmlbngaol.kra.agent.data.entity.EvaluationRunEntity
import id.andreasmlbngaol.kra.agent.data.entity.SurveyAnswerEntity
import id.andreasmlbngaol.kra.agent.data.entity.SurveyResponseEntity
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.UUID

interface EvaluationCaseJpaRepository : JpaRepository<EvaluationCaseEntity, UUID> {
    fun findByCode(code: String): EvaluationCaseEntity?
    fun findAllByActiveTrue(): List<EvaluationCaseEntity>
}

interface EvaluationRunJpaRepository : JpaRepository<EvaluationRunEntity, UUID> {
    fun findAllByOrderByTsDesc(limit: Limit): List<EvaluationRunEntity>

    @Query(
        """
        select count(r) as total,
               coalesce(sum(case when r.solutionCorrect = true then 1 else 0 end), 0) as correct,
               coalesce(avg(case when r.solutionCorrect = true then 1.0 else 0.0 end), 0) as accuracy,
               coalesce(avg(r.toolPrecision), 0) as avgPrecision,
               coalesce(avg(r.toolRecall), 0) as avgRecall,
               coalesce(avg(r.toolF1), 0) as avgF1
        from EvaluationRunEntity r
        """
    )
    fun summary(): EvaluationRunAggregate
}

/**
 * Numeric columns are projected as [BigDecimal] because PostgreSQL returns NUMERIC for
 * `avg()` and for `1.0` literals; the implementation converts them to Double.
 */
interface EvaluationRunAggregate {
    val total: Long
    val correct: Long
    val accuracy: BigDecimal
    val avgPrecision: BigDecimal
    val avgRecall: BigDecimal
    val avgF1: BigDecimal
}

interface SurveyResponseJpaRepository : JpaRepository<SurveyResponseEntity, UUID> {
    fun findAllByOrderBySubmittedAtDesc(limit: Limit): List<SurveyResponseEntity>

    /**
     * SUS responses use `sus_score`; TAM responses use the mean of `pu_score` and
     * `peou_score`. Both branches are coalesced so a half-filled row never yields NULL.
     */
    @Query(
        """
        select count(s) as responses,
               coalesce(avg(
                   coalesce(s.susScore, (coalesce(s.puScore, 0) + coalesce(s.peouScore, 0)) / 2)
               ), 0) as avgScore
        from SurveyResponseEntity s
        where s.instrument = :instrument
        """
    )
    fun summary(@Param("instrument") instrument: String): SurveyAggregate
}

interface SurveyAggregate {
    val responses: Long
    val avgScore: BigDecimal
}

interface SurveyAnswerJpaRepository : JpaRepository<SurveyAnswerEntity, UUID> {
    fun findByResponseId(responseId: UUID): List<SurveyAnswerEntity>

    fun findByResponseIdIn(responseIds: Collection<UUID>): List<SurveyAnswerEntity>
}
