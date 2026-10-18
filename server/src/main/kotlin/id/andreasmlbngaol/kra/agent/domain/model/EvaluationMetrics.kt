package id.andreasmlbngaol.kra.agent.domain.model

/** Precision/recall/F1 of one set of tool calls against the expected set. */
data class ToolScores(
    val precision: Double,
    val recall: Double,
    val f1: Double
)

/**
 * Matching between the tool calls a case expects and the calls the agent actually made.
 *
 * A call counts as a hit only when the tool name matches *and* every argument written in
 * the expectation is present with the same value. Argument values are compared after
 * trimming and case folding, because the model may send "ILK3101" or "ilk3101" and both
 * mean the same course.
 *
 * Expectations are matched most-specific first, so a case that expects both `solveKrs` and
 * `solveKrs?semester=4` cannot have its specific expectation consumed by the bare one.
 * Repeated identical calls are collapsed, which keeps the score about *which* calls were
 * made rather than how often the model retried.
 *
 * Degenerate cases are defined explicitly so the aggregate is always meaningful:
 * - expected empty, actual empty  -> 1.0 / 1.0 / 1.0 (nothing was required, nothing wasted)
 * - expected empty, actual filled -> 0.0 / 1.0 / 0.0 (pure noise)
 * - expected filled, actual empty -> 0.0 / 0.0 / 0.0 (nothing happened)
 */
object EvaluationMetrics {

    fun toolScores(
        expected: Collection<ToolCallExpectation>,
        actual: Collection<RecordedToolCall>
    ): ToolScores {
        val expectedCalls = expected.distinct().sortedByDescending { it.specificity }
        val actualCalls = actual.distinct()

        val used = BooleanArray(actualCalls.size)
        var hits = 0
        for (expectation in expectedCalls) {
            val index = actualCalls.indices.firstOrNull { i ->
                !used[i] && matches(expectation, actualCalls[i])
            }
            if (index != null) {
                used[index] = true
                hits++
            }
        }

        val precision = when {
            actualCalls.isEmpty() -> if (expectedCalls.isEmpty()) 1.0 else 0.0
            else -> hits.toDouble() / actualCalls.size
        }
        val recall = if (expectedCalls.isEmpty()) 1.0 else hits.toDouble() / expectedCalls.size
        val f1 = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)

        return ToolScores(precision, recall, f1)
    }

    private fun matches(expectation: ToolCallExpectation, call: RecordedToolCall): Boolean {
        if (!expectation.tool.equals(call.tool, ignoreCase = true)) return false
        return expectation.arguments.all { (key, value) ->
            call.arguments.any { (actualKey, actualValue) ->
                actualKey.equals(key, ignoreCase = true) &&
                    actualValue.trim().equals(value.trim(), ignoreCase = true)
            }
        }
    }
}
