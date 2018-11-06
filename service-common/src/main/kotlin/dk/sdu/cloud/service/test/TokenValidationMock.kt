package dk.sdu.cloud.service.test

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import dk.sdu.cloud.Role
import dk.sdu.cloud.SecurityPrincipal
import dk.sdu.cloud.SecurityScope
import dk.sdu.cloud.service.TokenValidationJWT
import java.util.*

object TokenValidationMock {
    internal const val sharedSecret = "shared-secret-for-testing"
    private val algorithm = Algorithm.HMAC512(sharedSecret)
    private val validator = TokenValidationJWT(algorithm)

    fun create(): TokenValidationJWT = validator

    /**
     * Generates a token for a security principal. This is required to be compatible with what the auth-service of
     * the latest version would provide.
     */
    fun createTokenForPrincipal(
        securityPrincipal: SecurityPrincipal,
        scopes: List<SecurityScope> = listOf(SecurityScope.ALL_WRITE),
        issuedAt: Long = 0L,
        expiresAt: Long = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365)
    ): String {
        return JWT.create().run {
            val principalType = when (securityPrincipal.role) {
                Role.USER, Role.ADMIN -> "password"
                Role.SERVICE, Role.THIRD_PARTY_APP -> "service"
                else -> ""
            }

            writeStandardClaims(
                securityPrincipal.username,
                securityPrincipal.role,
                issuedAt = issuedAt,
                expiresAt = expiresAt,
                principalType = principalType
            )

            when (securityPrincipal.role) {
                Role.USER, Role.ADMIN -> {
                    withClaim("firstNames", securityPrincipal.firstName)
                    withClaim("lastName", securityPrincipal.lastName)
                }

                else -> {
                    // Do nothing
                }
            }

            withAudience(*(scopes.map { it.toString() }.toTypedArray()))

            sign(algorithm)
        }
    }

    private fun JWTCreator.Builder.writeStandardClaims(
        username: String,
        role: Role,
        principalType: String,
        issuedAt: Long,
        expiresAt: Long
    ) {
        withSubject(username)
        withClaim("role", role.name)
        withIssuer("cloud.sdu.dk")
        withClaim("principalType", principalType)
        withIssuedAt(Date(issuedAt))
        withExpiresAt(Date(expiresAt))
    }
}

fun TokenValidationMock.createTokenForUser(
    username: String = "User",
    role: Role = Role.USER,
    scopes: List<SecurityScope> = listOf(SecurityScope.ALL_WRITE),
    issuedAt: Long = 0L,
    expiresAt: Long = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365)
): String {
    return createTokenForPrincipal(SecurityPrincipal(username, role, "user", "user"), scopes, issuedAt, expiresAt)
}

fun TokenValidationMock.createTokenForService(
    serviceName: String = "service",
    scopes: List<SecurityScope> = listOf(SecurityScope.ALL_WRITE),
    issuedAt: Long = 0L,
    expiresAt: Long = System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365)
): String {
    return createTokenForPrincipal(
        SecurityPrincipal("_" + serviceName.removePrefix("_"), Role.SERVICE, "service", "service"),
        scopes,
        issuedAt,
        expiresAt
    )
}

object TestUsers {
    val user = SecurityPrincipal("user", Role.USER, "user", "user")
    val admin = SecurityPrincipal("admin", Role.ADMIN, "admin", "admin")
    val service = SecurityPrincipal("_service", Role.SERVICE, "service", "service")
}
