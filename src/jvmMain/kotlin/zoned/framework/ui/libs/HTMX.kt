package zoned.framework.ui.libs

import io.javalin.http.Context
import kotlinx.html.*
import zoned.framework.api.*
import zoned.framework.api.Method.DELETE
import zoned.framework.api.Method.GET
import zoned.framework.api.Method.PATCH
import zoned.framework.api.Method.POST
import zoned.framework.api.Method.PUT
import zoned.framework.form.ConvertedEntity
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.layouts.HTMXTarget
import zoned.framework.ui.layouts.HtmxInclude
import zoned.framework.ui.layouts.HtmxSelector
import zoned.framework.ui.libs.HTMX.Swap.*
import zoned.framework.ui.libs.HTMX.htmxOnEvent
import zoned.framework.util.Either
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

/**
 * Call an api path (which can include query params) and send results to a target element.
 *
 * You can also use HTMXParamLink, which sends params via a form.
 */
object HTMX {

    /**
     * This is done automatically when using page. However, can be done on an as-needed basis
     */
    fun HTMLTag.pushUrl() {
        attributes["hx-push-url"] = "true"
    }

    fun HTMLTag.htmxOnEvent(event: String,
                            path: String,
                            target: HtmxSelector? = null,
                            method: Method = POST,
                            swap: Swap? = INNER,
                            include: HtmxInclude? = null,
                            swapDelay: Int? = 0): HTMLTag {
        attributes["hx-trigger"] = event
        when (method) {
            POST -> attributes["hx-post"] = path
            GET -> attributes["hx-get"] = path
            PUT -> attributes["hx-put"] = path
            DELETE -> attributes["hx-delete"] = path
            PATCH -> attributes["hx-patch"] = path
        }
        if (target != null) {
            attributes["hx-target"] = target.cssSelector
        }
        val swapAttr = when (swap) {
            INNER ->  "innerHTML"
            OUTER -> "outerHTML"
            BEFOREEND -> "beforeend"
            AFTEREND -> "afterend"
            null -> null
        }
        val delay = if (swapDelay == null) {
            ""
        } else {
            " swap:${swapDelay}ms"
        }
        if (swap != null) {
            attributes["hx-swap"] = "$swapAttr$delay"
        }
        if (include != null) {
            attributes["hx-include"] = include.cssSelector
        }
        return this
    }

    fun HTMLTag.htmxBoost() {
        attributes["hx-boost"] = "true"
    }

    enum class Swap {
        INNER, OUTER, BEFOREEND, AFTEREND
    }

    enum class Encoding(val value: String) {
        MULTIPART("multipart/form-data"),
        URL_ENCODED("application/x-www-form-urlencoded")
    }
}

// TODO move these to ConextExtensions
fun Context.target(target: HTMXTarget?) {
    if (target != null) {
        header("HX-Retarget", target.cssSelector)
    }
}

fun Context.location(path: String) {
    header("HX-Location", path)
}

fun Context.setHistory() {
    val queryString = queryParamMap().entries.flatMap { entry ->
        entry.value.map { value ->
            "${entry.key}=${value}"
        }
    }.joinToString("&")

    val url = if (queryString.isNotEmpty()) {
        "${path()}?$queryString"
    } else {
        path()
    }

    header("HX-Push-Url", url)
}

fun Context.unwrap() {
    header("HX-Reselect", ".wrapper > *")
}

fun Context.redirectExternal(location: String): Response {
    header("HX-Redirect", location)
    return Response(Either.left(""), redirect = true)
}

fun Context.isDynamic() = headerMap().containsKey("hx-request") || headerMap().containsKey("HX-Request")

private fun getRoute(action: HTMXAction): Route {
    return route(action.handler).let {
        action.parameterizer?.param(it) ?: it
    }
}

fun HTMLTag.onLoad(action: HTMXAction, delayMs: Int? = null): HTMLTag {
    with (getRoute(action)) {
        val delayStr = delayMs?.let {
            " delay:" + it.toString() + "ms"
        } ?: ""
        return htmxOnEvent("load$delayStr", url(), method = method)
    }
}

