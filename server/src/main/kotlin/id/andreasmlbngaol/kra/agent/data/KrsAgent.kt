package id.andreasmlbngaol.kra.agent.data

import ai.koog.agents.chatMemory.feature.ChatMemory
import ai.koog.agents.chatMemory.feature.InMemoryChatHistoryProvider
import ai.koog.agents.core.agent.GraphAIAgent
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import id.andreasmlbngaol.kra.academic.domain.usecase.KrsUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.KrsValidationUseCase
import id.andreasmlbngaol.kra.agent.data.observability.TokenUsageTracker
import id.andreasmlbngaol.kra.agent.domain.model.ToolCallStatus
import id.andreasmlbngaol.kra.agent.domain.model.TurnMetrics
import id.andreasmlbngaol.kra.agent.data.tools.CourseToolSet
import id.andreasmlbngaol.kra.agent.data.tools.KrsCspToolSet
import id.andreasmlbngaol.kra.agent.data.tools.KhsToolSet
import id.andreasmlbngaol.kra.agent.data.tools.KrsToolSet
import id.andreasmlbngaol.kra.agent.data.tools.StudentInfoToolSet
import org.springframework.stereotype.Service

/**
 * Orchestrates the KRS assistant agent (koog [AIAgent]): assembles the system prompt from
 * prompt modules, wires the academic tool sets, and runs a chat turn. A fresh agent is
 * built per request so the per-request onDraftSaved/onToolStep callbacks stay isolated,
 * while chat memory is shared across requests.
 */
