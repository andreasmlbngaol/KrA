package id.andreasmlbngaol.kra.agent.data.models

import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

/**
 * Model OpenRouter yang dipakai agent KrA.
 *
 * koog belum punya entri bawaan untuk `anthropic/claude-haiku-5.5`, jadi model
 * dideklarasikan manual di sini. `provider = LLMProvider.OpenRouter` WAJIB agar
 * `MultiLLMPromptExecutor` merutekan permintaan ke `OpenRouterLLMClient`
 * (lihat `agent/di/AgentConfig.kt`).
 */
object OpenRouterModels {
    private const val CLAUDE_HAIKU_5_5 = "anthropic/claude-haiku-5.5"
//    private const val CLAUDE_HAIKU_5_5 = "anthropic/claude-haiku-5.5:nitro"

    /**
     * Claude Haiku 5.5 — model cepat & murah Anthropic via OpenRouter, mendukung
     * tool-calling yang dipakai agent untuk memanggil tool akademik.
     *
     * Prompt caching (Anthropic `cache_control`) aktif lewat PromptCachingKoogHttpClient
     * yang menyuntikkan field top-level itu ke setiap request chat — koog belum bisa mengirimnya sendiri.
     */
    val ClaudeHaiku5_5: LLModel = LLModel(
        provider = LLMProvider.OpenRouter,
        id = CLAUDE_HAIKU_5_5,
        capabilities = listOf(
            LLMCapability.Temperature,
            LLMCapability.Tools,
            LLMCapability.ToolChoice,
            LLMCapability.PromptCaching,
            LLMCapability.Schema.JSON.Basic,
            LLMCapability.Schema.JSON.Standard,
            LLMCapability.Completion,
        ),
        contextLength = 200_000,
        maxOutputTokens = 64_000,
    )
}
