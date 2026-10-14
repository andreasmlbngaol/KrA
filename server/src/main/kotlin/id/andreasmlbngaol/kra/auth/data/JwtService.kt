package id.andreasmlbngaol.kra.auth.data

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date

/**
 * Creates and verifies JWTs (access + refresh).
 *
 * Settings come from the `jwt` block in application.yaml. The refresh token lives only in
 * an HttpOnly cookie; only the short-lived access token is handed to JavaScript.
 */
@Component
class JwtService(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.issuer}") private val issuer: String,
    @Value("\${jwt.audience}") private val audience: String,
    @Value("\${jwt.realm}") val realm: String,
    @Value("\${jwt.access-token-expiry-minutes}") accessTokenExpiryMinutes: Long,
    @Value("\${jwt.refresh-token-expiry-days}") refreshTokenExpiryDays: Long,
    @Value("\${jwt.cookie-secure:false}") val cookieSecure: Boolean
) {
    private val algorithm: Algorithm = Algorithm.HMAC256(secret)
    private val accessTokenExpiryMs: Long = accessTokenExpiryMinutes * 60_000L
    private val refreshTokenExpiryMs: Long = refreshTokenExpiryDays * 24L * 60L * 60L * 1000L

    /** Refresh-token lifetime in seconds, used as the refresh cookie's Max-Age. */
    val refreshTokenExpirySeconds: Long get() = refreshTokenExpiryMs / 1000L

    /** Verifier for access tokens, used by the Spring Security filter. */
    val accessTokenVerifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim(CLAIM_TYPE, TYPE_ACCESS)
        .build()

    private val refreshTokenVerifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim(CLAIM_TYPE, TYPE_REFRESH)
        .build()

    /** Creates a signed short-lived access token carrying the student's NIM. */
    fun generateAccessToken(nim: String): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim(CLAIM_NIM, nim)
        .withClaim(CLAIM_TYPE, TYPE_ACCESS)
        .withExpiresAt(Date(System.currentTimeMillis() + accessTokenExpiryMs))
        .sign(algorithm)

    /** Creates a signed long-lived refresh token carrying the student's NIM. */
    fun generateRefreshToken(nim: String): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim(CLAIM_NIM, nim)
        .withClaim(CLAIM_TYPE, TYPE_REFRESH)
        .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenExpiryMs))
        .sign(algorithm)

    /** Returns the NIM when the refresh token is valid, or null when invalid/expired. */
    fun verifyRefreshToken(token: String): String? = try {
        refreshTokenVerifier.verify(token).getClaim(CLAIM_NIM).asString()
    } catch (_: Exception) {
        null
    }

    companion object {
        const val CLAIM_NIM = "nim"
        private const val CLAIM_TYPE = "type"
        private const val TYPE_ACCESS = "access"
        private const val TYPE_REFRESH = "refresh"
    }
}
