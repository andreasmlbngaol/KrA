package id.andreasmlbngaol.kra.core.web

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Informational roots mirroring the old Ktor routing: `/` redirects to `api/`
 * and `/api` reports that the API is up. Only reached when the server is hit
 * directly; in production nginx serves `/` (the SPA) itself.
 */
@RestController
class RootController {
    @GetMapping("/")
    fun root(): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
            .header(HttpHeaders.LOCATION, "api/")
            .build()

    @GetMapping("/api", "/api/")
    fun apiRoot(): String = "KrA API is running"
}
