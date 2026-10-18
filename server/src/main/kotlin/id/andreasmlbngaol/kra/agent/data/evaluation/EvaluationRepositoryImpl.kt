package id.andreasmlbngaol.kra.agent.data.evaluation

import id.andreasmlbngaol.kra.agent.data.entity.EvaluationCaseEntity
import id.andreasmlbngaol.kra.agent.data.entity.EvaluationRunEntity
import id.andreasmlbngaol.kra.agent.data.repository.jpa.AgentToolCallJpaRepository
import id.andreasmlbngaol.kra.agent.data.repository.jpa.EvaluationCaseJpaRepository
import id.andreasmlbngaol.kra.agent.data.repository.jpa.EvaluationRunJpaRepository
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationCase
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationRunRecord
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationRunResult
import id.andreasmlbngaol.kra.agent.domain.model.EvaluationSummary
import id.andreasmlbngaol.kra.agent.domain.model.RecordedToolCall
import id.andreasmlbngaol.kra.agent.domain.repository.EvaluationRepository
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/**
 * PostgreSQL-backed evaluation store.
 *
 * Unlike telemetry, authoring operations (saving a case, recording a run) surface their
 * errors: a silently dropped ground-truth row would invalidate the measured accuracy.
 * Read-only aggregates still degrade to zero values so the dashboard never breaks.
 */
