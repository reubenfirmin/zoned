package zoned.framework.api

import io.javalin.http.Context
import io.javalin.http.HandlerType
import zoned.framework.ui.libs.location
import zoned.framework.util.Either
import java.net.URI
import java.util.*

fun Context.redirect(route: Route): Response {
    val currentMethod = when(method()) {
        HandlerType.PUT -> Method.PUT
        HandlerType.POST -> Method.POST
        HandlerType.GET -> Method.GET
        HandlerType.DELETE -> Method.DELETE
        HandlerType.PATCH -> Method.PATCH
        else -> throw Exception("No support for ${method()}")
    }
    if (currentMethod != route.method) {
        // (TODO) use HTMX to switch method
        if (route.method == Method.GET) {
            location(route.url())
            html("ok")
        } else {
            throw Exception("Cannot redirect from a ${method()} to a ${route.method}")
        }
    } else {
        redirect(route.url())
    }
    return Response(Either.left(""))
}

fun Context.locale(): Locale {
    val language = header("Accept-Language")
    return Locale.forLanguageTag(language?.split(",")?.first() ?: "en") ?: Locale.ENGLISH
}

fun Context.route(route: BaseRoute) = attribute("route", route)

fun Context.route() = attribute<BaseRoute>("route")!!

fun Context.hasForm() = formParamMap().isNotEmpty()

fun Context.queryParamStrict(name: String) = queryParam(name)?.let {
    it.ifEmpty {
        null
    }
}

fun Context.formParamStrict(name: String) = formParam(name)?.let {
    it.ifEmpty {
        null
    }
}

fun Context.sameApi(api: Api): Boolean {
    // TODO maybe dial this back
    val referer = header("Referer") ?: throw Exception("Referer not found")
    val basePath = api.basePath
    val uri = URI(referer)
    val path = uri.path
    return path.startsWith(basePath)
}