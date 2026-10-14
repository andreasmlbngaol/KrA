package id.andreasmlbngaol.kra.auth.presentation.dto

/** Login request: NIM + password. */
data class LoginRequest(
    val nim: String,
    val password: String
)

/**
 * Access token returned after login/refresh.
 *
 * The refresh token is intentionally NOT included here: it is delivered separately in an
 * HttpOnly cookie so JavaScript can never read it.
 */
data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer"
)