@Repository
class EvaluationRepositoryImpl(
    private val caseRepo: EvaluationCaseJpaRepository,
    private val runRepo: EvaluationRunJpaRepository,
    private val toolCallRepo: AgentToolCallJpaRepository
) : EvaluationRepository {

    private val logger = LoggerFactory.getLogger("EvaluationRepository")
    private val json = Json { ignoreUnknownKeys = true }

    override val isEnabled: Boolean get() = true

    override fun saveCase(case: EvaluationCase): String {
        val entity = caseRepo.findByCode(case.code)
            ?: EvaluationCaseEntity(code = case.code, createdAt = Instant.now())
        entity.nim = case.nim
        entity.semester = case.semester
        entity.description = case.description
        entity.expectedFeasible = case.expectedFeasible
        entity.expectedMinSks = case.expectedMinSks
        entity.expectedMaxSks = case.expectedMaxSks
        entity.expectedTools = encodeCodes(case.expectedTools)
        entity.expectedCourseCodes = encodeCodes(case.expectedCourseCodes)
        return caseRepo.save(entity).id.toString()
    }

    override fun findCase(code: String): EvaluationCase? = runCatching {
        caseRepo.findByCode(code)?.toDomain()
    }.getOrElse {
        logger.warn("Gagal membaca case evaluasi {}: {}", code, it.message)
        null
    }

    override fun listCases(): List<EvaluationCase> = runCatching {
        caseRepo.findAllByActiveTrue().map { it.toDomain() }
    }.getOrElse {
        logger.warn("Gagal membaca daftar case evaluasi: {}", it.message)
        emptyList()
    }

    override fun toolCallsForChat(chatId: String): List<RecordedToolCall> {
        val id = runCatching { UUID.fromString(chatId) }.getOrNull() ?: return emptyList()
        return runCatching {
            toolCallRepo.findByChatIdOrderBySeqAsc(id)
                .map { RecordedToolCall(it.toolName, parseArguments(it.arguments)) }
                .distinct()
        }.getOrElse {
            logger.warn("Gagal membaca tool call untuk chat {}: {}", chatId, it.message)
            emptyList()
        }
    }

    /**
     * Flattens the stored argument JSON into a string map. Tools only take scalar
     * arguments, so nested objects are not expected; anything unparsable is treated as
     * "no arguments" rather than failing the whole measurement.
     */
    private fun parseArguments(raw: String): Map<String, String> = runCatching {
        json.parseToJsonElement(raw).jsonObject.mapNotNull { (key, value) ->
            (value as? JsonPrimitive)?.let { key to it.content }
        }.toMap()
    }.getOrDefault(emptyMap())

    override fun recordRun(result: EvaluationRunResult, notes: String?) {
        // The use case resolves the case first; a miss here means the row vanished mid-flight.
        val case = checkNotNull(caseRepo.findByCode(result.caseCode)) {
            "Case evaluasi '${result.caseCode}' hilang saat run dicatat"
        }
        runRepo.save(
            EvaluationRunEntity(
                caseId = case.id,
                chatId = result.chatId?.let { runCatching { UUID.fromString(it) }.getOrNull() },
                ts = Instant.now(),
                feasible = result.feasible,
                totalSks = result.totalSks,
                toolPrecision = result.precision.toScale(),
                toolRecall = result.recall.toScale(),
                toolF1 = result.f1.toScale(),
                solutionCorrect = result.solutionCorrect,
                notes = notes
            )
        )
    }

    @Transactional(readOnly = true)
    override fun summary(): EvaluationSummary = runCatching {
        val a = runRepo.summary()
        EvaluationSummary(
            totalRuns = a.total,
            correctRuns = a.correct,
            solutionAccuracy = a.accuracy.toDouble(),
            avgPrecision = a.avgPrecision.toDouble(),
            avgRecall = a.avgRecall.toDouble(),
            avgF1 = a.avgF1.toDouble()
        )
    }.getOrElse {
        logger.warn("Gagal menghitung agregat evaluasi: {}", it.message)
        EvaluationSummary(0, 0, 0.0, 0.0, 0.0, 0.0)
    }

    /**
     * Newest runs, enriched with the case code, the expected tools, and the tools that were
     * actually invoked in the run chat. Loaded in three queries, not per row.
     */
    @Transactional(readOnly = true)
    override fun listRuns(limit: Int): List<EvaluationRunRecord> = runCatching {
        val runs = runRepo.findAllByOrderByTsDesc(Limit.of(limit.coerceIn(1, 500)))
        if (runs.isEmpty()) return@runCatching emptyList()

        val cases = caseRepo.findAllById(runs.map { it.caseId }.distinct()).associateBy { it.id }
        val chatIds = runs.mapNotNull { it.chatId }.distinct()
        val toolsByChat = if (chatIds.isEmpty()) {
            emptyMap<UUID, List<String>>()
        } else {
            toolCallRepo.findByChatIdInOrderByChatIdAscSeqAsc(chatIds)
                .groupBy { it.chatId }
                .mapValues { (_, calls) -> calls.sortedBy { it.seq }.map { it.toolName }.distinct() }
        }

        runs.mapNotNull { run ->
            val case = cases[run.caseId] ?: return@mapNotNull null
            EvaluationRunRecord(
                caseCode = case.code,
                chatId = run.chatId?.toString(),
                timestamp = run.ts,
                feasible = run.feasible,
                totalSks = run.totalSks,
                precision = run.toolPrecision?.toDouble() ?: 0.0,
                recall = run.toolRecall?.toDouble() ?: 0.0,
                f1 = run.toolF1?.toDouble() ?: 0.0,
                solutionCorrect = run.solutionCorrect,
                expectedTools = decodeCodes(case.expectedTools),
                actualTools = run.chatId?.let { toolsByChat[it] }.orEmpty(),
                notes = run.notes
            )
        }
    }.getOrElse {
        logger.warn("Gagal membaca daftar run evaluasi: {}", it.message)
        emptyList()
    }

    private fun EvaluationCaseEntity.toDomain() = EvaluationCase(
        code = code,
        nim = nim,
        semester = semester,
        description = description,
        expectedFeasible = expectedFeasible,
        expectedMinSks = expectedMinSks,
        expectedMaxSks = expectedMaxSks,
        expectedTools = decodeCodes(expectedTools),
        expectedCourseCodes = decodeCodes(expectedCourseCodes),
        active = active,
        createdAt = createdAt
    )

    private fun encodeCodes(values: List<String>): String =
        json.encodeToString(ListSerializer(String.serializer()), values)

    private fun decodeCodes(raw: String): List<String> =
        runCatching { json.decodeFromString(ListSerializer(String.serializer()), raw) }
            .getOrElse { emptyList() }

    /** Column is numeric(5,4); values outside 0..1 are clamped so the insert never fails. */
    private fun Double.toScale(): BigDecimal =
        BigDecimal.valueOf(coerceIn(0.0, 1.0)).setScale(4, RoundingMode.HALF_UP)
}
