package zoned.framework.routing

import kotlinx.html.TagConsumer
import web.html.HTMLElement

sealed class RouteSegment {
    data class Static(val value: String) : RouteSegment()
    data class Parameter(val name: String) : RouteSegment()
    data class Wildcard(val name: String) : RouteSegment()
}

fun List<RouteSegment>.toPath() = this.joinToString("/") {
    when (it) {
        is RouteSegment.Static -> it.value
        is RouteSegment.Parameter -> "{${it.name}}"
        is RouteSegment.Wildcard -> "{${it.name}...}"
    }
}

enum class RenderMode {
    FULL_PAGE,  // Clear and re-render entire page
    PARTIAL     // Partial update (app handles DOM manipulation)
}

data class RouteMetadata(
    var title: ((Params) -> String)? = null
)

open class Route(
    val pattern: String,
    val segments: List<RouteSegment>,
    val mode: RenderMode = RenderMode.FULL_PAGE,
    val handler: TagConsumer<HTMLElement>.(Params) -> Any) {

    val metadata = RouteMetadata()

    /** The collection this route was declared in, or null for standalone routes. */
    internal var owner: Routes<*>? = null

    /**
     * A typed link to an unmounted collection is a programming error (the route would 404), so it
     * fails here — at first link render — with the fix in the message, instead of silently.
     */
    private fun checkMounted() {
        val collection = owner ?: return   // standalone routes register immediately; nothing to check
        check(collection.mounted) {
            "Route '$pattern' belongs to a Routes collection that was never passed to Router.start()/mount()"
        }
    }

    fun path(vararg params: String): String {
        checkMounted()
        // Wildcards consume an argument too — counting only Parameters would reject every call
        // on a wildcard route (and index out of bounds on a zero-arg one).
        val fillable = segments.count { it !is RouteSegment.Static }

        require(params.size == fillable) { "Expected $fillable parameters, but got ${params.size}." }
        var paramIdx = 0
        return segments.joinToString("/", prefix = "/") { segment ->
            when (segment) {
                is RouteSegment.Static -> segment.value
                is RouteSegment.Parameter -> params[paramIdx++]
                is RouteSegment.Wildcard -> params[paramIdx++]
            }
        }
    }

    fun path(params: Map<String, String>): String {
        checkMounted()
        return segments.joinToString("/", prefix = "/") { segment ->
            when (segment) {
                is RouteSegment.Static -> segment.value
                is RouteSegment.Parameter -> params[segment.name]
                    ?: throw IllegalArgumentException("Missing parameter: ${segment.name}")
                is RouteSegment.Wildcard -> params[segment.name]
                    ?: throw IllegalArgumentException("Missing wildcard parameter: ${segment.name}")
            }
        }
    }

    fun title(block: (Params) -> String): Route {
        metadata.title = block
        return this
    }
}

/**
 * A route that renders into a declared [target] zone instead of the body: the router clears just
 * that zone and builds the handler's content into it (htmx-style swap). [parent] is optional — it
 * prefixes the path and, on a cold load (deep link) where the zone isn't in the DOM yet, is
 * rendered first to create it.
 */
class FragmentRoute(pattern: String, segments: List<RouteSegment>, mode: RenderMode, handler: TagConsumer<HTMLElement>.(Params) -> Any, val parent: Route?, val target: Zone):
    Route(pattern, segments, mode, handler)

/**
 * Not typesafe, but lower boilerplate than the Routes approach
 * ```
 * with (MyClass()) {
 *     route("{path}") { it.renderFunc() }
 * }
 * ```
 */
inline fun <reified T : Any> T.route(path: String, mode: RenderMode = RenderMode.FULL_PAGE, crossinline block: T.(TagConsumer<HTMLElement>) -> Unit) =
    RouteCreator.addRoute(path, mode = mode) {
        block(this@route, this)
    }


/**
 * Not typesafe, but lower boilerplate than the Routes approach
 *
 * ```
 * route (MyClass()) {
 *     "{path}" to { renderFunc() }
 * }
 * ```
 */
fun <T : Any> route(instance: T, mode: RenderMode = RenderMode.FULL_PAGE, block: T.() -> Pair<String, TagConsumer<HTMLElement>.(Params) -> Any>): Route {
    val (path, handler) = instance.block()
    return RouteCreator.addRoute(path, mode = mode, handler = handler)
}