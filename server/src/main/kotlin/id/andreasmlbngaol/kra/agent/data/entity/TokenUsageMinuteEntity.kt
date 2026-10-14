package id.andreasmlbngaol.kra.agent.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

/** Composite key of [TokenUsageMinuteEntity]. */
data class TokenUsageMinuteId(
    var day: String = "",
    var minute: Instant = Instant.EPOCH
) : Serializable

@Entity
@IdClass(TokenUsageMinuteId::class)
@Table(name = "token_usage_minute")
class TokenUsageMinuteEntity(
    @Id
    @Column(name = "day", nullable = false, length = 10)
    var day: String = "",

    @Id
    @Column(name = "minute", nullable = false)
    var minute: Instant = Instant.EPOCH,

    @Column(name = "llm_requests", nullable = false)
    var llmRequests: Long = 0,

    @Column(name = "input_tokens", nullable = false)
    var inputTokens: Long = 0,

    @Column(name = "output_tokens", nullable = false)
    var outputTokens: Long = 0,

    @Column(name = "total_tokens", nullable = false)
    var totalTokens: Long = 0,

    @Column(name = "tool_calls", nullable = false)
    var toolCalls: Long = 0,

    @Column(name = "tool_breakdown", nullable = false, columnDefinition = "text")
    var toolBreakdown: String = ""
)
