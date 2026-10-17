package id.andreasmlbngaol.kra.agent.di

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openrouter.OpenRouterClientSettings
import ai.koog.prompt.executor.clients.openrouter.OpenRouterLLMClient
import ai.koog.prompt.llm.LLModel
import id.andreasmlbngaol.kra.agent.data.http.PromptCachingKoogHttpClient
import id.andreasmlbngaol.kra.agent.data.models.OpenRouterModels
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring wiring for the koog LLM primitives. Exposing the client and model as beans keeps
 * [id.andreasmlbngaol.kra.agent.data.KrsAgent] free of construction logic: it just injects
 * `LLMClient` / `LLModel`, exactly like any other collaborator.
 */
@Configuration
class AgentConfig {

    /** Claude Haiku 5.5 via OpenRouter (no built-in koog entry for this model). */
    @Bean
    fun llmModel(): LLModel = OpenRouterModels.ClaudeHaiku5_5

    @Bean
    fun llmClient(@Value("\${agent.openrouter-api-key}") openRouterApiKey: String): LLMClient {
        // Fail fast at startup if the key is missing (the agent cannot run without it).
        require(openRouterApiKey.isNotBlank()) {
            "OPENROUTER_API_KEY tidak di-set: agent tidak bisa dijalankan."
        }
        // koog's native OpenRouter client. Its default settings already target the
        // OpenAI-compatible endpoint https://openrouter.ai/api/v1/chat/completions
        // (baseUrl "https://openrouter.ai" + chatCompletionsPath "api/v1/chat/completions").
        //
        // The Ktor client is wrapped so every chat request carries a top-level cache_control field,
        // which is what turns on OpenRouter's prompt caching for this Anthropic model: koog's
        // OpenRouter client has no built-in prompt caching support (see PromptCachingKoogHttpClient).
        return OpenRouterLLMClient(
            apiKey = openRouterApiKey,
            settings = OpenRouterClientSettings(),
            httpClientFactory = PromptCachingKoogHttpClient.Factory(),
        )
    }
}
