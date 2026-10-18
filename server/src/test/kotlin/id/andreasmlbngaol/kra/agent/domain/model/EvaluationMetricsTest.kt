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
            expected = expected("getKhs", "solveKrs"),
            actual = listOf(call("getKhs"), call("solveKrs"))
        )
        assertEquals(1.0, s.precision)
        assertEquals(1.0, s.recall)
        assertEquals(1.0, s.f1)
    }

    @Test
    fun `repeated tool calls do not inflate precision`() {
        val s = EvaluationMetrics.toolScores(
            expected = expected("getKhs"),
            actual = listOf(call("getKhs"), call("getKhs"), call("getKhs"))
        )
        assertEquals(1.0, s.precision)
        assertEquals(1.0, s.recall)
    }

    @Test
    fun `extra tool lowers precision but keeps recall`() {
        val s = EvaluationMetrics.toolScores(
            expected = expected("getKhs", "solveKrs"),
            actual = listOf(call("getKhs"), call("solveKrs"), call("getCourseHistory"))
        )
        assertEquals(2.0 / 3.0, s.precision, 1e-9)
        assertEquals(1.0, s.recall)
        assertEquals(0.8, s.f1, 1e-9)
    }

    @Test
    fun `missing tool lowers recall but keeps precision`() {
        val s = EvaluationMetrics.toolScores(
            expected = expected("getKhs", "solveKrs"),
            actual = listOf(call("getKhs"))
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
        val s = EvaluationMetrics.toolScores(emptyList(), listOf(call("getKhs")))
        assertEquals(0.0, s.precision)
        assertEquals(1.0, s.recall)
        assertEquals(0.0, s.f1)
    }

    @Test
    fun `no calls when calls were expected scores zero`() {
        val s = EvaluationMetrics.toolScores(expected("solveKrs"), emptyList())
        assertEquals(0.0, s.precision)
        assertEquals(0.0, s.recall)
        assertEquals(0.0, s.f1)
    }

    @Test
    fun `a call with the wrong argument is a miss`() {
        val s = EvaluationMetrics.toolScores(
            expected = expected("getKhs?semester=4"),
            actual = listOf(call("getKhs", "semester" to "5"))
        )
        assertEquals(0.0, s.precision)
        assertEquals(0.0, s.recall)
    }

    @Test
    fun `a call with the right argument is a hit`() {
        val s = EvaluationMetrics.toolScores(
            expected = expected("getKhs?semester=4"),
            actual = listOf(call("getKhs", "semester" to "4"))
        )
        assertEquals(1.0, s.precision)
        assertEquals(1.0, s.recall)
    }

    @Test
    fun `argument values ignore case and surrounding spaces`() {
        val s = EvaluationMetrics.toolScores(
            expected = expected("solveKrs?studentClassCode=a"),
            actual = listOf(call("solveKrs", "studentClassCode" to " A "))
        )
        assertEquals(1.0, s.recall)
    }

    @Test
    fun `only the declared arguments are checked`() {
        val s = EvaluationMetrics.toolScores(
            expected = expected("getKhs?semester=4"),
            actual = listOf(call("getKhs", "semester" to "4", "nim" to "221401067"))
        )
        assertEquals(1.0, s.precision)
        assertEquals(1.0, s.recall)
    }

    @Test
    fun `a bare expectation accepts any arguments`() {
        val s = EvaluationMetrics.toolScores(
            expected = expected("getKhs"),
            actual = listOf(call("getKhs", "semester" to "4"))
        )
        assertEquals(1.0, s.recall)
    }

    @Test
    fun `the most specific expectation is matched first`() {
        val s = EvaluationMetrics.toolScores(
            expected = expected("solveKrs", "solveKrs?semester=4"),
            actual = listOf(
                call("solveKrs", "semester" to "4"),
                call("solveKrs", "semester" to "2")
            )
        )
        assertEquals(1.0, s.precision)
        assertEquals(1.0, s.recall)
    }

    @Test
    fun `a malformed expectation is rejected at parse time`() {
        assertThrows(IllegalArgumentException::class.java) {
            ToolCallExpectation.parse("getKhs?semester")
        }
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

    private fun expected(vararg entries: String): List<ToolCallExpectation> =
        entries.map { ToolCallExpectation.parse(it) }

    private fun call(tool: String, vararg arguments: Pair<String, String>): RecordedToolCall =
        RecordedToolCall(tool, arguments.toMap())
}
