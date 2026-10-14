package id.andreasmlbngaol.kra.agent.domain.repository

import id.andreasmlbngaol.kra.agent.domain.model.TokenUsageMinute
import java.time.Instant

/**
 * Storage for agent token-usage metrics (observability).
 *
 * Every operation must be safe to call even when the storage backend is unavailable:
 * writes become no-ops and reads return empty.
 */
interface TokenUsageRepository {
    /** true when the storage backend is ready to use. */
    val isEnabled: Boolean

    /** Persists one minute's aggregate. */
    fun record(minute: TokenUsageMinute)

    /** Returns all minute rows within [start, end] (inclusive, UTC), ordered ascending. */
    fun queryRange(start: Instant, end: Instant): List<TokenUsageMinute>
}
