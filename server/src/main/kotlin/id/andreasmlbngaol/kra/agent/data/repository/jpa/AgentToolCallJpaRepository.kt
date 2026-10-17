package id.andreasmlbngaol.kra.agent.data.repository.jpa

import id.andreasmlbngaol.kra.agent.data.entity.AgentToolCallEntity
import id.andreasmlbngaol.kra.agent.domain.model.ToolCallStatus
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

/** Projection for per-tool aggregates. */
interface ToolUsageAggregate {
    val toolName: String
    val calls: Long
    val errors: Long
    val avgDuration: Double
}

interface AgentToolCallJpaRepository : JpaRepository<AgentToolCallEntity, UUID> {

    fun findAllByOrderByTsDesc(limit: Limit): List<AgentToolCallEntity>

    fun findByChatIdOrderBySeqAsc(chatId: UUID): List<AgentToolCallEntity>

    fun findByChatIdInOrderByChatIdAscSeqAsc(chatIds: Collection<UUID>): List<AgentToolCallEntity>

    @Query(
        """
        select t.toolName as toolName,
               count(t) as calls,
               coalesce(sum(case when t.status = :error then 1 else 0 end), 0) as errors,
               coalesce(avg(t.durationMs), 0) as avgDuration
        from AgentToolCallEntity t
        group by t.toolName
        order by count(t) desc
        """
    )
    fun usageByTool(@Param("error") error: ToolCallStatus): List<ToolUsageAggregate>
}
