package id.andreasmlbngaol.kra.agent.domain.model

/** Scores computed from one questionnaire submission. */
data class SurveyScores(
    val sus: Double? = null,
    val pu: Double? = null,
    val peou: Double? = null
) {
    /** Single headline number: the SUS score, or the mean of the two TAM constructs. */
    val overall: Double
        get() = sus ?: ((pu ?: 0.0) + (peou ?: 0.0)) / 2
}

/**
 * Pure questionnaire scoring, kept outside the repository so it can be unit-tested and so
 * the stored score always matches the answers.
 */
object SurveyScoring {

    private val SUS_ITEMS = (1..10).map { "SUS%02d".format(it) }
    val LIKERT_RANGE = 1..5

    /** Validates the answers and scores them according to the instrument. */
    fun score(instrument: SurveyInstrument, answers: Map<String, Int>): SurveyScores {
        require(answers.isNotEmpty()) { "Jawaban kuesioner tidak boleh kosong" }
        answers.forEach { (item, value) ->
            require(value in LIKERT_RANGE) { "Nilai $item harus 1..5, bukan $value" }
        }
        return when (instrument) {
            SurveyInstrument.SUS -> SurveyScores(sus = sus(answers))
            SurveyInstrument.TAM -> {
                val (pu, peou) = tam(answers)
                SurveyScores(pu = pu, peou = peou)
            }
        }
    }

    /**
     * Standard SUS scoring: items 1,3,5,7,9 contribute (score - 1), items 2,4,6,8,10
     * contribute (5 - score). The sum (0..40) is multiplied by 2.5 to reach 0..100.
     */
    fun sus(answers: Map<String, Int>): Double {
        val ordered = SUS_ITEMS.map { item ->
            answers[item] ?: throw IllegalArgumentException("Item $item belum dijawab")
        }
        val total = ordered.mapIndexed { index, value ->
            if (index % 2 == 0) value - 1 else 5 - value
        }.sum()
        return total * 2.5
    }

    /** Mean per TAM construct (perceived usefulness, perceived ease of use), scale 1..5. */
    fun tam(answers: Map<String, Int>): Pair<Double, Double> {
        val pu = answers.filterKeys { it.startsWith("PU") }.values
        val peou = answers.filterKeys { it.startsWith("PEOU") }.values
        require(pu.isNotEmpty()) { "Item PU belum dijawab" }
        require(peou.isNotEmpty()) { "Item PEOU belum dijawab" }
        return pu.average() to peou.average()
    }
}
