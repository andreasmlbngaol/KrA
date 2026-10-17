package id.andreasmlbngaol.kra.agent.data.http

import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.ktor.KtorKoogHttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.reflect.KClass

/**
 * [KoogHttpClient] decorator yang menyuntikkan `cache_control` ke body request chat OpenRouter.
 *
 * **Kenapa perlu.** koog 1.3.0 tidak punya dukungan prompt caching di `OpenRouterLLMClient`:
 * `OpenRouterChatCompletionRequest` tidak punya field `cache_control`, sedangkan OpenRouter
 * mewajibkan field itu dikirim eksplisit untuk mengaktifkan caching pada model Anthropic.
 * Tanpa ini tidak akan pernah ada cache write/read (kolom cache di dashboard OpenRouter kosong).
 *
 * **Kenapa aman di level HTTP.** `AbstractOpenAILLMClient` sudah menyerialkan request menjadi
 * JSON String sebelum memanggil HTTP client (`serializeProviderChatRequest` →
 * `httpClient.post<String, String>` / `httpClient.sse(requestBodyType = String::class)`),
 * jadi body yang diterima di sini sudah final dan cukup ditambah satu field top-level.
 *
 * Bentuk yang ditambahkan adalah *automatic caching* OpenRouter
 * (`{"cache_control": {"type": "ephemeral"}}`): breakpoint cache diletakkan otomatis di blok
 * cacheable terakhir dan maju seiring percakapan bertambah — cocok untuk chat multi-turn dengan
 * `ChatMemory`. TTL default 5 menit.
 *
 * Hanya endpoint chat completions yang disentuh; `get` (daftar model), embeddings, dan body
 * non-String diteruskan apa adanya.
 */
internal class PromptCachingKoogHttpClient(
    private val delegate: KoogHttpClient
) : KoogHttpClient by delegate {

    override suspend fun <T : Any, R : Any> post(
        path: String,
        requestBody: T,
        requestBodyType: KClass<T>,
        responseType: KClass<R>,
        parameters: Map<String, String>,
        headers: Map<String, String>,
    ): R = delegate.post(
        path = path,
        requestBody = withCacheControl(path, requestBody, requestBodyType),
        requestBodyType = requestBodyType,
        responseType = responseType,
        parameters = parameters,
        headers = headers,
    )

    override fun <T : Any, R : Any, O : Any> sse(
        path: String,
        requestBody: T,
        requestBodyType: KClass<T>,
        dataFilter: (String?) -> Boolean,
        decodeStreamingResponse: (String) -> R,
        processStreamingChunk: (R) -> O?,
        parameters: Map<String, String>,
        headers: Map<String, String>,
    ): Flow<O> = delegate.sse(
        path = path,
        requestBody = withCacheControl(path, requestBody, requestBodyType),
        requestBodyType = requestBodyType,
        dataFilter = dataFilter,
        decodeStreamingResponse = decodeStreamingResponse,
        processStreamingChunk = processStreamingChunk,
        parameters = parameters,
        headers = headers,
    )

    /** Menambah `cache_control` bila ini request chat completions yang di-serialkan sebagai JSON. */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> withCacheControl(path: String, body: T, bodyType: KClass<T>): T {
        if (bodyType != String::class || body !is String || !path.endsWith(CHAT_COMPLETIONS_PATH)) return body

        val request = runCatching { cacheJson.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return body
        // Lewati kalau bukan request chat, atau kalau pemanggil sudah mengirim cache_control sendiri.
        if (request.containsKey(CACHE_CONTROL) || !request.containsKey(MESSAGES)) return body

        val patched = buildJsonObject {
            request.forEach { (key, value) -> put(key, value) }
            put(CACHE_CONTROL, buildJsonObject { put(TYPE, JsonPrimitive(EPHEMERAL)) })
        }
        return cacheJson.encodeToString(JsonObject.serializer(), patched) as T
    }

    /** Membungkus factory lain (default: Ktor) dengan [PromptCachingKoogHttpClient]. */
    class Factory(
        private val delegate: KoogHttpClient.Factory = KtorKoogHttpClient.Factory()
    ) : KoogHttpClient.Factory {

        override fun create(
            clientName: String,
            baseUrl: String,
            headers: Map<String, String>,
            queryParameters: Map<String, String>,
            requestTimeoutMillis: Long,
            connectTimeoutMillis: Long,
            socketTimeoutMillis: Long,
            json: Json,
        ): KoogHttpClient = PromptCachingKoogHttpClient(
            delegate.create(
                clientName = clientName,
                baseUrl = baseUrl,
                headers = headers,
                queryParameters = queryParameters,
                requestTimeoutMillis = requestTimeoutMillis,
                connectTimeoutMillis = connectTimeoutMillis,
                socketTimeoutMillis = socketTimeoutMillis,
                json = json,
            )
        )
    }

    private companion object {
        const val CACHE_CONTROL = "cache_control"
        const val TYPE = "type"
        const val EPHEMERAL = "ephemeral"
        const val MESSAGES = "messages"
        const val CHAT_COMPLETIONS_PATH = "chat/completions"

        /** Json khusus manipulasi [JsonObject]: key dipakai apa adanya, jadi naming strategy tidak relevan. */
        val cacheJson = Json { isLenient = true; ignoreUnknownKeys = true }
    }
}
