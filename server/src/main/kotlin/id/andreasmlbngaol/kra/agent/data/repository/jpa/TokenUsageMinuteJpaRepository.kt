package id.andreasmlbngaol.kra.agent.data.repository.jpa

import id.andreasmlbngaol.kra.agent.data.entity.TokenUsageMinuteEntity
import id.andreasmlbngaol.kra.agent.data.entity.TokenUsageMinuteId
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface TokenUsageMinuteJpaRepository : JpaRepository<TokenUsageMinuteEntity, TokenUsageMinuteId> {
    fun findByMinuteBetweenOrderByMinuteAsc(start: Instant, end: Instant): List<TokenUsageMinuteEntity>
}
