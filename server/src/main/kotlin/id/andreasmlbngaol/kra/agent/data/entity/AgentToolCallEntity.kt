package id.andreasmlbngaol.kra.agent.data.entity

import id.andreasmlbngaol.kra.agent.domain.model.ToolCallStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/** One tool invocation performed by the agent. */
@Entity
@Table(name = "agent_tool_call")
class AgentToolCallEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "day", nullable = false, length = 10)
    var day: String = "",

    @Column(name = "chat_id", nullable = false)
    var chatId: UUID = UUID(0, 0),

    @Column(name = "ts", nullable = false)
    var ts: Instant = Instant.EPOCH,

    @Column(name = "nim", nullable = false, length = 9)
    var nim: String = "",

    @Column(name = "session_id", nullable = false, length = 64)
    var sessionId: String = "",

    @Column(name = "seq", nullable = false)
    var seq: Int = 0,

    @Column(name = "tool_name", nullable = false, length = 64)
    var toolName: String = "",

    @Column(name = "arguments", nullable = false, columnDefinition = "text")
    var arguments: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: ToolCallStatus = ToolCallStatus.OK,

    @Column(name = "duration_ms", nullable = false)
    var durationMs: Long = 0,

    @Column(name = "result_size", nullable = false)
    var resultSize: Int = 0
)
