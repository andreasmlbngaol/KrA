package id.andreasmlbngaol.kra.agent.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/** Ground-truth evaluation scenario. */
@Entity
@Table(name = "evaluation_case")
class EvaluationCaseEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "code", nullable = false, length = 32)
    var code: String = "",

    @Column(name = "nim", nullable = false, length = 9)
    var nim: String = "",

    @Column(name = "semester", nullable = false)
    var semester: Int = 0,

    @Column(name = "description", nullable = false, columnDefinition = "text")
    var description: String = "",

    @Column(name = "expected_feasible", nullable = false)
    var expectedFeasible: Boolean = false,

    @Column(name = "expected_min_sks")
    var expectedMinSks: Int? = null,

    @Column(name = "expected_max_sks")
    var expectedMaxSks: Int? = null,

    @Column(name = "expected_tools", nullable = false, columnDefinition = "text")
    var expectedTools: String = "[]",

    @Column(name = "expected_course_codes", nullable = false, columnDefinition = "text")
    var expectedCourseCodes: String = "[]",

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.EPOCH
)

/** One measured run against an [EvaluationCaseEntity]. */
@Entity
@Table(name = "evaluation_run")
class EvaluationRunEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "case_id", nullable = false)
    var caseId: UUID = UUID(0, 0),

    @Column(name = "chat_id")
    var chatId: UUID? = null,

    @Column(name = "ts", nullable = false)
    var ts: Instant = Instant.EPOCH,

    @Column(name = "feasible", nullable = false)
    var feasible: Boolean = false,

    @Column(name = "total_sks", nullable = false)
    var totalSks: Int = 0,

    @Column(name = "tool_precision", precision = 5, scale = 4)
    var toolPrecision: BigDecimal? = null,

    @Column(name = "tool_recall", precision = 5, scale = 4)
    var toolRecall: BigDecimal? = null,

    @Column(name = "tool_f1", precision = 5, scale = 4)
    var toolF1: BigDecimal? = null,

    @Column(name = "solution_correct", nullable = false)
    var solutionCorrect: Boolean = false,

    @Column(name = "notes", columnDefinition = "text")
    var notes: String? = null
)

/** One respondent's questionnaire. */
@Entity
@Table(name = "survey_response")
class SurveyResponseEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "nim", nullable = false, length = 9)
    var nim: String = "",

    @Column(name = "instrument", nullable = false, length = 8)
    var instrument: String = "TAM",

    @Column(name = "submitted_at", nullable = false)
    var submittedAt: Instant = Instant.EPOCH,

    @Column(name = "sus_score", precision = 5, scale = 2)
    var susScore: BigDecimal? = null,

    @Column(name = "pu_score", precision = 5, scale = 2)
    var puScore: BigDecimal? = null,

    @Column(name = "peou_score", precision = 5, scale = 2)
    var peouScore: BigDecimal? = null
)

/** One Likert answer of a questionnaire. */
@Entity
@Table(name = "survey_answer")
class SurveyAnswerEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "response_id", nullable = false)
    var responseId: UUID = UUID(0, 0),

    @Column(name = "item_code", nullable = false, length = 16)
    var itemCode: String = "",

    @Column(name = "likert", nullable = false)
    var likert: Short = 1
)
