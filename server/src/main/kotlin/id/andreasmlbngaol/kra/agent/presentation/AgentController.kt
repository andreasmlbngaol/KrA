package id.andreasmlbngaol.kra.agent.presentation

import id.andreasmlbngaol.kra.academic.domain.usecase.StudentUseCase
import id.andreasmlbngaol.kra.agent.data.KrsAgent
import ai.koog.prompt.llm.LLModel
import id.andreasmlbngaol.kra.agent.domain.model.AgentToolCallLog
import id.andreasmlbngaol.kra.agent.domain.model.TurnMetrics
import id.andreasmlbngaol.kra.agent.domain.repository.ChatHistoryRepository
import id.andreasmlbngaol.kra.agent.domain.repository.ToolCallLogRepository
import id.andreasmlbngaol.kra.agent.data.session.SessionTokenRegistry
import id.andreasmlbngaol.kra.agent.presentation.dto.ChatRequest
import id.andreasmlbngaol.kra.agent.presentation.dto.ChatStreamEvent
import id.andreasmlbngaol.kra.core.enums.ChatStreamEventType
import id.andreasmlbngaol.kra.core.error.ErrorMessages
import id.andreasmlbngaol.kra.core.error.friendlyError
import id.andreasmlbngaol.kra.core.error.looksLikeRawProviderError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.util.Collections
import kotlin.time.Duration.Companion.milliseconds

private val logger = LoggerFactory.getLogger("AgentController")

/** Compact (single-line) JSON for each NDJSON event. */
private val streamJson = Json { encodeDefaults = true }

/**
 * Agent chat endpoints under `/api/agent`: a non-streaming `/chat` fallback and a streaming
 * `/chat/stream` that emits NDJSON step/ping/final/error events.
 */
