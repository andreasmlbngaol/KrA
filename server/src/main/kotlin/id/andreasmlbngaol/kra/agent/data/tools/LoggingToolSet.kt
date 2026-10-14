package id.andreasmlbngaol.kra.agent.data.tools

import ai.koog.agents.core.tools.reflect.ToolSet
import org.slf4j.LoggerFactory

/**
 * Shared base class for every domain ToolSet: consistent logging so tool calls and their
 * results are traceable in the server logs.
 */
abstract class LoggingToolSet : ToolSet {

    private val log = LoggerFactory.getLogger(this::class.java)

    /** Logs a tool invocation with its name and serialized arguments. */
    protected fun logCall(tool: String, args: String) {
        log.info("→ TOOL CALL: {}({})", tool, args)
    }

    /** Logs a tool's result and returns it unchanged, so call sites log and return in one expression. */
    protected fun logResult(tool: String, result: String): String {
        log.info("← TOOL RESULT: {} ->\n{}", tool, result)
        return result
    }
}
