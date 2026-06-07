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

    /**
     * Get the current auth key hash for a user.
     * Used at link-request time to embed the current hash in the token.
     * @return Current auth key hash, or null if user has no key yet
     */
    fun getAuthKeyHash(id: UUID): String?

    /**
     * Atomically consume a magic link: if the user's current auth key hash still equals
     * [expectedKeyHash], rotate the key to [newKey] and return true; otherwise return false.
     *
     * This is the one-time-use gate. The check ("is this link still valid?") and the rotation
     * ("burn it") MUST happen as a single atomic operation, or two concurrent clicks of the
     * same link (e.g. an email-security scanner pre-fetching it, then the user) can both pass
     * the check before either rotates the key, logging in twice.
     *
     * Implement with a single conditional UPDATE, e.g.:
     * ```sql
     * UPDATE users SET auth_key = :newKey WHERE id = :id AND sha512(auth_key) = :expectedKeyHash
     * ```
     * and return whether exactly one row changed.
     *
     * The default implementation delegates to [getAuthKeyHash] + [updateAuthKey], which is
     * NOT atomic — override this for a true one-time-use guarantee under concurrency.
     */
    fun consumeAuthKey(id: UUID, expectedKeyHash: String, newKey: String): Boolean {
        if ((getAuthKeyHash(id) ?: "") != expectedKeyHash) return false
        updateAuthKey(id, newKey)
        return true
    }

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

    // ========== Optional: Email Branding ==========

    /**
     * Primary accent color for the email (hex).
     * Used for the CTA button, links, and brand accents in the template.
     */
    fun emailAccentColor(): String = "#2563eb"

    /**
     * Hero emoji/icon shown at the top of the email.
     * Keep it a single unicode glyph; it will be rendered large on a gradient background.
     */
    fun emailHeroEmoji(): String = "\uD83D\uDD11" // 🔑

    /**
     * Fully render the magic link email HTML body.
     * Override to supply your own design (any approach: kotlinx.html, a different
     * template file, an external service, etc.).
     *
     * Return null to use the framework's built-in template, themed via
     * [emailAccentColor] and [emailHeroEmoji].
     */
    fun renderEmailBody(
        ctx: Context,
        magicLinkUrl: String,
        subject: String,
        text1: String,
        text2: String,
        linkText: String,
        isNewUser: Boolean
    ): String? = null

    // ========== Optional: Token Configuration ==========

    /**
     * Magic link token expiry in seconds.
     * Default: 21600 (6 hours)
     */
    fun tokenExpirySeconds(): Int = 21600

    // ========== Optional: Hooks ==========

    /**
     * Specify form parameter names to preserve through the magic link flow.
     * These parameters will be extracted from the request, stored in the JWT,
     * and appended as query params to the success redirect URL.
     * @return List of form parameter names to preserve (default: empty list)
     */
    fun customMagicLinkParamNames(): List<String> = emptyList()

    /**
     * Called after successful login, before redirect.
     * Use for tracking, analytics, feature flags, etc.
     */
    fun onLoginSuccess(ctx: Context, user: Person) {
        // Override for custom behavior
    }

    /**
     * Check if the user is already logged in (has valid session).
     * Used to redirect already-logged-in users instead of showing error pages.
     * Default checks for non-ANON role.
     */
    fun isUserLoggedIn(ctx: Context): Boolean {
        return try {
            val authUser = ctx.attribute<zoned.framework.auth.AuthUser>("auth")
            authUser != null && authUser.role != zoned.framework.auth.Role.ANON
        } catch (e: Exception) {
            false
        }
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

    /**
     * Render custom "link already used" page.
     * Shown when a magic link is clicked a second time.
     * @return Custom response, or null to use framework default
     */
    fun renderLinkAlreadyUsedPage(ctx: Context): Response? = null
}
