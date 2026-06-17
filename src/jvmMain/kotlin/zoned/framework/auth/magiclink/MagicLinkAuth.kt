package zoned.framework.auth.magiclink

import com.auth0.jwt.exceptions.TokenExpiredException
import com.google.common.hash.Hashing
import com.google.inject.Inject
import com.google.inject.name.Named
import io.javalin.http.Context
import io.javalin.http.Cookie
import io.javalin.http.SameSite
import org.slf4j.LoggerFactory
import zoned.framework.api.*
import zoned.framework.auth.JWTAuthentication
import zoned.framework.db.FormObject
import zoned.framework.form.ConvertedEntity
import zoned.framework.auth.Person
import zoned.framework.auth.Role
import zoned.framework.email.PostmarkEmailer
import zoned.framework.ui.layouts.Fragment.fragment
import zoned.framework.util.toUUID
import kotlinx.html.*
import java.nio.charset.Charset
import java.util.*

/**
 * Claim keys for magic link tokens.
 * These are distinct from session token claims (USER_ID, ACCOUNT_ID, ROLE).
 */
const val MAGIC_LINK_EMAIL = "email"
const val MAGIC_LINK_USER_ID = "userId"
const val MAGIC_LINK_KEY_HASH = "keyHash"

/**
 * Magic link authentication API.
 * Provides passwordless authentication via email links.
 *
 * Routes:
 * - POST /magiclink/request - Send magic link to email
 * - GET /magiclink/verify - Verify token and establish session
 * - GET /magiclink/expired - Show token expired page
 */
