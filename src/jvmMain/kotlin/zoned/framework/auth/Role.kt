package zoned.framework.auth

import io.javalin.security.RouteRole

enum class Role(): RouteRole {
    ANON,
    USER,
    ADMIN;


    fun isOrSuperiorTo(targetRole: RouteRole): Boolean {
        return this == targetRole
                || this == ADMIN
                || (this == USER && targetRole == ANON)
    }
}