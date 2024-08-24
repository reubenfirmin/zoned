package zoned.framework.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.google.inject.Inject
import zoned.framework.auth.Role.ANON
import org.slf4j.LoggerFactory
import zoned.framework.config.Config
import java.lang.IllegalArgumentException
import java.util.*

const val USER_ID = "USER_ID"
const val ACCOUNT_ID = "ACCOUNT_ID"
const val ROLE = "ROLE"

interface Person {
    val id: UUID
    val email: String
    val role: Role
    val accountId: UUID
}

class JWTAuthentication @Inject constructor(config: Config) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val algorithm: Algorithm = Algorithm.HMAC256(config.jwtSecret)
    private val anon = AuthUser(null, ANON)

    fun issue(person: Person): String {
        return JWT.create()
            .withIssuer("auth0")
            .withExpiresAt(Date().toInstant().plusSeconds(86400))
            .withSubject(person.email)
            .withClaim(ROLE, person.role.name)
            .withClaim(USER_ID, person.id.toString())
            .withClaim(ACCOUNT_ID, person.accountId.toString())
            .sign(algorithm)
    }

    fun verify(email: String, token: String): AuthUser {
        val verifier: JWTVerifier = JWT.require(algorithm) // specify an specific claim validations
            .withIssuer("auth0") // reusable verifier instance
            .build()

        return try {
            val decodedJWT = verifier.verify(token)

            val subject = decodedJWT.subject
            // this check isn't strictly required, but does make it a little harder to attack
            require(subject == email) {
                "Could not verify" // don't leak details of why
            }

            // extract the claims
            val userId = UUID.fromString(decodedJWT.getClaim(USER_ID).asString())
            val accountId = UUID.fromString(decodedJWT.getClaim(ACCOUNT_ID).asString())
            val role = Role.valueOf(decodedJWT.getClaim(ROLE).asString())

            val identity = Identity(decodedJWT.subject, userId, accountId)
            AuthUser(identity, role)
        } catch (e: JWTVerificationException) {
            anon
        } catch (e: IllegalArgumentException) {
            logger.error(e.message, e)
            anon
        }
    }

    fun createExpiringToken(claims: Map<String, String>): String {
        return JWT.create()
            .withIssuer("auth0").apply {
                claims.forEach {
                    withClaim(it.key, it.value)
                }
            }
            .withExpiresAt(Date().toInstant().plusSeconds(86400))
            .sign(algorithm)
    }

    // TODO perhaps guard against expiry and return empty map
    fun extractClaims(token: String, claims: List<String>): Map<String, String?> {
        val verifier: JWTVerifier = JWT.require(algorithm) // specify an specific claim validations
            .withIssuer("auth0") // reusable verifier instance
            .build()

        val decoded = verifier.verify(token)
        val tokenClaims = decoded.claims
        return claims.associateWith {
            tokenClaims[it]?.asString()
        }
    }
}