package id.andreasmlbngaol.kra.agent.data.http

import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PromptCachingKoogHttpClientTest {

    /** Delegate palsu yang merekam body request terakhir. */
    private class RecordingClient : KoogHttpClient {
        override val clientName: String = "recording"
        var lastBody: String? = null

        override suspend fun <R : Any> get(
            path: String,
            responseType: KClass<R>,
            parameters: Map<String, String>,
            headers: Map<String, String>,
        ): R = error("get tidak dipakai di test ini")

        override suspend fun <T : Any, R : Any> post(
            path: String,
            requestBody: T,
            requestBodyType: KClass<T>,
            responseType: KClass<R>,
            parameters: Map<String, String>,
            headers: Map<String, String>,
        ): R {
            lastBody = requestBody as? String
            @Suppress("UNCHECKED_CAST")
            return "{}" as R
        }

        override fun <T : Any, R : Any, O : Any> sse(
            path: String,
            requestBody: T,
            requestBodyType: KClass<T>,
            dataFilter: (String?) -> Boolean,
            decodeStreamingResponse: (String) -> R,
            processStreamingChunk: (R) -> O?,
            parameters: Map<String, String>,
            headers: Map<String, String>,
        ): Flow<O> {
            lastBody = requestBody as? String
            return flowOf()
        }

        override fun <T : Any> lines(
            path: String,
            requestBody: T,
            requestBodyType: KClass<T>,
            parameters: Map<String, String>,
            headers: Map<String, String>,
        ): Flow<String> = flowOf()

        override fun close() {}
    }

    private val json = Json { isLenient = true; ignoreUnknownKeys = true }
    private val chatPath = "api/v1/chat/completions"
    private val chatBody =
        """{"model":"anthropic/claude-haiku-5.5","stream":true,"messages":[{"role":"system","content":"halo"}]}"""

    private fun sendPost(client: KoogHttpClient, path: String, body: String) = runBlocking {
        client.post<String, String>(path = path, requestBody = body)
    }

    private fun sendSse(client: KoogHttpClient, path: String, body: String) {
        client.sse(
            path = path,
            requestBody = body,
            requestBodyType = String::class,
            dataFilter = { true },
            decodeStreamingResponse = { it },
            processStreamingChunk = { it },
            parameters = emptyMap(),
            headers = emptyMap(),
        )
    }

    private fun cacheControlType(body: String): String? =
        json.parseToJsonElement(body).jsonObject["cache_control"]?.jsonObject?.get("type")?.jsonPrimitive?.content

    @Test
    fun `menambah cache_control top-level pada request chat`() {
        val recording = RecordingClient()
        sendPost(PromptCachingKoogHttpClient(recording), chatPath, chatBody)

        assertEquals("ephemeral", cacheControlType(recording.lastBody!!))
        // Field asli harus utuh.
        val body = json.parseToJsonElement(recording.lastBody!!).jsonObject
        assertEquals("anthropic/claude-haiku-5.5", body["model"]?.jsonPrimitive?.content)
        assertTrue(body.containsKey("messages"))
    }

    @Test
    fun `menambah cache_control pada request streaming sse`() {
        val recording = RecordingClient()
        sendSse(PromptCachingKoogHttpClient(recording), chatPath, chatBody)

        assertEquals("ephemeral", cacheControlType(recording.lastBody!!))
    }

    @Test
    fun `tidak menyentuh request non-chat`() {
        val recording = RecordingClient()
        sendPost(PromptCachingKoogHttpClient(recording), "api/v1/embeddings", chatBody)

        assertEquals(chatBody, recording.lastBody)
    }

    @Test
    fun `tidak menimpa cache_control yang sudah ada`() {
        val recording = RecordingClient()
        val withTtl =
            """{"model":"m","cache_control":{"type":"ephemeral","ttl":"1h"},"messages":[{"role":"user","content":"hi"}]}"""
        sendPost(PromptCachingKoogHttpClient(recording), chatPath, withTtl)

        val body = json.parseToJsonElement(recording.lastBody!!).jsonObject
        assertEquals("1h", body["cache_control"]?.jsonObject?.get("ttl")?.jsonPrimitive?.content)
    }

    @Test
    fun `meneruskan body non-String apa adanya`() {
        val recording = RecordingClient()
        runBlocking {
            recording.post<Any, String>(
                path = chatPath,
                requestBody = Any(),
                requestBodyType = Any::class,
                responseType = String::class,
                parameters = emptyMap(),
                headers = emptyMap(),
            )
        }
        assertNull(recording.lastBody)
    }

    @Test
    fun `tidak menambah cache_control bila bukan body JSON chat`() {
        val recording = RecordingClient()
        sendPost(PromptCachingKoogHttpClient(recording), chatPath, "not-json")

        assertEquals("not-json", recording.lastBody)
        assertFalse(recording.lastBody!!.contains("cache_control"))
    }
}