@Service
class KrsAgent(
    private val llmClient: LLMClient,
    private val llModel: LLModel,
    private val studentInfoToolSet: StudentInfoToolSet,
    private val khsToolSet: KhsToolSet,
    private val courseToolSet: CourseToolSet,
    private val krsCspToolSet: KrsCspToolSet,
    // Raw use cases injected so KrsToolSet can be built PER-REQUEST (see buildAgent),
    // keeping the onDraftSaved callback isolated per request.
    private val krsUseCase: KrsUseCase,
    private val krsValidationUseCase: KrsValidationUseCase,
    private val tokenUsageTracker: TokenUsageTracker
) {

    /**
     * The system prompt is split into modules under `resources/prompt/`. Only universal
     * rules + the relevant semester flow are included, to keep the prompt small per-request.
     */
    private val universalModules = listOf(
        "00-identitas-dan-alur-wajib.md",
        "04-aturan-umum.md",
        "05-aturan-kritis.md",
        "06-aturan-draft-krs.md",
        "07-format-tampilan-rekomendasi.md",
        "08-gaya-jawaban.md"
    )

    private val semesterFlowModules = mapOf(
        1 to "01-alur-semester-1-2.md",
        2 to "01-alur-semester-1-2.md",
        3 to "02-alur-semester-3-5.md",
        4 to "02-alur-semester-3-5.md",
        5 to "02-alur-semester-3-5.md",
        6 to "03-alur-semester-6-8.md",
        7 to "03-alur-semester-6-8.md",
        8 to "03-alur-semester-6-8.md"
    )

    /** Builds the system prompt: universal rules + the relevant semester flow file. */
    private fun buildSystemPrompt(semester: Int): String {
        val flowFile = semesterFlowModules[semester]
            ?: error("Semester $semester tidak valid (harus 1-8)")
        val modules = (universalModules + flowFile).joinToString(separator = "\n\n---\n\n") { module ->
            KrsAgent::class.java.getResourceAsStream("/prompt/$module")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?.trim()
                ?: error("Modul prompt /prompt/$module tidak ditemukan di classpath resources")
        }
        // Closing rule: it sits at the very end of the system prompt, right before the
        // conversation, because a small model otherwise forgets the question gate once the
        // tool results arrive.
        return modules + "\n\n---\n\n" + CLOSING_RULE
    }

    private companion object {
        val CLOSING_RULE = """
            ## Aturan Penutup (selalu berlaku)

            - Jika pesan mahasiswa mengandung pertanyaan (tanda tanya atau kata tanya), jawab pertanyaan itu di **kalimat pertama** sebelum menampilkan draft atau daftar matkul.
            - Untuk pertanyaan bisa/tidak atau boleh/tidak, awali dengan **"Bisa."** atau **"Tidak bisa."** lalu alasannya.
            - **Dilarang memulai jawaban dengan "Draft", "Berikut", atau "Ini"** selama pertanyaan belum dijawab.
            - Jika pesan hanya pertanyaan, berhenti setelah menjawab. Jika ada perintah juga, jalankan setelah pertanyaan dijawab.
        """.trimIndent()
    }

    // Shared across requests so per-session memory survives even though the agent instance
    // is rebuilt for every message (to inject the step/tool reporter into handleEvents).
    private val chatHistoryProvider = InMemoryChatHistoryProvider()

    /** Builds an agent instance for a SINGLE chat request. */
    private fun buildAgent(
        systemPrompt: String,
        metrics: TurnMetrics,
        onToolStep: (String) -> Unit,
        onDraftSaved: () -> Unit
    ): GraphAIAgent<String, String> {
        // KrsToolSet is intentionally NOT a singleton: built per-request so the onDraftSaved
        // callback applies only to the current request.
        val krsToolSet = KrsToolSet(
            krsUseCase = krsUseCase,
            krsValidationUseCase = krsValidationUseCase,
            onDraftSaved = onDraftSaved
        )

        val toolRegistry = ToolRegistry {
            tools(
                studentInfoToolSet.asTools() +
                    khsToolSet.asTools() +
                    courseToolSet.asTools() +
                    krsCspToolSet.asTools() +
                    krsToolSet.asTools()
            )
        }

        return AIAgent(
            promptExecutor = MultiLLMPromptExecutor(llmClient),
            llmModel = llModel,
            systemPrompt = systemPrompt,
            toolRegistry = toolRegistry
        ) {
            install(ChatMemory) {
                chatHistoryProvider = this@KrsAgent.chatHistoryProvider
                windowSize(50)
            }

            // Observability: record tokens on each completed LLM call and each tool call.
            handleEvents {
                onLLMCallCompleted { eventContext ->
                    val meta = eventContext.response?.metaInfo
                    val input = meta?.inputTokensCount ?: 0
                    val output = meta?.outputTokensCount ?: 0
                    val total = meta?.totalTokensCount ?: 0
                    tokenUsageTracker.recordLlmCall(
                        inputTokens = input,
                        outputTokens = output,
                        totalTokens = total
                    )
                    metrics.recordLlmCall(input, output, total)
                }

                onToolCallStarting { eventContext ->
                    tokenUsageTracker.recordToolCall(eventContext.toolName)
                    metrics.startTool(
                        toolCallId = eventContext.toolCallId,
                        toolName = eventContext.toolName,
                        arguments = eventContext.toolArgs.toString()
                    )
                    onToolStep(eventContext.toolName)
                }

                onToolCallCompleted { eventContext ->
                    metrics.finishTool(
                        toolCallId = eventContext.toolCallId,
                        toolName = eventContext.toolName,
                        status = ToolCallStatus.OK,
                        resultSize = eventContext.toolResult?.toString()?.length ?: 0
                    )
                }

                onToolCallFailed { eventContext ->
                    metrics.finishTool(
                        toolCallId = eventContext.toolCallId,
                        toolName = eventContext.toolName,
                        status = ToolCallStatus.ERROR,
                        resultSize = 0
                    )
                }
            }
        }
    }

    /**
     * Runs the agent for a single chat message.
     *
     * @param agentInput The user's chat message (with the verified NIM context prefixed).
     * @param sessionId The chat session identifier used for per-session memory.
     * @param semester The student's current semester (1-8); loads the relevant prompt modules.
     * @param onStep Friendly-label callback invoked each time the agent starts a step/tool.
     * @param onDraftSaved Invoked once when a KRS draft is successfully saved.
     * @return The agent's final response text.
     */
    suspend fun run(
        agentInput: String,
        sessionId: String,
        semester: Int,
        metrics: TurnMetrics = TurnMetrics(),
        onStep: (String) -> Unit = {},
        onDraftSaved: () -> Unit = {}
    ): String {
        val agent = buildAgent(
            systemPrompt = buildSystemPrompt(semester),
            metrics = metrics,
            onToolStep = { toolName -> onStep(ToolStepLabels.labelFor(toolName)) },
            onDraftSaved = onDraftSaved
        )
        return agent.run(agentInput = agentInput, sessionId = sessionId)
    }
}
