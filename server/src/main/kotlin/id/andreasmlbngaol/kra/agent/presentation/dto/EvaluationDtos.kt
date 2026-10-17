package id.andreasmlbngaol.kra.agent.presentation.dto

import kotlinx.serialization.Serializable

/** Ground-truth scenario submitted by the researcher. */
@Serializable
data class EvaluationCaseRequest(
    val code: String,
    val nim: String,
    val semester: Int,
    val description: String,
    val expectedFeasible: Boolean,
    val expectedMinSks: Int? = null,
    val expectedMaxSks: Int? = null,
    val expectedTools: List<String> = emptyList(),
    val expectedCourseCodes: List<String> = emptyList()
)

@Serializable
data class EvaluationCaseDto(
    val code: String,
    val nim: String,
    val semester: Int,
    val description: String,
    val expectedFeasible: Boolean,
    val expectedMinSks: Int? = null,
    val expectedMaxSks: Int? = null,
    val expectedTools: List<String>,
    val expectedCourseCodes: List<String>,
    val active: Boolean = true,
    val createdAt: String? = null
)

@Serializable
data class EvaluationCaseListResponse(
    val count: Int,
    val cases: List<EvaluationCaseDto>
)

@Serializable
data class EvaluationCaseSavedResponse(
    val id: String,
    val code: String
)

/** Outcome of one turn, as observed by the researcher running the scenario. */
@Serializable
data class EvaluationRunRequest(
    val chatId: String? = null,
    val feasible: Boolean,
    val totalSks: Int,
    val courseCodes: List<String> = emptyList(),
    val notes: String? = null
)

@Serializable
data class EvaluationRunResponse(
    val caseCode: String,
    val feasible: Boolean,
    val totalSks: Int,
    val precision: Double,
    val recall: Double,
    val f1: Double,
    val solutionCorrect: Boolean,
    val expectedTools: List<String>,
    val actualTools: List<String>
)

@Serializable
data class EvaluationSummaryResponse(
    val totalRuns: Long,
    val correctRuns: Long,
    val solutionAccuracy: Double,
    val avgPrecision: Double,
    val avgRecall: Double,
    val avgF1: Double
)

/** One stored evaluation run, with the tools expected by the case and actually used. */
@Serializable
data class EvaluationRunDto(
    val caseCode: String,
    val chatId: String? = null,
    val timestamp: String,
    val feasible: Boolean,
    val totalSks: Int,
    val precision: Double,
    val recall: Double,
    val f1: Double,
    val solutionCorrect: Boolean,
    val expectedTools: List<String>,
    val actualTools: List<String>,
    val notes: String? = null
)

@Serializable
data class EvaluationRunListResponse(
    val count: Int,
    val runs: List<EvaluationRunDto>
)

/** One questionnaire submission (item code -> Likert 1..5). */
@Serializable
data class SurveySubmitRequest(
    val nim: String,
    val instrument: String,
    val answers: Map<String, Int>
)

@Serializable
data class SurveySubmitResponse(
    val instrument: String,
    val score: Double
)

@Serializable
data class SurveySummaryDto(
    val instrument: String,
    val responses: Long,
    val avgScore: Double
)

@Serializable
data class SurveySummaryResponse(
    val instruments: List<SurveySummaryDto>
)

/** One stored answer, e.g. item PU1 with Likert 4. */
@Serializable
data class SurveyAnswerDto(
    val itemCode: String,
    val likert: Int
)

/** One stored questionnaire response with its computed score and answers. */
@Serializable
data class SurveyResponseDto(
    val nim: String,
    val instrument: String,
    val submittedAt: String,
    val score: Double,
    val susScore: Double? = null,
    val puScore: Double? = null,
    val peouScore: Double? = null,
    val answers: List<SurveyAnswerDto>
)

@Serializable
data class SurveyResponseListResponse(
    val count: Int,
    val responses: List<SurveyResponseDto>
)
