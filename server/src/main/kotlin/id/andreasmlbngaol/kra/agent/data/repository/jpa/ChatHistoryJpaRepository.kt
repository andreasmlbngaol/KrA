package id.andreasmlbngaol.kra.agent.data.repository.jpa

import id.andreasmlbngaol.kra.agent.data.entity.ChatHistoryEntity
import id.andreasmlbngaol.kra.agent.data.entity.ChatHistoryId
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository

interface ChatHistoryJpaRepository : JpaRepository<ChatHistoryEntity, ChatHistoryId> {
    fun findAllByOrderByTsDesc(limit: Limit): List<ChatHistoryEntity>
}
