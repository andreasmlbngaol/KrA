package id.andreasmlbngaol.kra.agent.presentation.dto

import id.andreasmlbngaol.kra.core.enums.ChatStreamEventType
import kotlinx.serialization.Serializable

/** Request body for a chat turn: the session id plus the user's message. */
@Serializable
data class ChatRequest(
    val sessionId: String,
    val message: String
)

/**
 * One event line in the chat reply NDJSON stream. The [type] discriminator is an enum so
 * the server cannot emit an unknown event kind; the exact wire strings come from
 * [ChatStreamEventType]'s @SerialName.
 */
@Serializable
data class ChatStreamEvent(
    val type: ChatStreamEventType,
    val label: String? = null,
    val content: String? = null
)