@RestController
@RequestMapping("/api/agent")
class AgentController(
    private val krsAgent: KrsAgent,
    private val studentUseCase: StudentUseCase,
    private val chatHistory: ChatHistoryRepository,
    private val toolCallLog: ToolCallLogRepository,
    private val llModel: LLModel,
    private val sessionTokens: SessionTokenRegistry
) {

    /** Non-streaming fallback: returns the agent's final reply as plain text. */
    @PostMapping("/chat")
    fun chat(@RequestBody request: ChatRequest, auth: Authentication): ResponseEntity<String> =
        runBlocking {
            if (request.message.isBlank()) {
                return@runBlocking ResponseEntity.badRequest().body("Message tidak boleh kosong")
            }
            val nim = auth.name
            val student = studentUseCase.getStudentByNim(nim)
                ?: return@runBlocking ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Data mahasiswa tidak ditemukan")

            val startedAt = System.currentTimeMillis()
            val steps = Collections.synchronizedList(mutableListOf<String>())
            val metrics = TurnMetrics()
            try {
                val response = krsAgent.run(
                    agentInput = buildContextualMessage(sessionTokens.issue(nim, request.sessionId), request.message),
                    sessionId = request.sessionId,
                    semester = student.semester,
                    metrics = metrics,
                    onStep = { steps.add(it) }
                )
                if (looksLikeRawProviderError(response)) {
                    logger.error("Agent mengembalikan error provider mentah: {}", response)
                    record(nim, request, ErrorMessages.AI_UNAVAILABLE, "error", false, startedAt, steps, metrics)
                    return@runBlocking ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ErrorMessages.AI_UNAVAILABLE)
                }
                val finalResponse = ensureQuestionAnswered(
                    agent = krsAgent,
                    sessionToken = sessionTokens.issue(nim, request.sessionId),
                    message = request.message,
                    response = response,
                    semester = student.semester,
                    sessionId = request.sessionId,
                    metrics = metrics
                )
                record(nim, request, finalResponse, "final", false, startedAt, steps, metrics)
                ResponseEntity.ok(finalResponse)
            } catch (e: Exception) {
                logger.error("Gagal memproses permintaan chat agent", e)
                val (status, message) = friendlyError(e)
                record(nim, request, message, "error", false, startedAt, steps, metrics)
                ResponseEntity.status(status).body(message)
            }
        }

    /**
     * Streaming version: streams each step (tool) as an NDJSON line in real time, then one
     * "final" event with the Markdown reply (or "error"). Heartbeat "ping" events keep the
     * connection alive during the long gap before the first token.
     */
    @PostMapping("/chat/stream", produces = ["application/x-ndjson"])
    fun chatStream(@RequestBody request: ChatRequest, auth: Authentication): ResponseEntity<StreamingResponseBody> {
        if (request.message.isBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val nim = auth.name
        val student = studentUseCase.getStudentByNim(nim)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val body = StreamingResponseBody { out ->
            // Commit the response & send the first byte IMMEDIATELY so the client (and any
            // proxy) sees activity before the first tool runs.
            //
            // NB: since Spring Framework 7.0.6 the stream handed to StreamingResponseBody
            // is non-flushing by default (ServletServerHttpResponse wraps it in
            // StreamUtils.nonFlushing), which silently buffers the whole reply. Flushing is
            // re-enabled by resources/spring.properties -> spring.http.response.flush.enabled.
            // Do not remove that file, or these flushes become no-ops again.
            out.write('\n'.code)
            out.flush()

            val startedAt = System.currentTimeMillis()
            val steps = Collections.synchronizedList(mutableListOf<String>())
            val metrics = TurnMetrics()

            runBlocking {
                val events = Channel<ChatStreamEvent>(Channel.UNLIMITED)

                val runJob = launch {
                    try {
                        val response = krsAgent.run(
                            agentInput = buildContextualMessage(sessionTokens.issue(nim, request.sessionId), request.message),
                            sessionId = request.sessionId,
                            semester = student.semester,
                            metrics = metrics,
                            onStep = { label ->
                                steps.add(label)
                                events.trySend(ChatStreamEvent(ChatStreamEventType.STEP, label = label))
                            },
                            onDraftSaved = {
                                events.trySend(ChatStreamEvent(ChatStreamEventType.DRAFT_SAVED))
                            }
                        )
                        if (looksLikeRawProviderError(response)) {
                            logger.error("Agent mengembalikan error provider mentah: {}", response)
                            record(nim, request, ErrorMessages.AI_UNAVAILABLE, "error", true, startedAt, steps, metrics)
                            events.send(ChatStreamEvent(ChatStreamEventType.ERROR, content = ErrorMessages.AI_UNAVAILABLE))
                        } else {
                            val finalResponse = ensureQuestionAnswered(
                                agent = krsAgent,
                                sessionToken = sessionTokens.issue(nim, request.sessionId),
                                message = request.message,
                                response = response,
                                semester = student.semester,
                                sessionId = request.sessionId,
                                metrics = metrics
                            )
                            record(nim, request, finalResponse, "final", true, startedAt, steps, metrics)
                            events.send(ChatStreamEvent(ChatStreamEventType.FINAL, content = finalResponse))
                        }
                    } catch (e: CancellationException) {
                        // Client closed the connection. Normal, not a failure: record the
                        // abandoned turn and rethrow so the coroutine actually cancels.
                        record(nim, request, "", "cancelled", true, startedAt, steps, metrics)
                        throw e
                    } catch (e: Exception) {
                        logger.error("Gagal memproses permintaan chat agent (stream)", e)
                        val (_, message) = friendlyError(e)
                        record(nim, request, message, "error", true, startedAt, steps, metrics)
                        events.send(ChatStreamEvent(ChatStreamEventType.ERROR, content = message))
                    } finally {
                        events.close()
                    }
                }

                // Heartbeat every 5s so the connection isn't considered idle during the gap
                // before the first token. Sent to the SAME channel -> only one writer.
                val heartbeat = launch {
                    try {
                        while (true) {
                            delay(5_000.milliseconds)
                            events.trySend(ChatStreamEvent(ChatStreamEventType.PING))
                        }
                    } catch (_: CancellationException) {
                        // stop silently when the stream finishes / is interrupted
                    }
                }

                try {
                    for (event in events) {
                        out.write(streamJson.encodeToString(event).toByteArray(Charsets.UTF_8))
                        out.write('\n'.code)
                        out.flush()
                    }
                } catch (e: Exception) {
                    // Client connection dropped while writing (IOException etc.): stop the
                    // agent silently; don't flood the log with a stacktrace.
                    logger.debug("Klien menutup stream chat sebelum selesai: {}", e.message)
                    heartbeat.cancel()
                    runJob.cancelAndJoin()
                    return@runBlocking
                }
                heartbeat.cancel()
                runJob.join()
            }
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/x-ndjson"))
            .body(body)
    }

    /** Records a chat turn plus its tool calls, computing the duration from [startedAt]. */
    private fun record(
        nim: String,
        request: ChatRequest,
        response: String,
        status: String,
        streamed: Boolean,
        startedAt: Long,
        steps: List<String>,
        metrics: TurnMetrics
    ) {
        val chatId = chatHistory.record(
            nim = nim,
            sessionId = request.sessionId,
            request = request.message,
            response = response,
            status = status,
            streamed = streamed,
            durationMs = System.currentTimeMillis() - startedAt,
            steps = steps.toList(),
            metrics = metrics,
            model = llModel.id
        )
        toolCallLog.recordAll(
            metrics.toolCalls.map {
                AgentToolCallLog(
                    chatId = chatId,
                    nim = nim,
                    sessionId = request.sessionId,
                    seq = it.seq,
                    toolName = it.toolName,
                    arguments = it.arguments,
                    status = it.status,
                    durationMs = it.durationMs,
                    resultSize = it.resultSize,
                    timestamp = it.timestamp
                )
            }
        )
    }
}

/** Injects the session token as system context; the NIM itself never reaches the LLM. */
private fun buildContextualMessage(sessionToken: String, message: String): String = buildString {
    appendLine("[KONTEKS SISTEM TERVERIFIKASI - Token sesi mahasiswa: $sessionToken.")
    appendLine("Gunakan token ini sebagai argumen sessionToken pada SEMUA pemanggilan tool; JANGAN meminta NIM atau token ke mahasiswa.]")
    appendLine()
    append(message)
    // Recency matters: the reminder goes AFTER the user message so the model reads it last.
    if (containsQuestion(message)) {
        appendLine()
        appendLine()
        appendLine("[INSTRUKSI WAJIB UNTUK GILIRAN INI]")
        appendLine("Pesan di atas mengandung pertanyaan. Sebelum menampilkan draft atau daftar matkul, jawab pertanyaan itu di kalimat pertama.")
        appendLine("Jika pertanyaannya berbentuk bisa/tidak atau boleh/tidak, awali dengan \"Bisa.\" atau \"Tidak bisa.\" lalu alasannya.")
        appendLine("DILARANG memulai jawaban dengan \"Draft\", \"Berikut\", atau \"Ini\" selama pertanyaan belum dijawab.")
        appendLine("Setelah pertanyaan dijawab, baru jalankan perintah dalam pesan.")
    }
}

private val QUESTION_WORD = Regex(
    "\\b(bisa|boleh|apakah|gimana|bagaimana|kenapa|mengapa|berapa|kok)\\b",
    RegexOption.IGNORE_CASE
)

/** True when the message asks something, even if it is mixed with a command. */
private fun containsQuestion(message: String): Boolean =
    message.contains('?') || QUESTION_WORD.containsMatchIn(message)

/** A reply that leads with a full draft instead of an answer. */
private fun looksLikeDraft(text: String): Boolean {
    val trimmed = text.trimStart()
    return trimmed.startsWith("Draft", ignoreCase = true) ||
        trimmed.startsWith("Berikut", ignoreCase = true) ||
        text.contains("**Wajib") ||
        text.contains("Total draft", ignoreCase = true)
}

/** True when the reply already opens with a direct yes/no answer. */
private fun alreadyAnswers(text: String): Boolean {
    val head = text.trimStart().take(160)
    return Regex("^(Bisa|Tidak\\s+bisa|Ya|Tidak)\\b", RegexOption.IGNORE_CASE)
        .containsMatchIn(head) ||
        Regex("\\b(tidak|nggak|gak)\\s+bisa\\b", RegexOption.IGNORE_CASE).containsMatchIn(head)
}

/** Keeps only the question sentences so the repair turn cannot trigger a new draft. */
private fun extractQuestions(message: String): String {
    val sentences = message.split(Regex("(?<=[.!?])\\s+"))
    val questions = sentences.filter { containsQuestion(it) }
    return if (questions.isEmpty()) message else questions.joinToString(" ")
}

/**
 * Small models tend to answer with the draft and forget the question embedded in the same
 * message. When that happens, run one focused question-only turn and prepend its answer, so
 * the student always gets a direct reply before the draft.
 */
private suspend fun ensureQuestionAnswered(
    agent: KrsAgent,
    sessionToken: String,
    message: String,
    response: String,
    semester: Int,
    sessionId: String,
    metrics: TurnMetrics
): String {
    if (!containsQuestion(message) || !looksLikeDraft(response) || alreadyAnswers(response)) {
        return response
    }
    val repairInput = buildContextualMessage(
        sessionToken,
        "[INSTRUKSI SISTEM: Ini hanya pertanyaan lanjutan. JANGAN menyusun draft dan " +
            "JANGAN menampilkan daftar matkul. Jawab pertanyaan berikut dalam 1-3 kalimat. " +
            "Jika pertanyaannya bisa/tidak, awali dengan \"Bisa.\" atau \"Tidak bisa.\".]\n\n" +
            extractQuestions(message)
    )
    val answer = try {
        agent.run(
            agentInput = repairInput,
            sessionId = "$sessionId-q-${System.currentTimeMillis()}",
            semester = semester,
            metrics = metrics
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.warn("Gagal menjawab pertanyaan lanjutan: {}", e.message)
        null
    }
    if (answer.isNullOrBlank() || looksLikeRawProviderError(answer)) return response
    return answer.trim() + "\n\n" + response
}
