package id.andreasmlbngaol.kra.agent.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

/** Composite key of [ChatHistoryEntity]. */
data class ChatHistoryId(
    var day: String = "",
    var id: UUID = UUID(0, 0)
) : Serializable

@Entity
@IdClass(ChatHistoryId::class)
@Table(name = "chat_history")
class ChatHistoryEntity(
    @Id
    @Column(name = "day", nullable = false, length = 10)
    var day: String = "",

    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID(0, 0),

    @Column(name = "ts", nullable = false)
    var ts: Instant = Instant.EPOCH,

    @Column(name = "nim", nullable = false, length = 9)
    var nim: String = "",

    @Column(name = "session_id", nullable = false, length = 64)
    var sessionId: String = "",

    @Column(name = "request", nullable = false, columnDefinition = "text")
    var request: String = "",

    @Column(name = "response", nullable = false, columnDefinition = "text")
    var response: String = "",

    @Column(name = "status", nullable = false, length = 16)
    var status: String = "",

    @Column(name = "streamed", nullable = false)
    var streamed: Boolean = false,

    @Column(name = "duration_ms", nullable = false)
    var durationMs: Long = 0,

    @Column(name = "steps", nullable = false, columnDefinition = "text")
    var steps: String = "",

    @Column(name = "llm_calls", nullable = false)
    var llmCalls: Int = 0,

    @Column(name = "tool_calls", nullable = false)
    var toolCalls: Int = 0,

    @Column(name = "input_tokens", nullable = false)
    var inputTokens: Long = 0,

    @Column(name = "output_tokens", nullable = false)
    var outputTokens: Long = 0,

    @Column(name = "total_tokens", nullable = false)
    var totalTokens: Long = 0,

    @Column(name = "model", length = 64)
    var model: String? = null
)
