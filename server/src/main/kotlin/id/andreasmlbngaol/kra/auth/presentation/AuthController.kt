package id.andreasmlbngaol.kra.auth.presentation

import id.andreasmlbngaol.kra.academic.domain.usecase.StudentUseCase
import id.andreasmlbngaol.kra.auth.data.JwtService
import id.andreasmlbngaol.kra.auth.presentation.dto.LoginRequest
import id.andreasmlbngaol.kra.auth.presentation.dto.TokenResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private const val REFRESH_COOKIE_NAME = "refreshToken"

/**
 * Path the refresh cookie is scoped to: only the auth endpoints (refresh + logout), so the
 * browser never attaches the refresh token to ordinary API calls.
 */
private const val REFRESH_COOKIE_PATH = "/api/auth"

/**
 * Authentication endpoints under `/api/auth`:
 * - POST /login   : NIM + password -> access token (body) + refresh cookie (HttpOnly).
 * - POST /refresh : reads the refresh cookie, rotates it, returns a fresh access token.
 * - POST /logout  : clears the refresh cookie.
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val studentUseCase: StudentUseCase,
    private val jwtService: JwtService
) {
    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        if (request.nim.isBlank() || request.password.isBlank()) {
            return ResponseEntity.badRequest().body("NIM dan password tidak boleh kosong")
        }

        val student = studentUseCase.getStudentByNim(request.nim)
        // TODO: replace with password-hash verification from the Student.passwordHash column
        if (student == null || request.password != request.nim) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("NIM atau password salah")
        }

        response.addCookie(refreshCookie(jwtService.generateRefreshToken(student.nim)))
        return ResponseEntity.ok(TokenResponse(accessToken = jwtService.generateAccessToken(student.nim)))
    }

    @PostMapping("/refresh")
    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        val refreshToken = request.cookies?.firstOrNull { it.name == REFRESH_COOKIE_NAME }?.value
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sesi tidak ditemukan")
        }

        val nim = jwtService.verifyRefreshToken(refreshToken)
        if (nim == null) {
            response.addCookie(expiredRefreshCookie())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Sesi tidak valid atau sudah kadaluarsa")
        }

        // Rotation: issue a fresh refresh token on every refresh (sliding expiry).
        response.addCookie(refreshCookie(jwtService.generateRefreshToken(nim)))
        return ResponseEntity.ok(TokenResponse(accessToken = jwtService.generateAccessToken(nim)))
    }

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        response.addCookie(expiredRefreshCookie())
        return ResponseEntity.noContent().build()
    }

    private fun refreshCookie(token: String) = Cookie(REFRESH_COOKIE_NAME, token).apply {
        isHttpOnly = true
        secure = jwtService.cookieSecure
        path = REFRESH_COOKIE_PATH
        maxAge = jwtService.refreshTokenExpirySeconds.toInt()
        // Servlet API has no SameSite setter; set the header directly on the cookie attribute.
        setAttribute("SameSite", "Strict")
    }

    private fun expiredRefreshCookie() = Cookie(REFRESH_COOKIE_NAME, "").apply {
        isHttpOnly = true
        secure = jwtService.cookieSecure
        path = REFRESH_COOKIE_PATH
        maxAge = 0
        setAttribute("SameSite", "Strict")
    }
}