fun HTMLTag.onClick(action: HTMXAction): HTMLTag {
    with (getRoute(action)) {
        return htmxOnEvent(
            "click",
            url(),
            method = method,
            swap = action.swap,
            swapDelay = action.swapDelay,
            target = action.target,
            include = action.include)
    }
}

/**
 * Add a confirmation dialog before the HTMX action executes.
 */
fun HTMLTag.confirm(message: String): HTMLTag {
    attributes["hx-confirm"] = message
    return this
}

/**
 * Opt this element out of HTMX boosting. Use on a real file-download `<a>` (or
 * any link that must trigger a full browser navigation) inside a boosted page,
 * where an AJAX-swapped response would otherwise be wrong. Sets hx-boost="false".
 */
fun HTMLTag.htmxNoBoost(): HTMLTag {
    attributes["hx-boost"] = "false"
    return this
}

fun HTMLTag.onChange(action: HTMXAction): HTMLTag {
    with (getRoute(action)) {
        return htmxOnEvent("change",
            url(),
            method = method,
            swap = action.swap,
            target = action.target,
            include = action.include)
    }
}

fun HTMLTag.onKeyPress(action: HTMXAction, keycode: String): HTMLTag {
    with (getRoute(action)) {
        return htmxOnEvent("keypress[code=='$keycode']",
            url(),
            method = method,
            swap = action.swap,
            target = action.target,
            include = action.include)
    }
}

fun INPUT.onTypingPause(action: HTMXAction,
                        delayMs: Int = 250): HTMLTag {
    with (getRoute(action)) {
        return htmxOnEvent("keyup changed delay:${delayMs}ms",
            url(),
            method = method,
            swap = action.swap,
            swapDelay = action.swapDelay,
            target = action.target,
            include = action.include)
    }
}

fun HTMLTag.onKeypress(action: HTMXAction): HTMLTag {
    with (getRoute(action)) {
        return htmxOnEvent("keypress",
            url(),
            method = method,
            swap = action.swap,
            swapDelay = action.swapDelay,
            target = action.target,
            include = action.include)
    }
}

fun HTMLTag.onKeyUp(action: HTMXAction): HTMLTag {
    with (getRoute(action)) {
        return htmxOnEvent("keyup",
            url(),
            method = method,
            swap = action.swap,
            swapDelay = action.swapDelay,
            target = action.target,
            include = action.include)
    }
}

fun HTMLTag.onHover(action: HTMXAction, once: Boolean = true): HTMLTag {
    with (getRoute(action)) {
        val trigger = if (once) "mouseenter once" else "mouseenter"
        return htmxOnEvent(trigger,
            url(),
            method = method,
            swap = action.swap,
            swapDelay = action.swapDelay,
            target = action.target,
            include = action.include)
    }
}

fun withAction(handler: KFunction<Response>,
               parameterizer: Parameterizer? = null,
               include: HtmxInclude? = null,
               swap: HTMX.Swap? = null,
               swapDelay: Int? = null,
               target: HtmxSelector? = null) =
    HTMXAction(handler, parameterizer, include, swap, swapDelay, target)

/**
 * Simplified parameter passing using varargs.
 * Example: withAction(api::save, "id" to 123, "mode" to "edit")
 */
fun withAction(handler: KFunction<Response>,
               vararg params: Pair<String, Any>,
               include: HtmxInclude? = null,
               swap: HTMX.Swap? = null,
               swapDelay: Int? = null,
               target: HtmxSelector? = null): HTMXAction {
    val parameterizer: Parameterizer? = if (params.isNotEmpty()) {
        Parameterizer { route ->
            var paramRoute = route.param(params[0].first, params[0].second)
            for (i in 1 until params.size) {
                paramRoute = paramRoute.addParam(params[i].first, params[i].second)
            }
            paramRoute
        }
    } else {
        null
    }
    return HTMXAction(handler, parameterizer, include, swap, swapDelay, target)
}
