package id.andreasmlbngaol.kra.core.web

import id.andreasmlbngaol.kra.core.error.BadRequestException
import id.andreasmlbngaol.kra.core.error.ErrorMessages
import id.andreasmlbngaol.kra.core.error.friendlyError
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = LoggerFactory.getLogger("ErrorHandler")

/**
 * GLOBAL safety net for every endpoint (the Spring equivalent of Ktor's StatusPages).
 *
 * Any exception not handled in a controller is turned into a friendly, safe message for the
 * user. Technical details (stack traces, LLM provider error bodies, exception messages, etc.)
 * are ONLY recorded in the server log and never sent to the client.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /** Malformed / unreadable request body -> 400 with a friendly message. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableBody(cause: HttpMessageNotReadableException): ResponseEntity<String> {
        logger.warn("Permintaan tidak valid", cause)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorMessages.BAD_REQUEST)
    }

    /** Caller-facing validation error (safe to show its message). */
    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequest(cause: BadRequestException): ResponseEntity<String> {
        logger.warn("Permintaan ditolak: {}", cause.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(cause.message ?: ErrorMessages.BAD_REQUEST)
    }

    @ExceptionHandler(Throwable::class)
    fun handleAny(cause: Throwable): ResponseEntity<String> {
        logger.error("Error tak tertangani di server", cause)
        val (status, message) = friendlyError(cause)
        return ResponseEntity.status(status).body(message)
    }
}
