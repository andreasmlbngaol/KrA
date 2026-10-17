package id.andreasmlbngaol.kra.agent.domain.repository

import id.andreasmlbngaol.kra.agent.domain.model.EvaluationCase
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationRunRecord
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationRunResult
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationSummary
import id.andreasmlbngaol.kra.agent.domain.model.SurveyInstrument
import id.andreasmlbngaol.kra.agent.domain.model.SurveyResponseRecord
import id.andreasmlbngaol.kra.agent.domain.model.SurveyScores
import id.andreasmlbngaol.kra.agent.domain.model.SurveySummary

/** Ground-truth scenarios and the runs measured against them. */
interface EvaluationRepository {
    val isEnabled: Boolean

    fun saveCase(case: EvaluationCase): String

    fun findCase(code: String): EvaluationCase?

    fun listCases(): List<EvaluationCase>

    /** Tool names invoked in one chat turn, in order. */
    fun toolNamesForChat(chatId: String): List<String>

    fun recordRun(result: EvaluationRunResult, notes: String? = null)

    fun summary(): EvaluationSummary

    /** Newest runs, flattened with the case code and expected/actual tools. */
    fun listRuns(limit: Int): List<EvaluationRunRecord>
}

/**
 * TAM / SUS questionnaire storage. Validation and scoring are the caller's job
 * ([id.andreasmlbngaol.kra.agent.domain.model.SurveyScoring]); this only persists.
 */
interface SurveyRepository {
    val isEnabled: Boolean

    fun save(
        nim: String,
        instrument: SurveyInstrument,
        answers: Map<String, Int>,
        scores: SurveyScores
    )

    fun summary(): List<SurveySummary>

    /** Newest responses with every item answer, for the evaluation drill-down. */
    fun listResponses(limit: Int): List<SurveyResponseRecord>
}
