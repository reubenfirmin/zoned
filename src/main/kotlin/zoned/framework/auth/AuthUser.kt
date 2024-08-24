package zoned.framework.auth

import java.util.*

// warning - used in cache keys. don't add things that change per session or request
data class AuthUser(val identity: Identity?,
                    val role: Role
)

data class Identity(val email: String,
                    val userId: UUID,
                    val accountId: UUID)