class MagicLinkAuth @Inject constructor(
    private val provider: MagicLinkProvider,
    private val jwt: JWTAuthentication,
    private val emailer: PostmarkEmailer,
    @param:Named("baseUrl") private val baseUrl: String
) : Api {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val basePath = "/magiclink"
    override val baseRoles = listOf(Role.ANON)

    @POST("/request")
    fun requestMagicLink(ctx: Context, request: ConvertedEntity<MagicLinkRequest>): Response {
        val email = request.entity().email
        val user = provider.findUserByEmail(email)
        val isNewUser = user == null

        // Get current auth key hash to make link one-time-use
        val currentKeyHash = user?.let { provider.getAuthKeyHash(it.id) } ?: ""

        // Build JWT claims: framework claims + custom params
        val claims = mutableMapOf(
            MAGIC_LINK_EMAIL to email,
            MAGIC_LINK_USER_ID to (user?.id?.toString() ?: ""),
            MAGIC_LINK_KEY_HASH to currentKeyHash
        )

        // Add custom params from provider (prefixed to avoid conflicts)
        provider.customMagicLinkParamNames().forEach { paramName ->
            ctx.formParam(paramName)?.let { paramValue ->
                claims["custom_$paramName"] = paramValue
            }
        }

        // Create short-lived token
        val token = jwt.createExpiringToken(claims, provider.tokenExpirySeconds())

        // Build magic link URL
        val magicLinkUrl = if (user != null) {
            // Existing user - login link
            route(::verify)
                .param("token", token)
                .urlWithHost(baseUrl)
        } else {
            // New user - registration link (or login if no registration)
            provider.newUserRegistrationRoute(email, token)
                ?: route(::verify)
                    .param("token", token)
                    .urlWithHost(baseUrl)
        }

        // Get email content from provider
        val subject = provider.emailSubject(ctx, isNewUser)
        val text1 = provider.emailBodyText1(ctx, isNewUser)
        val text2 = provider.emailBodyText2(ctx, isNewUser)
        val linkText = provider.emailLinkText(ctx, isNewUser)

        // Render email - provider can fully override, or fall back to framework template
        val emailBody = provider.renderEmailBody(ctx, magicLinkUrl, subject, text1, text2, linkText, isNewUser)
            ?: renderEmail(magicLinkUrl, subject, text1, text2, linkText)

        // Send email. Pass an explicit text body so Postmark doesn't auto-derive
        // one from the HTML (which would expose the magic link as a bare URL).
        // Body is intentionally URL-free; HTML clients show the CTA button.
        val textBody = "$subject\n\n$text1 $text2\n\n" +
                "Open this email in an HTML-capable client to continue. " +
                "If you didn't request this, you can ignore it."
        emailer.sendSimpleEmail(
            provider.fromEmail(),
            email,
            subject,
            emailBody,
            textBody
        )

        // Show confirmation page
        return provider.renderCheckEmailPage(ctx)
            ?: defaultCheckEmailPage(ctx, email)
    }

    @GET("/verify")
    fun verify(ctx: Context): Response {
        val token = ctx.queryParam("token")

        if (token == null) {
            logger.warn("Magic link verification failed: no token provided")
            return provider.renderTokenExpiredPage(ctx)
                ?: defaultExpiredPage(ctx)
        }

        return try {
            // Extract and validate token - get all claims including custom ones
            val allClaimNames = listOf(MAGIC_LINK_EMAIL, MAGIC_LINK_USER_ID, MAGIC_LINK_KEY_HASH) +
                    provider.customMagicLinkParamNames().map { "custom_$it" }
            val claims = jwt.extractClaims(token, allClaimNames)
            val email = claims[MAGIC_LINK_EMAIL]
            val userIdStr = claims[MAGIC_LINK_USER_ID]
            val tokenKeyHash = claims[MAGIC_LINK_KEY_HASH] ?: ""
            val userId = userIdStr?.toUUID()

            if (userId == null) {
                logger.warn("Magic link verification failed: claim '$MAGIC_LINK_USER_ID' missing or invalid. " +
                    "email=$email, userIdStr=$userIdStr, availableClaims=${claims.keys}")
                return provider.renderTokenExpiredPage(ctx)
                    ?: defaultExpiredPage(ctx)
            }

            val user = provider.findUserById(userId)

            // Atomically consume the link: check-and-rotate the auth key in one operation so
            // two concurrent clicks of the same link can't both log in. If we lose the swap,
            // the link was already used (or is a stale outstanding link).
            val newKey = generateKey()
            if (!provider.consumeAuthKey(user.id, tokenKeyHash, newKey)) {
                logger.info("Magic link already used: key hash mismatch for user ${user.id}")
                // If user is already logged in, just redirect to success
                if (provider.isUserLoggedIn(ctx)) {
                    return ctx.redirect(provider.loginSuccessRoute(user))
                }
                return provider.renderLinkAlreadyUsedPage(ctx)
                    ?: defaultAlreadyUsedPage(ctx)
            }

            // Issue long-lived auth token
            val authToken = jwt.issue(user)

            // Set auth cookie with security flags
            ctx.cookie(Cookie(
                name = "auth_token",
                value = "${user.email}|$authToken",
                maxAge = 86400, // 24 hours
                path = "/",
                sameSite = SameSite.LAX,
                secure = true,      // Only sent over HTTPS
                isHttpOnly = true   // Not accessible from JavaScript
            ))

            // Call custom hook
            provider.onLoginSuccess(ctx, user)

            // Build redirect route with custom params from JWT
            val successRoute = provider.loginSuccessRoute(user)
            val customParams = claims
                .filterKeys { it.startsWith("custom_") }
                .mapKeys { (key, _) -> key.removePrefix("custom_") }

            // Add custom params as query parameters (if any)
            val routeWithParams = if (customParams.isNotEmpty() && successRoute is BaseRoute) {
                successRoute.params(customParams)
            } else {
                successRoute
            }

            // Redirect to success route
            return ctx.redirect(routeWithParams)

        } catch (e: TokenExpiredException) {
            logger.warn("Magic link verification failed: token expired", e)
            provider.renderTokenExpiredPage(ctx)
                ?: defaultExpiredPage(ctx)
        } catch (e: Exception) {
            logger.error("Magic link verification failed: unexpected error", e)
            provider.renderTokenExpiredPage(ctx)
                ?: defaultExpiredPage(ctx)
        }
    }

    @GET("/expired")
    fun expired(ctx: Context): Response {
        return provider.renderTokenExpiredPage(ctx)
            ?: defaultExpiredPage(ctx)
    }

    // ========== Private Helpers ==========

    private fun renderEmail(
        magicLinkUrl: String,
        subject: String,
        text1: String,
        text2: String,
        linkText: String
    ): String {
        // Load email template from resources
        val template = javaClass.getResource("/magicLinkEmailTemplate.html")?.readText()
            ?: throw IllegalStateException("Magic link email template not found")

        // Substitute variables
        return template
            .replace("\${cta}", magicLinkUrl)
            .replace("\${linkText}", linkText)
            .replace("\${subject}", subject)
            .replace("\${text1}", text1)
            .replace("\${text2}", text2)
            .replace("\${appName}", provider.appName())
            .replace("\${accentColor}", provider.emailAccentColor())
            .replace("\${heroEmoji}", provider.emailHeroEmoji())
    }

    private fun generateKey(): String {
        val base1 = UUID.randomUUID().toString()
        val base2 = UUID.randomUUID().toString()
        val stretch = Hashing.sha512().hashString("$base1$base2", Charset.defaultCharset()).toString()
        return "sloth-$stretch-puravida"
    }

    private fun defaultCheckEmailPage(ctx: Context, email: String): Response {
        return ctx.fragment {
            div("max-w-2xl mx-auto mt-20 p-8 text-center") {
                h1("text-3xl font-bold text-gray-800 mb-4") {
                    +"✉️ Check Your Email"
                }
                p("text-gray-600 mb-2") {
                    +"We've sent a magic link to "
                    strong { +email }
                    +"."
                }
                p("text-gray-600") {
                    +"Click the link in the email to sign in. The link will expire in ${provider.tokenExpirySeconds() / 3600} hours."
                }
            }
        }
    }

    private fun defaultExpiredPage(ctx: Context): Response {
        return ctx.fragment {
            div("max-w-2xl mx-auto mt-20 p-8 text-center") {
                h1("text-3xl font-bold text-gray-800 mb-4") {
                    +"⏰ Link Expired"
                }
                p("text-gray-600 mb-2") {
                    +"This magic link has expired or is no longer valid."
                }
                p("text-gray-600 mb-6") {
                    +"Please request a new one to sign in."
                }
                a(href = "/", classes = "inline-block mt-4 px-6 py-3 bg-blue-600 text-white rounded hover:bg-blue-700") {
                    +"Return to Home"
                }
            }
        }
    }

    private fun defaultAlreadyUsedPage(ctx: Context): Response {
        return ctx.fragment {
            div("max-w-2xl mx-auto mt-20 p-8 text-center") {
                h1("text-3xl font-bold text-gray-800 mb-4") {
                    +"🔗 Link Already Used"
                }
                p("text-gray-600 mb-2") {
                    +"This magic link has already been used."
                }
                p("text-gray-600 mb-6") {
                    +"Magic links can only be used once. Please request a new one to sign in."
                }
                a(href = "/", classes = "inline-block mt-4 px-6 py-3 bg-blue-600 text-white rounded hover:bg-blue-700") {
                    +"Return to Home"
                }
            }
        }
    }
}

/**
 * Request body for magic link request.
 */
data class MagicLinkRequest(val email: String) : FormObject
