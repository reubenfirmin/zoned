package zoned.framework.routing

import kotlinx.html.TagConsumer
import web.html.HTMLElement

sealed class RouteSegment {
    data class Static(val value: String) : RouteSegment()
    data class Parameter(val name: String) : RouteSegment()
}

fun List<RouteSegment>.toPath() = this.joinToString("/") {
    when (it) {
        is RouteSegment.Static -> it.value
        is RouteSegment.Parameter -> "{${it.name}}"
    }
}

open class Route(
    val pattern: String,
    val segments: List<RouteSegment>,
    val handler: TagConsumer<HTMLElement>.(Params) -> Any) {

    fun path(vararg params: String): String {
        val paramSegments = segments.filterIsInstance<RouteSegment.Parameter>()

        require(params.size == paramSegments.size) { "Expected ${paramSegments.size} parameters, but got ${params.size}." }
        var paramIdx = 0
        return segments.joinToString("/", prefix = "/") { segment ->
            when (segment) {
                is RouteSegment.Static -> segment.value
                is RouteSegment.Parameter -> params[paramIdx++]
            }
        }
    }
}

class FragmentRoute(pattern: String, segments: List<RouteSegment>, handler: TagConsumer<HTMLElement>.(Params) -> Any, val parent: Route, val target: Zone):
    Route(pattern, segments, handler)

/**
 * Not typesafe, but lower boilerplate than the Routes approach
 * ```
 * with (MyClass()) {
 *     route("{path}") { it.renderFunc() }
 * }
 * ```
 */
inline fun <reified T : Any> T.route(path: String, crossinline block: T.(TagConsumer<HTMLElement>) -> Unit) =
    RouteCreator.addRoute(path) {
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
fun <T : Any> route(instance: T, block: T.() -> Pair<String, TagConsumer<HTMLElement>.(Params) -> Any>): Route {
    val (path, handler) = instance.block()
    return RouteCreator.addRoute(path, handler = handler)
}