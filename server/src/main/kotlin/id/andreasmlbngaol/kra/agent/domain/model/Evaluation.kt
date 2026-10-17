package id.andreasmlbngaol.kra.agent.domain.model

import java.time.Instant

/** One evaluation scenario (ground truth) used to measure agent quality. */
data class EvaluationCase(
    val code: String,
    val nim: String,
    val semester: Int,
    val description: String,
    val expectedFeasible: Boolean,
    val expectedMinSks: Int? = null,
    val expectedMaxSks: Int? = null,
    val expectedTools: List<String> = emptyList(),
    val expectedCourseCodes: List<String> = emptyList(),
    val active: Boolean = true,
    val createdAt: Instant = Instant.EPOCH
)

/** Result of comparing one agent run against an [EvaluationCase]. */
data class EvaluationRunResult(
    val caseCode: String,
    val chatId: String?,
    val feasible: Boolean,
    val totalSks: Int,
    val precision: Double,
    val recall: Double,
    val f1: Double,
    val solutionCorrect: Boolean,
    val expectedTools: List<String> = emptyList(),
    val actualTools: List<String> = emptyList()
)

/** Aggregate of every recorded evaluation run. */
data class EvaluationSummary(
    val totalRuns: Long,
    val correctRuns: Long,
    val solutionAccuracy: Double,
    val avgPrecision: Double,
    val avgRecall: Double,
    val avgF1: Double
)

/** Questionnaire instrument. */
enum class SurveyInstrument {
    TAM,
    SUS
}

/** One stored run, flattened with its case metadata for the evaluation dashboard. */
data class EvaluationRunRecord(
    val caseCode: String,
    val chatId: String?,
    val timestamp: Instant,
    val feasible: Boolean,
    val totalSks: Int,
    val precision: Double,
    val recall: Double,
    val f1: Double,
    val solutionCorrect: Boolean,
    val expectedTools: List<String>,
    val actualTools: List<String>,
    val notes: String?
)

/** Aggregate of one questionnaire instrument. */
data class SurveySummary(
    val instrument: SurveyInstrument,
    val responses: Long,
    val avgScore: Double
)

/** One stored questionnaire response together with its item answers. */
data class SurveyResponseRecord(
    val nim: String,
    val instrument: SurveyInstrument,
    val submittedAt: Instant,
    val score: Double,
    val susScore: Double? = null,
    val puScore: Double? = null,
    val peouScore: Double? = null,
    val answers: Map<String, Int>
)
