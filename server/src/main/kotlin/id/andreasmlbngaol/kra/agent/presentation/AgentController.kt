package id.andreasmlbngaol.kra.agent.presentation

import id.andreasmlbngaol.kra.academic.domain.usecase.StudentUseCase
import id.andreasmlbngaol.kra.agent.data.KrsAgent
import id.andreasmlbngaol.kra.agent.domain.repository.ChatHistoryRepository
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
    private val chatHistory: ChatHistoryRepository
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
            try {
                val response = krsAgent.run(
                    agentInput = buildContextualMessage(nim, request.message),
                    sessionId = request.sessionId,
                    semester = student.semester,
                    onStep = { steps.add(it) }
                )
                if (looksLikeRawProviderError(response)) {
                    logger.error("Agent mengembalikan error provider mentah: {}", response)
                    record(nim, request, ErrorMessages.AI_UNAVAILABLE, "error", false, startedAt, steps)
                    return@runBlocking ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ErrorMessages.AI_UNAVAILABLE)
                }
                record(nim, request, response, "final", false, startedAt, steps)
                ResponseEntity.ok(response)
            } catch (e: Exception) {
                logger.error("Gagal memproses permintaan chat agent", e)
                val (status, message) = friendlyError(e)
                record(nim, request, message, "error", false, startedAt, steps)
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

            runBlocking {
                val events = Channel<ChatStreamEvent>(Channel.UNLIMITED)

                val runJob = launch {
                    try {
                        val response = krsAgent.run(
                            agentInput = buildContextualMessage(nim, request.message),
                            sessionId = request.sessionId,
                            semester = student.semester,
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
                            record(nim, request, ErrorMessages.AI_UNAVAILABLE, "error", true, startedAt, steps)
                            events.send(ChatStreamEvent(ChatStreamEventType.ERROR, content = ErrorMessages.AI_UNAVAILABLE))
                        } else {
                            record(nim, request, response, "final", true, startedAt, steps)
                            events.send(ChatStreamEvent(ChatStreamEventType.FINAL, content = response))
                        }
                    } catch (e: CancellationException) {
                        // Client closed the connection. Normal, not a failure: record the
                        // abandoned turn and rethrow so the coroutine actually cancels.
                        record(nim, request, "", "cancelled", true, startedAt, steps)
                        throw e
                    } catch (e: Exception) {
                        logger.error("Gagal memproses permintaan chat agent (stream)", e)
                        val (_, message) = friendlyError(e)
                        record(nim, request, message, "error", true, startedAt, steps)
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

    /** Records a chat turn, computing the duration from [startedAt]. */
    private fun record(
        nim: String,
        request: ChatRequest,
        response: String,
        status: String,
        streamed: Boolean,
        startedAt: Long,
        steps: List<String>
    ) {
        chatHistory.record(
            nim = nim,
            sessionId = request.sessionId,
            request = request.message,
            response = response,
            status = status,
            streamed = streamed,
            durationMs = System.currentTimeMillis() - startedAt,
            steps = steps.toList()
        )
    }
}

/** Injects the verified NIM as system context so the agent won't ask for it. */
private fun buildContextualMessage(nim: String, message: String): String = buildString {
    appendLine("[KONTEKS SISTEM TERVERIFIKASI - NIM mahasiswa yang sedang login: $nim.")
    appendLine("Gunakan NIM ini untuk semua pemanggilan tool; JANGAN meminta NIM ke mahasiswa.]")
    appendLine()
    append(message)
}
