package id.andreasmlbngaol.kra.agent.domain.model

/** Precision/recall/F1 of one set of tool calls against the expected set. */
data class ToolScores(
    val precision: Double,
    val recall: Double,
    val f1: Double
)

/**
 * Set-based comparison between the tools a case expects and the tools the agent actually
 * invoked in one turn.
 *
 * Repeated calls of the same tool are collapsed (a second `getKhs` is not a new hit), and
 * the degenerate cases are defined explicitly so the aggregate is always meaningful:
 * - expected empty, actual empty  -> 1.0 / 1.0 / 1.0 (nothing was required, nothing wasted)
 * - expected empty, actual filled -> 0.0 / 1.0 / 0.0 (pure noise)
 * - expected filled, actual empty -> 0.0 / 0.0 / 0.0 (nothing happened)
 */
object EvaluationMetrics {

    fun toolScores(expected: Collection<String>, actual: Collection<String>): ToolScores {
        val expectedSet = expected.toSet()
        val actualSet = actual.toSet()
        val hits = expectedSet.intersect(actualSet).size

        val precision = when {
            actualSet.isEmpty() -> if (expectedSet.isEmpty()) 1.0 else 0.0
            else -> hits.toDouble() / actualSet.size
        }
        val recall = if (expectedSet.isEmpty()) 1.0 else hits.toDouble() / expectedSet.size
        val f1 = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)

        return ToolScores(precision, recall, f1)
    }
}
