package zoned.framework.ui.response

import io.javalin.http.Context
import zoned.framework.api.Response
import zoned.framework.api.route
import zoned.framework.ui.layouts.HTMXTarget
import zoned.framework.ui.libs.HTMX
import kotlin.reflect.KFunction

/**
 * Builder for type-safe HTMX response headers
 *
 * Example usage:
 * ```
 * ctx.htmxResponse {
 *     location = route(DashboardApi::get)
 *     trigger("refreshTable", "updateCount")
 *     retarget = "#main-content"
 *     reswap = HTMX.Swap.INNER_HTML
 * }
 * ```
 */
class HTMXResponseBuilder(private val ctx: Context) {
    /**
     * Set HX-Location header to redirect the browser (using route for type safety)
     */
    var location: String? = null
        set(value) {
            if (value != null) {
                ctx.header("HX-Location", value)
            }
            field = value
        }

    /**
     * Set HX-Redirect header for client-side redirect
     */
    var redirect: String? = null
        set(value) {
            if (value != null) {
                ctx.header("HX-Redirect", value)
            }
            field = value
        }

    /**
     * Set HX-Refresh header to force page refresh
     */
    var refresh: Boolean = false
        set(value) {
            if (value) {
                ctx.header("HX-Refresh", "true")
            }
            field = value
        }

    /**
     * Set HX-Retarget header to change the target element
     */
    var retarget: String? = null
        set(value) {
            if (value != null) {
                ctx.header("HX-Retarget", value)
            }
            field = value
        }

    /**
     * Set HX-Reswap header to change the swap strategy
     */
    var reswap: HTMX.Swap? = null
        set(value) {
            if (value != null) {
                val swapValue = when (value) {
                    HTMX.Swap.INNER -> "innerHTML"
                    HTMX.Swap.OUTER -> "outerHTML"
                    HTMX.Swap.BEFOREEND -> "beforeend"
                    HTMX.Swap.AFTEREND -> "afterend"
                }
                ctx.header("HX-Reswap", swapValue)
            }
            field = value
        }

    /**
     * Set HX-Trigger header to trigger client-side events
     * Can be called multiple times to trigger multiple events
     */
    fun trigger(vararg events: String) {
        if (events.isNotEmpty()) {
            val existing = ctx.header("HX-Trigger")
            val combined = if (existing != null) {
                "$existing, ${events.joinToString(", ")}"
            } else {
                events.joinToString(", ")
            }
            ctx.header("HX-Trigger", combined)
        }
    }

    /**
     * Set HX-Trigger-After-Settle header
     */
    fun triggerAfterSettle(vararg events: String) {
        if (events.isNotEmpty()) {
            ctx.header("HX-Trigger-After-Settle", events.joinToString(", "))
        }
    }

    /**
     * Set HX-Trigger-After-Swap header
     */
    fun triggerAfterSwap(vararg events: String) {
        if (events.isNotEmpty()) {
            ctx.header("HX-Trigger-After-Swap", events.joinToString(", "))
        }
    }

    /**
     * Set HX-Push-Url header to update the browser URL
     */
    var pushUrl: String? = null
        set(value) {
            if (value != null) {
                ctx.header("HX-Push-Url", value)
            }
            field = value
        }

    /**
     * Set HX-Replace-Url header to replace the browser URL without adding to history
     */
    var replaceUrl: String? = null
        set(value) {
            if (value != null) {
                ctx.header("HX-Replace-Url", value)
            }
            field = value
        }
}

/**
 * Configure HTMX response headers using a type-safe DSL
 */
fun Context.htmxResponse(configure: HTMXResponseBuilder.() -> Unit) {
    HTMXResponseBuilder(this).apply(configure)
}

/**
 * Type-safe location setter using HTMXTarget
 */
fun HTMXResponseBuilder.retarget(target: HTMXTarget) {
    retarget = target.selector
}
