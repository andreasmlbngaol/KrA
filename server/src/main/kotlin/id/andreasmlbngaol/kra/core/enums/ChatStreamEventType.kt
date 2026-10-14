package id.andreasmlbngaol.kra.core.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The kind of a single line in the chat reply NDJSON stream (endpoint /agent/chat/stream).
 *
 * Each entry's @SerialName is the exact wire value, so the JSON contract is unchanged:
 * - [STEP]        a step/tool has started (carries a label).
 * - [DRAFT_SAVED] the KRS draft passed validation and was saved (the UI may show a shortcut).
 * - [FINAL]       the agent's final reply (carries content, in Markdown).
 * - [ERROR]       an error occurred (carries a user-friendly content message).
 * - [PING]        heartbeat that keeps the connection alive; ignored by the client.
 */
@Serializable
enum class ChatStreamEventType {
    @SerialName("step") STEP,
    @SerialName("draftSaved") DRAFT_SAVED,
    @SerialName("final") FINAL,
    @SerialName("error") ERROR,
    @SerialName("ping") PING
}
