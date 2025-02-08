package zoned.framework.auth

import com.google.inject.Inject
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.UnauthorizedResponse

class Auth @Inject constructor(private val jwt: JWTAuthentication): Handler {

    override fun handle(ctx: Context) {
        // TODO clean up this check
        if (ctx.path().startsWith("/static")) {
            return
        }

        // must supply both
        val authHeader = ctx.header("Authorization")
        val email = ctx.header("Identity") // TODO revisit / remove?
        val cookie = ctx.cookie("auth_token")?.split("|")

        val userAndRole = if (authHeader != null && email != null) {
            val token = authHeader.removePrefix("Bearer ")
            jwt.verify(email, token)
        } else if (cookie != null && cookie.size == 2) {
            val cookieEmail = cookie[0]
            val token = cookie[1]
            jwt.verify(cookieEmail, token)
        } else {
            AuthUser(null, Role.ANON)
        }

        // this can now be accessed by controllers.
        ctx.authUser(userAndRole)

        if (!ctx.routeRoles().any { userAndRole.role.isOrSuperiorTo(it) }) {
            throw UnauthorizedResponse()
        }
    }

    companion object {
        private val authKey = "auth"

        fun Context.authUser() = attribute<AuthUser>(authKey)!!

        fun Context.authUser(authUser: AuthUser) = attribute(authKey, authUser)
    }
}