package id.andreasmlbngaol.kra.agent.data.tools

import ai.koog.agents.core.tools.reflect.ToolSet
import id.andreasmlbngaol.kra.agent.data.session.SessionTokenRegistry
import org.slf4j.LoggerFactory

/**
 * Shared base class for every domain ToolSet: consistent logging so tool calls and their
 * results are traceable in the server logs, plus the one place where a tool turns the
 * session token from the LLM back into a NIM.
 */
abstract class LoggingToolSet(protected val sessions: SessionTokenRegistry) : ToolSet {

    private val log = LoggerFactory.getLogger(this::class.java)

    /**
     * Resolves the session token supplied by the LLM. Returns null when the token is
     * unknown or expired; the caller must then return [sessionExpired] instead of data.
     */
    protected fun resolveNim(tool: String, sessionToken: String): String? {
        val nim = sessions.resolve(sessionToken)
        if (nim == null) log.warn("Token sesi tidak valid atau kedaluwarsa pada tool {}", tool)
        return nim
    }

    /** JSON returned when a tool cannot resolve the session; never contains a NIM. */
    protected fun sessionExpired(): String =
        """{"error":"Sesi tidak valid atau sudah berakhir. Muat ulang halaman lalu coba lagi."}"""

    /** Logs a tool invocation with its name and serialized arguments. */
    protected fun logCall(tool: String, args: String) {
        log.info("\u2192 TOOL CALL: {}({})", tool, args)
    }

    /** Logs a tool's result and returns it unchanged, so call sites log and return in one expression. */
    protected fun logResult(tool: String, result: String): String {
        log.info("\u2190 TOOL RESULT: {} ->\n{}", tool, result)
        return result
    }
}
