package id.andreasmlbngaol.kra.auth.data

import com.auth0.jwt.exceptions.JWTVerificationException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Reads the `Authorization: Bearer <token>` header and, when the access token is valid,
 * puts an authenticated principal (name = NIM) into the SecurityContext.
 *
 * Missing or invalid tokens are left anonymous; the security rules decide whether that is
 * acceptable for the requested path.
 */
@Component
class JwtAuthFilter(private val jwtService: JwtService) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val nim = verify(header.removePrefix("Bearer ").trim())
            if (nim != null) {
                val auth = UsernamePasswordAuthenticationToken(
                    nim, null, listOf(SimpleGrantedAuthority("ROLE_STUDENT"))
                )
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun verify(token: String): String? = try {
        jwtService.accessTokenVerifier.verify(token).getClaim(JwtService.CLAIM_NIM).asString()
    } catch (_: JWTVerificationException) {
        null
    } catch (_: Exception) {
        null
    }
}
