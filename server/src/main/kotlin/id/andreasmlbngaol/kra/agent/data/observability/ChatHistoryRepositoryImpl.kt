package id.andreasmlbngaol.kra.agent.data.observability

import id.andreasmlbngaol.kra.agent.data.entity.ChatHistoryEntity
import id.andreasmlbngaol.kra.agent.data.repository.jpa.ChatHistoryJpaRepository
import id.andreasmlbngaol.kra.agent.domain.model.ChatLogEntry
import id.andreasmlbngaol.kra.agent.domain.repository.ChatHistoryRepository
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/**
 * PostgreSQL-backed implementation of [ChatHistoryRepository].
 *
 * Writes are fire-and-forget on a dedicated executor, so the request path is never
 * blocked or broken by logging (and an abandoned/cancelled turn can still be recorded).
 * Every operation is safe when the database is unavailable: writes become no-ops
 * and reads return empty.
 */
@Repository
class ChatHistoryRepositoryImpl(
    private val repo: ChatHistoryJpaRepository
) : ChatHistoryRepository {
    private val log = LoggerFactory.getLogger("ChatHistoryRepository")
    private val json = Json { ignoreUnknownKeys = true }
    private val executor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "chat-history-writer").apply { isDaemon = true }
    }

    override val isEnabled: Boolean get() = true

    private fun dayOf(ts: Instant): String = ts.atZone(ZoneOffset.UTC).toLocalDate().toString()

    override fun record(
        nim: String,
        sessionId: String,
        request: String,
        response: String,
        status: String,
        streamed: Boolean,
        durationMs: Long,
        steps: List<String>
    ) {
        val now = Instant.now()
        val entity = ChatHistoryEntity(
            day = dayOf(now),
            id = UUID.randomUUID(),
            ts = now,
            nim = nim,
            sessionId = sessionId,
            request = request,
            response = response,
            status = status,
            streamed = streamed,
            durationMs = durationMs,
            steps = json.encodeToString(steps)
        )
        executor.execute {
            try {
                repo.save(entity)
            } catch (e: Exception) {
                log.warn("Gagal menyimpan chat turn ke PostgreSQL: {}", e.message)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun queryRecent(limit: Int): List<ChatLogEntry> {
        val capped = limit.coerceIn(1, 1000)
        return try {
            repo.findAllByOrderByTsDesc(Limit.of(capped)).map {
                val steps = if (it.steps.isNotBlank()) {
                    json.decodeFromString<List<String>>(it.steps)
                } else {
                    emptyList()
                }
                ChatLogEntry(
                    id = it.id.toString(),
                    timestamp = it.ts,
                    nim = it.nim,
                    sessionId = it.sessionId,
                    request = it.request,
                    response = it.response,
                    status = it.status,
                    streamed = it.streamed,
                    durationMs = it.durationMs,
                    steps = steps
                )
            }
        } catch (e: Exception) {
            log.warn("Gagal membaca chat history dari PostgreSQL: {}", e.message)
            emptyList()
        }
    }
}
