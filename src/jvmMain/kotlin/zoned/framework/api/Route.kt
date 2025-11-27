package zoned.framework.api

import io.javalin.http.HandlerType
import zoned.framework.auth.Role

interface Route {

    val method: Method

    /**
     * Hostless url.
     */
    fun url(): String

    /**
     * Url with host
     */
    fun urlWithHost(baseUrl: String) = "$baseUrl${url()}"
}

data class AuthedRoute(val route: BaseRoute, val roles: Array<out Role>): Route {

    override val method: Method
        get() = route.method

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AuthedRoute

        if (route != other.route) return false
        if (!roles.contentEquals(other.roles)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = route.hashCode()
        result = 31 * result + roles.contentHashCode()
        return result
    }

    override fun url() = route.url()
}

data class BaseRoute(
    val path: String,
    override val method: Method = Method.POST,
    private val queryParam: String = ""): Route {

    internal fun path() = path.replace("//", "/")

    override fun url() = path

    fun params(params: Map<String, *>) = ParameterizedRoute(this).addParams(params)

    fun <T> param(name: String, value: T) = ParameterizedRoute(this).addParam(name, value)
}


data class ParameterizedRoute(
    val route: BaseRoute,
    private val pathParams: MutableMap<String, String> = mutableMapOf(),
    private val queryParams: MutableMap<String, String> = mutableMapOf()
) : Route {

    override val method: Method
        get() = route.method

    override fun url(): String {
        // Substitute path params in the route path
        var path = route.path()
        pathParams.forEach { (key, value) ->
            path = path.replace("{$key}", value)
        }

        // Append query params
        val paramStr = queryParams.entries.joinToString("&") { (key, value) -> "$key=$value" }
        return "$path${if (paramStr.isNotEmpty()) "?$paramStr" else ""}"
    }

    fun <T> addParam(name: String, value: T): ParameterizedRoute {
        val strValue = when (value) {
            is Enum<*> -> value.name
            else -> value.toString()
        }

        // Check if this is a path param or query param
        if (route.path().contains("{$name}")) {
            pathParams[name] = strValue
        } else {
            queryParams[name] = strValue
        }
        return this
    }

    fun addParams(newParams: Map<String, *>): ParameterizedRoute {
        newParams.forEach { (key, value) ->
            addParam(key, value)
        }
        return this
    }
}

fun interface Parameterizer {
    fun param(route: BaseRoute): ParameterizedRoute
}

enum class Method {
    POST, GET, PUT, DELETE, PATCH;

    fun toJavalin(): HandlerType {
        return when (this) {
            POST -> HandlerType.POST
            GET -> HandlerType.GET
            PUT -> HandlerType.PUT
            DELETE -> HandlerType.DELETE
            PATCH -> HandlerType.PATCH
        }
    }
}