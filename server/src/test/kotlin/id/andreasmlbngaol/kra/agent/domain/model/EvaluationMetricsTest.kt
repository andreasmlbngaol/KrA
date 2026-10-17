package id.andreasmlbngaol.kra.agent.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Pure tests for the evaluation arithmetic used by the proposal's metrics:
 * tool precision/recall/F1 and TAM/SUS scoring. No Spring, no database.
 */
class EvaluationMetricsTest {

    @Test
    fun `exact tool match scores perfect`() {
        val s = EvaluationMetrics.toolScores(
            expected = listOf("getKhs", "solveKrs"),
            actual = listOf("getKhs", "solveKrs")
        )
        assertEquals(1.0, s.precision)
        assertEquals(1.0, s.recall)
        assertEquals(1.0, s.f1)
    }

    @Test
    fun `repeated tool calls do not inflate precision`() {
        val s = EvaluationMetrics.toolScores(
            expected = listOf("getKhs"),
            actual = listOf("getKhs", "getKhs", "getKhs")
        )
        assertEquals(1.0, s.precision)
        assertEquals(1.0, s.recall)
    }

    @Test
    fun `extra tool lowers precision but keeps recall`() {
        val s = EvaluationMetrics.toolScores(
            expected = listOf("getKhs", "solveKrs"),
            actual = listOf("getKhs", "solveKrs", "getCourseHistory")
        )
        assertEquals(2.0 / 3.0, s.precision, 1e-9)
        assertEquals(1.0, s.recall)
        assertEquals(0.8, s.f1, 1e-9)
    }

    @Test
    fun `missing tool lowers recall but keeps precision`() {
        val s = EvaluationMetrics.toolScores(
            expected = listOf("getKhs", "solveKrs"),
            actual = listOf("getKhs")
        )
        assertEquals(1.0, s.precision)
        assertEquals(0.5, s.recall)
        assertEquals(2.0 / 3.0, s.f1, 1e-9)
    }

    @Test
    fun `empty expectation and empty actual is a perfect no-op`() {
        val s = EvaluationMetrics.toolScores(emptyList(), emptyList())
        assertEquals(1.0, s.precision)
        assertEquals(1.0, s.recall)
        assertEquals(1.0, s.f1)
    }

    @Test
    fun `unexpected calls with no expectation are pure noise`() {
        val s = EvaluationMetrics.toolScores(emptyList(), listOf("getKhs"))
        assertEquals(0.0, s.precision)
        assertEquals(1.0, s.recall)
        assertEquals(0.0, s.f1)
    }

    @Test
    fun `no calls when calls were expected scores zero`() {
        val s = EvaluationMetrics.toolScores(listOf("solveKrs"), emptyList())
        assertEquals(0.0, s.precision)
        assertEquals(0.0, s.recall)
        assertEquals(0.0, s.f1)
    }

    @Test
    fun `SUS with all positive answers scores 100`() {
        val answers = (1..10).associate { i ->
            val code = "SUS%02d".format(i)
            code to if (i % 2 == 1) 5 else 1
        }
        assertEquals(100.0, SurveyScoring.sus(answers))
    }

    @Test
    fun `SUS with all negative answers scores 0`() {
        val answers = (1..10).associate { i ->
            val code = "SUS%02d".format(i)
            code to if (i % 2 == 1) 1 else 5
        }
        assertEquals(0.0, SurveyScoring.sus(answers))
    }

    @Test
    fun `SUS with neutral answers scores 50`() {
        val answers = (1..10).associate { "SUS%02d".format(it) to 3 }
        assertEquals(50.0, SurveyScoring.sus(answers))
    }

    @Test
    fun `SUS requires all ten items`() {
        val answers = (1..9).associate { "SUS%02d".format(it) to 3 }
        assertThrows(IllegalArgumentException::class.java) { SurveyScoring.sus(answers) }
    }

    @Test
    fun `TAM averages each construct separately`() {
        val answers = mapOf("PU1" to 4, "PU2" to 5, "PEOU1" to 2, "PEOU2" to 3)
        val (pu, peou) = SurveyScoring.tam(answers)
        assertEquals(4.5, pu, 1e-9)
        assertEquals(2.5, peou, 1e-9)
    }

    @Test
    fun `TAM rejects a missing construct`() {
        assertThrows(IllegalArgumentException::class.java) {
            SurveyScoring.tam(mapOf("PU1" to 4))
        }
    }
}
