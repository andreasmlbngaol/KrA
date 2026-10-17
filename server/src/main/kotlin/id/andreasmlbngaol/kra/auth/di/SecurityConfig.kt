package id.andreasmlbngaol.kra.auth.di

import id.andreasmlbngaol.kra.auth.data.JwtAuthFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Stateless JWT security: the auth, dashboard, and evaluation endpoints are public,
 * everything else under `/api` needs a valid access token. No sessions, no CSRF
 * (token-based API), no form login.
 */
@Configuration
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize(HttpMethod.OPTIONS, "/**", permitAll)
                authorize("/", permitAll)
                authorize("/api", permitAll)
                authorize("/api/", permitAll)
                authorize("/api/auth/**", permitAll)
                authorize("/api/dashboard/**", permitAll)
                authorize("/api/evaluation/**", permitAll)
                authorize("/actuator/health", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/api/**", authenticated)
                authorize(anyRequest, permitAll)
            }
            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthFilter)
        }
        return http.build()
    }
}
