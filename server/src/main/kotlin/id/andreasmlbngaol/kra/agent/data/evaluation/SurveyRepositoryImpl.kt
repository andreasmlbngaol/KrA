package id.andreasmlbngaol.kra.agent.data.evaluation

import id.andreasmlbngaol.kra.agent.data.entity.SurveyAnswerEntity
import id.andreasmlbngaol.kra.agent.data.entity.SurveyResponseEntity
import id.andreasmlbngaol.kra.agent.data.repository.jpa.SurveyAnswerJpaRepository
import id.andreasmlbngaol.kra.agent.data.repository.jpa.SurveyResponseJpaRepository
import id.andreasmlbngaol.kra.agent.domain.model.SurveyInstrument
import id.andreasmlbngaol.kra.agent.domain.model.SurveyResponseRecord
import id.andreasmlbngaol.kra.agent.domain.model.SurveyScores
import id.andreasmlbngaol.kra.agent.domain.model.SurveySummary
import id.andreasmlbngaol.kra.agent.domain.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * PostgreSQL-backed TAM/SUS questionnaire store: one response row (with its computed
 * scores) plus one answer row per item, written in a single transaction.
 */
@Repository
class SurveyRepositoryImpl(
    private val responseRepo: SurveyResponseJpaRepository,
    private val answerRepo: SurveyAnswerJpaRepository
) : SurveyRepository {

    private val logger = LoggerFactory.getLogger("SurveyRepository")

    override val isEnabled: Boolean get() = true

    @Transactional
    override fun save(
        nim: String,
        instrument: SurveyInstrument,
        answers: Map<String, Int>,
        scores: SurveyScores
    ) {
        val saved = responseRepo.save(
            SurveyResponseEntity(
                nim = nim,
                instrument = instrument.name,
                submittedAt = Instant.now(),
                susScore = scores.sus?.toScore(),
                puScore = scores.pu?.toScore(),
                peouScore = scores.peou?.toScore()
            )
        )
        answerRepo.saveAll(
            answers.map { (item, value) ->
                SurveyAnswerEntity(responseId = saved.id, itemCode = item, likert = value.toShort())
            }
        )
    }

    @Transactional(readOnly = true)
    override fun summary(): List<SurveySummary> = SurveyInstrument.entries.map { instrument ->
        runCatching {
            val a = responseRepo.summary(instrument.name)
            SurveySummary(instrument, a.responses, a.avgScore.toDouble())
        }.getOrElse {
            logger.warn("Gagal menghitung agregat kuesioner {}: {}", instrument, it.message)
            SurveySummary(instrument, 0, 0.0)
        }
    }

    /** Newest responses plus every item answer, loaded in two queries. */
    @Transactional(readOnly = true)
    override fun listResponses(limit: Int): List<SurveyResponseRecord> = runCatching {
        val rows = responseRepo.findAllByOrderBySubmittedAtDesc(Limit.of(limit.coerceIn(1, 500)))
        if (rows.isEmpty()) return@runCatching emptyList()

        val answersByResponse = answerRepo.findByResponseIdIn(rows.map { it.id })
            .groupBy { it.responseId }

        rows.map { row ->
            val pu = row.puScore
            val peou = row.peouScore
            val score = row.susScore?.toDouble() ?: if (pu != null || peou != null) {
                ((pu ?: BigDecimal.ZERO) + (peou ?: BigDecimal.ZERO))
                    .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                    .toDouble()
            } else {
                0.0
            }
            SurveyResponseRecord(
                nim = row.nim,
                instrument = runCatching { SurveyInstrument.valueOf(row.instrument) }
                    .getOrDefault(SurveyInstrument.TAM),
                submittedAt = row.submittedAt,
                score = score,
                susScore = row.susScore?.toDouble(),
                puScore = row.puScore?.toDouble(),
                peouScore = row.peouScore?.toDouble(),
                answers = answersByResponse[row.id].orEmpty()
                    .associate { it.itemCode to it.likert.toInt() }
            )
        }
    }.getOrElse {
        logger.warn("Gagal membaca daftar respons kuesioner: {}", it.message)
        emptyList()
    }

    private fun Double.toScore(): BigDecimal =
        BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)
}
