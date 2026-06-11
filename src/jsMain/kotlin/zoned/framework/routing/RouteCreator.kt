package zoned.framework.routing

import kotlinx.html.TagConsumer
import web.html.HTMLElement

object RouteCreator {

    /**
     * Parse [path] into a [Route] (a [FragmentRoute] when [target] is given) WITHOUT registering
     * it — [Routes] collections are mounted explicitly via [Router.mount]/[Router.start].
     */
    fun build(
        path: String,
        target: Zone? = null,
        parent: Route? = null,
        mode: RenderMode = RenderMode.FULL_PAGE,
        handler: TagConsumer<HTMLElement>.(Params) -> Any
    ): Route {
        require(parent == null || target != null) { "Target must be specified along with parent" }

        val segments = path.split("/").filter {
            it.isNotEmpty()
        }.map { segment ->
            when {
                segment.startsWith("{") && segment.endsWith("...}") -> {
                    RouteSegment.Wildcard(segment.trim('{', '.', '}'))
                }
                segment.startsWith("{") && segment.endsWith("}") -> {
                    RouteSegment.Parameter(segment.trim('{', '}'))
                }
                else -> {
                    RouteSegment.Static(segment)
                }
            }
        }
        return if (target != null) {
            FragmentRoute(path, segments, mode, handler, parent, target)
        } else {
            Route(path, segments, mode, handler)
        }
    }

    /**
     * Build AND immediately register a standalone route (no collection). The escape hatch for
     * prototypes and tests; apps composed of [Routes] collections should mount via [Router.start].
     */
    fun addRoute(
        path: String,
        target: Zone? = null,
        parent: Route? = null,
        mode: RenderMode = RenderMode.FULL_PAGE,
        handler: TagConsumer<HTMLElement>.(Params) -> Any
    ): Route = build(path, target, parent, mode, handler).also(RouteTrie::addRoute)
}
