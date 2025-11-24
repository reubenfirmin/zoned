package zoned.framework.auth.magiclink

import com.auth0.jwt.exceptions.TokenExpiredException
import com.google.common.hash.Hashing
import com.google.inject.Inject
import com.google.inject.name.Named
import io.javalin.http.Context
import io.javalin.http.Cookie
import io.javalin.http.SameSite
import zoned.framework.api.*
import zoned.framework.auth.JWTAuthentication
import zoned.framework.auth.Person
import zoned.framework.auth.Role
import zoned.framework.email.PostmarkEmailer
import zoned.framework.ui.layouts.Fragment.fragment
import zoned.framework.util.toUUID
import kotlinx.html.*
import java.nio.charset.Charset
import java.util.*

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

    override val basePath = "/magiclink"
    override val baseRoles = listOf(Role.ANON)

    @POST("/request")
    fun requestMagicLink(ctx: Context, request: MagicLinkRequest): Response {
        val email = request.email
        val user = provider.findUserByEmail(email)
        val isNewUser = user == null

        // Create short-lived token
        val token = jwt.createExpiringToken(
            mapOf(
                "email" to email,
                "userId" to (user?.id?.toString() ?: "")
            ),
            provider.tokenExpirySeconds()
        )

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

        // Render email
        val emailBody = renderEmail(magicLinkUrl, subject, text1, text2, linkText)

        // Send email
        emailer.sendSimpleEmail(
            provider.fromEmail(),
            email,
            subject,
            emailBody
        )

        // Show confirmation page
        return provider.renderCheckEmailPage(ctx)
            ?: defaultCheckEmailPage(ctx, email)
    }

    @GET("/verify")
    fun verify(ctx: Context): Response {
        val token = ctx.queryParam("token")

        if (token == null) {
            return provider.renderTokenExpiredPage(ctx)
                ?: defaultExpiredPage(ctx)
        }

        return try {
            // Extract and validate token
            val claims = jwt.extractClaims(token, listOf("email", "userId"))
            val userId = claims["userId"]?.toUUID()

            if (userId == null) {
                return provider.renderTokenExpiredPage(ctx)
                    ?: defaultExpiredPage(ctx)
            }

            val user = provider.findUserById(userId)

            // Generate new auth key
            val newKey = generateKey()
            provider.updateAuthKey(user.id, newKey)

            // Issue long-lived auth token
            val authToken = jwt.issue(user)

            // Set auth cookie
            ctx.cookie(Cookie(
                name = "auth_token",
                value = "${user.email}|$authToken",
                maxAge = 86400, // 24 hours
                path = "/",
                sameSite = SameSite.LAX
            ))

            // Call custom hook
            provider.onLoginSuccess(ctx, user)

            // Redirect to success route
            return ctx.redirect(provider.loginSuccessRoute(user))

        } catch (e: TokenExpiredException) {
            provider.renderTokenExpiredPage(ctx)
                ?: defaultExpiredPage(ctx)
        } catch (e: Exception) {
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
            .replace("\${host}", baseUrl)
            .replace("\${cta}", magicLinkUrl)
            .replace("\${linkText}", linkText)
            .replace("\${subject}", subject)
            .replace("\${text1}", text1)
            .replace("\${text2}", text2)
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
}

/**
 * Request body for magic link request.
 */
data class MagicLinkRequest(val email: String)
