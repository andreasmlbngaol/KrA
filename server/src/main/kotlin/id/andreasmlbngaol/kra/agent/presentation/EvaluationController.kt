package id.andreasmlbngaol.kra.agent.presentation

import id.andreasmlbngaol.kra.agent.domain.model.EvaluationCase
import id.andreasmlbngaol.kra.agent.domain.usecase.EvaluationUseCase
import id.andreasmlbngaol.kra.agent.presentation.dto.EvaluationCaseDto
import id.andreasmlbngaol.kra.agent.presentation.dto.EvaluationCaseListResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.EvaluationCaseRequest
import id.andreasmlbngaol.kra.agent.presentation.dto.EvaluationCaseSavedResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.EvaluationRunDto
import id.andreasmlbngaol.kra.agent.presentation.dto.EvaluationRunListResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.EvaluationRunRequest
import id.andreasmlbngaol.kra.agent.presentation.dto.EvaluationRunResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.EvaluationSummaryResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.SurveyAnswerDto
import id.andreasmlbngaol.kra.agent.presentation.dto.SurveyResponseDto
import id.andreasmlbngaol.kra.agent.presentation.dto.SurveyResponseListResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.SurveySubmitRequest
import id.andreasmlbngaol.kra.agent.presentation.dto.SurveySubmitResponse
import id.andreasmlbngaol.kra.agent.presentation.dto.SurveySummaryDto
import id.andreasmlbngaol.kra.agent.presentation.dto.SurveySummaryResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Evaluation endpoints (INTENTIONALLY WITHOUT AUTH - used internally by the researcher):
 * authoring ground-truth cases, measuring a turn against a case, and reading the aggregate
 * accuracy / tool metrics plus the TAM-SUS acceptance results.
 */
@RestController
@RequestMapping("/api/evaluation")
class EvaluationController(
    private val evaluationUseCase: EvaluationUseCase
) {

    /** Creates or updates a ground-truth case (upsert by [EvaluationCaseRequest.code]). */
    @PostMapping("/cases")
    fun saveCase(@RequestBody request: EvaluationCaseRequest): EvaluationCaseSavedResponse {
        val id = evaluationUseCase.saveCase(
            EvaluationCase(
                code = request.code,
                nim = request.nim,
                semester = request.semester,
                description = request.description,
                expectedFeasible = request.expectedFeasible,
                expectedMinSks = request.expectedMinSks,
                expectedMaxSks = request.expectedMaxSks,
                expectedTools = request.expectedTools,
                expectedCourseCodes = request.expectedCourseCodes
            )
        )
        return EvaluationCaseSavedResponse(id = id, code = request.code)
    }

    @GetMapping("/cases")
    fun listCases(): EvaluationCaseListResponse {
        val cases = evaluationUseCase.listCases()
        return EvaluationCaseListResponse(
            count = cases.size,
            cases = cases.map {
                EvaluationCaseDto(
                    code = it.code,
                    nim = it.nim,
                    semester = it.semester,
                    description = it.description,
                    expectedFeasible = it.expectedFeasible,
                    expectedMinSks = it.expectedMinSks,
                    expectedMaxSks = it.expectedMaxSks,
                    expectedTools = it.expectedTools,
                    expectedCourseCodes = it.expectedCourseCodes,
                    active = it.active,
                    createdAt = it.createdAt.toString()
                )
            }
        )
    }

    /**
     * Measures one observed turn against a case. Tool precision/recall/F1 are derived from
     * the tool calls recorded for [EvaluationRunRequest.chatId], so the turn must have been
     * produced by the agent (not entered manually).
     */
    @PostMapping("/cases/{code}/run")
    fun recordRun(
        @PathVariable code: String,
        @RequestBody request: EvaluationRunRequest
    ): EvaluationRunResponse {
        val result = evaluationUseCase.recordRun(
            caseCode = code,
            chatId = request.chatId,
            feasible = request.feasible,
            totalSks = request.totalSks,
            courseCodes = request.courseCodes,
            notes = request.notes
        )
        return EvaluationRunResponse(
            caseCode = result.caseCode,
            feasible = result.feasible,
            totalSks = result.totalSks,
            precision = result.precision,
            recall = result.recall,
            f1 = result.f1,
            solutionCorrect = result.solutionCorrect,
            expectedTools = result.expectedTools,
            actualTools = result.actualTools
        )
    }

    @GetMapping("/summary")
    fun summary(): EvaluationSummaryResponse {
        val s = evaluationUseCase.summary()
        return EvaluationSummaryResponse(
            totalRuns = s.totalRuns,
            correctRuns = s.correctRuns,
            solutionAccuracy = s.solutionAccuracy,
            avgPrecision = s.avgPrecision,
            avgRecall = s.avgRecall,
            avgF1 = s.avgF1
        )
    }

    /** Newest recorded runs, including the tools expected vs actually used. */
    @GetMapping("/runs")
    fun listRuns(@RequestParam(defaultValue = "100") limit: Int): EvaluationRunListResponse {
        val runs = evaluationUseCase.listRuns(limit.coerceIn(1, 500))
        return EvaluationRunListResponse(
            count = runs.size,
            runs = runs.map {
                EvaluationRunDto(
                    caseCode = it.caseCode,
                    chatId = it.chatId,
                    timestamp = it.timestamp.toString(),
                    feasible = it.feasible,
                    totalSks = it.totalSks,
                    precision = it.precision,
                    recall = it.recall,
                    f1 = it.f1,
                    solutionCorrect = it.solutionCorrect,
                    expectedTools = it.expectedTools,
                    actualTools = it.actualTools,
                    notes = it.notes
                )
            }
        )
    }

    /** Stores one TAM or SUS questionnaire; returns the computed score. */
    @PostMapping("/survey")
    fun submitSurvey(@RequestBody request: SurveySubmitRequest): SurveySubmitResponse {
        val score = evaluationUseCase.submitSurvey(
            nim = request.nim,
            instrument = request.instrument,
            answers = request.answers
        )
        return SurveySubmitResponse(instrument = request.instrument.uppercase(), score = score)
    }

    @GetMapping("/survey/summary")
    fun surveySummary(): SurveySummaryResponse = SurveySummaryResponse(
        instruments = evaluationUseCase.surveySummary().map {
            SurveySummaryDto(it.instrument.name, it.responses, it.avgScore)
        }
    )

    /** Newest questionnaire responses with every item answer. */
    @GetMapping("/survey/responses")
    fun surveyResponses(@RequestParam(defaultValue = "100") limit: Int): SurveyResponseListResponse {
        val responses = evaluationUseCase.listSurveyResponses(limit.coerceIn(1, 500))
        return SurveyResponseListResponse(
            count = responses.size,
            responses = responses.map {
                SurveyResponseDto(
                    nim = it.nim,
                    instrument = it.instrument.name,
                    submittedAt = it.submittedAt.toString(),
                    score = it.score,
                    susScore = it.susScore,
                    puScore = it.puScore,
                    peouScore = it.peouScore,
                    answers = it.answers.map { (item, likert) -> SurveyAnswerDto(item, likert) }
                        .sortedBy { it.itemCode }
                )
            }
        )
    }
}
