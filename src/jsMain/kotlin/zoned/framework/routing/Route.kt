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

    fun path(vararg params: String): String {
        val paramSegments = segments.filterIsInstance<RouteSegment.Parameter>()

        require(params.size == paramSegments.size) { "Expected ${paramSegments.size} parameters, but got ${params.size}." }
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

class FragmentRoute(pattern: String, segments: List<RouteSegment>, mode: RenderMode, handler: TagConsumer<HTMLElement>.(Params) -> Any, val parent: Route, val target: Zone):
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