package zoned.framework.api

import zoned.framework.auth.Role

interface Api {

    val basePath: String
    val baseRoles: List<Role>
}