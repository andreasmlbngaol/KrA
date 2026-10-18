package id.andreasmlbngaol.kra.agent.domain.usecase

import id.andreasmlbngaol.kra.academic.domain.usecase.StudentUseCase
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationCase
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationMetrics
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationRunRecord
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationRunResult
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationSummary
import id.andreasmlbngaol.kra.agent.domain.model.SurveyInstrument
import id.andreasmlbngaol.kra.agent.domain.model.SurveyResponseRecord
import id.andreasmlbngaol.kra.agent.domain.model.SurveyScoring
import id.andreasmlbngaol.kra.agent.domain.model.SurveySummary
import id.andreasmlbngaol.kra.agent.domain.model.ToolCallExpectation
import id.andreasmlbngaol.kra.agent.domain.repository.EvaluationRepository
import id.andreasmlbngaol.kra.agent.domain.repository.SurveyRepository
import id.andreasmlbngaol.kra.core.error.BadRequestException
import org.springframework.stereotype.Service

/**
 * Evaluation of the agent as described in the proposal: solution accuracy against
 * ground-truth cases, tool invocation precision/recall/F1, and TAM/SUS acceptance.
 *
 * The metric arithmetic itself lives in [EvaluationMetrics] (pure, unit-tested); this class
 * only assembles the case, the recorded turn, and the persistence call.
 */
@Service
class EvaluationUseCase(
    private val evaluationRepository: EvaluationRepository,
    private val surveyRepository: SurveyRepository,
    private val studentUseCase: StudentUseCase
) {

    /** Creates or updates a ground-truth case (identified by its code). */
    fun saveCase(case: EvaluationCase): String {
        if (case.code.isBlank()) throw BadRequestException("Kode case tidak boleh kosong")
        if (studentUseCase.getStudentByNim(case.nim) == null) {
            throw BadRequestException("Mahasiswa dengan NIM ${case.nim} tidak ditemukan")
        }
        if (case.semester !in SEMESTER_RANGE) {
            throw BadRequestException("Semester harus ${SEMESTER_RANGE.first}..${SEMESTER_RANGE.last}")
        }
        val min = case.expectedMinSks
        val max = case.expectedMaxSks
        if (min != null && max != null && min > max) {
            throw BadRequestException("expectedMinSks tidak boleh lebih besar dari expectedMaxSks")
        }
        // Fail at authoring time: a malformed expectation would otherwise be scored as a
        // miss and silently depress the metric.
        case.expectedTools.forEach { raw ->
            try {
                ToolCallExpectation.parse(raw)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("expectedTools tidak valid: undefined")
            }
        }
        return evaluationRepository.saveCase(case)
    }

    fun listCases(): List<EvaluationCase> = evaluationRepository.listCases()

    /**
     * Measures one agent run against its case and stores the result.
     *
     * [courseCodes] are the courses present in the agent's final KRS proposal, used to check
     * that the solution contains every course the case expects.
     */
    fun recordRun(
        caseCode: String,
        chatId: String?,
        feasible: Boolean,
        totalSks: Int,
        courseCodes: List<String>,
        notes: String? = null
    ): EvaluationRunResult {
        val case = evaluationRepository.findCase(caseCode)
            ?: throw BadRequestException("Case evaluasi '$caseCode' tidak ditemukan")

        val actualCalls = chatId?.let { evaluationRepository.toolCallsForChat(it) } ?: emptyList()
        val expectedCalls = case.expectedTools.map { ToolCallExpectation.parse(it) }
        val scores = EvaluationMetrics.toolScores(expectedCalls, actualCalls)
        val actualTools = actualCalls.map { it.tool }.distinct()
        val result = EvaluationRunResult(
            caseCode = case.code,
            chatId = chatId,
            feasible = feasible,
            totalSks = totalSks,
            precision = scores.precision,
            recall = scores.recall,
            f1 = scores.f1,
            solutionCorrect = isSolutionCorrect(case, feasible, totalSks, courseCodes),
            expectedTools = case.expectedTools,
            actualTools = actualTools
        )
        evaluationRepository.recordRun(result, notes)
        return result
    }

    fun summary(): EvaluationSummary = evaluationRepository.summary()

    /** Newest recorded runs, newest first, for the evaluation dashboard. */
    fun listRuns(limit: Int): List<EvaluationRunRecord> = evaluationRepository.listRuns(limit)

    /** Validates and stores one questionnaire; returns the computed score. */
    fun submitSurvey(nim: String, instrument: String, answers: Map<String, Int>): Double {
        if (studentUseCase.getStudentByNim(nim) == null) {
            throw BadRequestException("Mahasiswa dengan NIM $nim tidak ditemukan")
        }
        val parsed = runCatching { SurveyInstrument.valueOf(instrument.trim().uppercase()) }
            .getOrElse {
                throw BadRequestException(
                    "Instrumen harus salah satu dari: ${SurveyInstrument.entries.joinToString()}"
                )
            }
        val scores = try {
            SurveyScoring.score(parsed, answers)
        } catch (e: IllegalArgumentException) {
            throw BadRequestException(e.message ?: "Jawaban kuesioner tidak valid")
        }
        surveyRepository.save(nim, parsed, answers, scores)
        return scores.overall
    }

    fun surveySummary(): List<SurveySummary> = surveyRepository.summary()

    /** Newest questionnaire responses with every answer, newest first. */
    fun listSurveyResponses(limit: Int): List<SurveyResponseRecord> =
        surveyRepository.listResponses(limit)

    /**
     * A run is correct when the feasibility verdict matches, the SKS total is inside the
     * expected range, and every expected course is present in the proposed KRS.
     */
    private fun isSolutionCorrect(
        case: EvaluationCase,
        feasible: Boolean,
        totalSks: Int,
        courseCodes: List<String>
    ): Boolean {
        if (feasible != case.expectedFeasible) return false
        if (case.expectedMinSks != null && totalSks < case.expectedMinSks) return false
        if (case.expectedMaxSks != null && totalSks > case.expectedMaxSks) return false
        val proposed = courseCodes.toSet()
        return case.expectedCourseCodes.all { it in proposed }
    }

    private companion object {
        val SEMESTER_RANGE = 1..8
    }
}
