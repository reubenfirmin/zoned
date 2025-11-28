package zoned.framework.ui.components.inputs

import kotlinx.html.FORM
import zoned.framework.api.Method
import zoned.framework.api.Parameterizer
import zoned.framework.api.Response
import zoned.framework.api.Route
import zoned.framework.api.route
import zoned.framework.ui.layouts.HtmxSelector
import zoned.framework.ui.libs.HTMX
import kotlin.reflect.KFunction

/**
 * Set form action using a Route with optional HTMX target, swap strategy, and encoding
 */
fun FORM.action(
    route: Route,
    target: HtmxSelector? = null,
    swap: HTMX.Swap? = null,
    encoding: HTMX.Encoding? = null
) {
    val trigger = when (route.method) {
        Method.POST -> "hx-post"
        Method.GET -> "hx-get"
        Method.PUT -> "hx-put"
        Method.DELETE -> "hx-delete"
        Method.PATCH -> "hx-patch"
    }
    attributes[trigger] = route.url()

    if (target != null) {
        attributes["hx-target"] = target.cssSelector
    }

    if (swap != null) {
        val swapValue = when (swap) {
            HTMX.Swap.INNER -> "innerHTML"
            HTMX.Swap.OUTER -> "outerHTML"
            HTMX.Swap.BEFOREEND -> "beforeend"
            HTMX.Swap.AFTEREND -> "afterend"
        }
        attributes["hx-swap"] = swapValue
    }

    if (encoding != null) {
        attributes["hx-encoding"] = encoding.value
    }
}

/**
 * Type-safe form action using method reference
 */
fun FORM.action(
    handler: KFunction<Response>,
    target: HtmxSelector? = null,
    swap: HTMX.Swap? = null,
    encoding: HTMX.Encoding? = null
) {
    val route = route(handler)
    action(route, target, swap, encoding)
}

/**
 * Type-safe form action with parameterization
 */
fun FORM.action(
    handler: KFunction<Response>,
    parameterizer: Parameterizer,
    target: HtmxSelector? = null,
    swap: HTMX.Swap? = null,
    encoding: HTMX.Encoding? = null
) {
    val route = route(handler).let { parameterizer.param(it) }
    action(route, target, swap, encoding)
}

/**
 * Simplified form action with varargs parameters
 * Example: form.action(api::save, "id" to 123, "mode" to "edit")
 */
fun FORM.action(
    handler: KFunction<Response>,
    vararg params: Pair<String, Any>,
    target: HtmxSelector? = null,
    swap: HTMX.Swap? = null,
    encoding: HTMX.Encoding? = null
) {
    val baseRoute = route(handler)
    val finalRoute: Route = if (params.isNotEmpty()) {
        var paramRoute = baseRoute.param(params[0].first, params[0].second)
        for (i in 1 until params.size) {
            paramRoute = paramRoute.addParam(params[i].first, params[i].second)
        }
        paramRoute
    } else {
        baseRoute
    }
    action(finalRoute, target, swap, encoding)
}