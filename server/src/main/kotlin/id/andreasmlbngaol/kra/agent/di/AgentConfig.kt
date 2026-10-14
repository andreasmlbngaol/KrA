package id.andreasmlbngaol.kra.agent.di

import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.executor.clients.google.GoogleModels
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

    /** koog's built-in Gemini 3.5 Flash Lite (no manual model definition needed). */
    @Bean
    fun llmModel(): LLModel = GoogleModels.Gemini3_5FlashLite

    @Bean
    fun llmClient(@Value("\${agent.google-api-key}") googleApiKey: String): LLMClient {
        // Fail fast at startup if the key is missing (the agent cannot run without it).
        require(googleApiKey.isNotBlank()) {
            "GOOGLE_API_KEY tidak di-set: agent tidak bisa dijalankan."
        }
        return GoogleLLMClient(apiKey = googleApiKey)
    }
}
