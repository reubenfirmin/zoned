package zoned.framework.auth.magiclink

import io.javalin.http.Context
import zoned.framework.api.Response
import zoned.framework.api.Route
import zoned.framework.auth.Person
import java.util.UUID

/**
 * Provider interface for magic link authentication.
 * Apps implement this interface to customize magic link behavior and integrate with their user system.
 */
interface MagicLinkProvider {

    // ========== Required: User Operations ==========

    /**
     * Find a user by email address.
     * @return User if found, null if not found
     */
    fun findUserByEmail(email: String): Person?

    /**
     * Find a user by ID.
     * @return User (throws if not found)
     */
    fun findUserById(id: UUID): Person

    /**
     * Update user's authentication key after successful magic link login.
     */
    fun updateAuthKey(id: UUID, key: String)

    // ========== Required: Email Configuration ==========

    /**
     * Email address to send magic links from.
     */
    fun fromEmail(): String

    /**
     * Application name for branding in emails.
     */
    fun appName(): String

    // ========== Required: Routes ==========

    /**
     * Route to redirect to after successful login.
     * @param user The authenticated user
     * @return Route (e.g., route(DashboardApi::get))
     */
    fun loginSuccessRoute(user: Person): Route

    /**
     * Route for new user registration (if email not found).
     * @param email Email address submitted
     * @param token JWT token containing email
     * @return Registration URL with token, or null to send login link anyway
     */
    fun newUserRegistrationRoute(email: String, token: String): String?

    // ========== Optional: Email Content ==========

    /**
     * Email subject line.
     * Default: "{appName} - Create your account" or "{appName} - Your login link"
     */
    fun emailSubject(ctx: Context, isNewUser: Boolean): String {
        return if (isNewUser) {
            "${appName()} - Create your account"
        } else {
            "${appName()} - Your login link"
        }
    }

    /**
     * Email body text (first paragraph).
     * Default: "We received a request to [create an account|log in] to {appName}."
     */
    fun emailBodyText1(ctx: Context, isNewUser: Boolean): String {
        return "We received a request to ${if (isNewUser) "create an account" else "log in"} to ${appName()}."
    }

    /**
     * Email body text (second paragraph).
     * Default: "If you didn't request this, you can safely ignore this email."
     */
    fun emailBodyText2(ctx: Context, isNewUser: Boolean): String {
        return "If you didn't request this, you can safely ignore this email."
    }

    /**
     * Email call-to-action button text.
     * Default: "Sign Up" or "Log In"
     */
    fun emailLinkText(ctx: Context, isNewUser: Boolean): String {
        return if (isNewUser) "Sign Up" else "Log In"
    }

    // ========== Optional: Token Configuration ==========

    /**
     * Magic link token expiry in seconds.
     * Default: 21600 (6 hours)
     */
    fun tokenExpirySeconds(): Int = 21600

    // ========== Optional: Hooks ==========

    /**
     * Called after successful login, before redirect.
     * Use for tracking, analytics, feature flags, etc.
     */
    fun onLoginSuccess(ctx: Context, user: Person) {
        // Override for custom behavior
    }

    // ========== Optional: UI Customization ==========

    /**
     * Render custom "check your email" page after magic link sent.
     * @return Custom response, or null to use framework default
     */
    fun renderCheckEmailPage(ctx: Context): Response? = null

    /**
     * Render custom "token expired" page.
     * @return Custom response, or null to use framework default
     */
    fun renderTokenExpiredPage(ctx: Context): Response? = null
}
