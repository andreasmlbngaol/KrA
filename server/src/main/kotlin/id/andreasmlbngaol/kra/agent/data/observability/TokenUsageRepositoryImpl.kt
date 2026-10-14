package id.andreasmlbngaol.kra.agent.data.observability

import id.andreasmlbngaol.kra.agent.data.entity.TokenUsageMinuteEntity
import id.andreasmlbngaol.kra.agent.data.repository.jpa.TokenUsageMinuteJpaRepository
import id.andreasmlbngaol.kra.agent.domain.model.TokenUsageMinute
import id.andreasmlbngaol.kra.agent.domain.repository.TokenUsageRepository
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset

/**
 * PostgreSQL-backed implementation of [TokenUsageRepository].
 *
 * Every operation is safe when the database is unavailable: writes become no-ops
 * and reads return empty.
 */
@Repository
@Transactional
class TokenUsageRepositoryImpl(
    private val repo: TokenUsageMinuteJpaRepository
) : TokenUsageRepository {
    private val log = LoggerFactory.getLogger("TokenUsageRepository")
    private val json = Json { ignoreUnknownKeys = true }

    override val isEnabled: Boolean get() = true

    private fun dayOf(instant: Instant): String =
        instant.atZone(ZoneOffset.UTC).toLocalDate().toString()

    override fun record(minute: TokenUsageMinute) {
        try {
            val day = dayOf(minute.minute)
            val id = id.andreasmlbngaol.kra.agent.data.entity.TokenUsageMinuteId(day, minute.minute)
            val existing = repo.findById(id).orElse(null)
            val mergedBreakdown = if (existing != null) {
                val prior = decode(existing.toolBreakdown)
                (prior.keys + minute.toolBreakdown.keys)
                    .associateWith { (prior[it] ?: 0L) + (minute.toolBreakdown[it] ?: 0L) }
                    .filterValues { it != 0L }
            } else {
                minute.toolBreakdown
            }

            val entity = existing ?: TokenUsageMinuteEntity(
                day = day,
                minute = minute.minute,
                llmRequests = 0,
                inputTokens = 0,
                outputTokens = 0,
                totalTokens = 0,
                toolCalls = 0,
                toolBreakdown = "{}"
            )
            entity.llmRequests += minute.llmRequests
            entity.inputTokens += minute.inputTokens
            entity.outputTokens += minute.outputTokens
            entity.totalTokens += minute.totalTokens
            entity.toolCalls += minute.toolCalls
            entity.toolBreakdown = json.encodeToString(mergedBreakdown)
            repo.save(entity)
        } catch (e: Exception) {
            log.warn("Gagal simpan metrik token ke PostgreSQL: {}", e.message)
        }
    }

    @Transactional(readOnly = true)
    override fun queryRange(start: Instant, end: Instant): List<TokenUsageMinute> =
        try {
            repo.findByMinuteBetweenOrderByMinuteAsc(start, end).map {
                TokenUsageMinute(
                    minute = it.minute,
                    llmRequests = it.llmRequests,
                    inputTokens = it.inputTokens,
                    outputTokens = it.outputTokens,
                    totalTokens = it.totalTokens,
                    toolCalls = it.toolCalls,
                    toolBreakdown = decode(it.toolBreakdown)
                )
            }
        } catch (e: Exception) {
            log.warn("Gagal baca metrik token dari PostgreSQL: {}", e.message)
            emptyList()
        }

    private fun decode(raw: String): Map<String, Long> =
        if (raw.isBlank()) emptyMap() else json.decodeFromString(raw)
}
