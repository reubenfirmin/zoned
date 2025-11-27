package zoned.framework.routing

import kotlinx.html.TagConsumer
import web.html.HTMLElement

object RouteCreator {

    fun addRoute(
        path: String,
        target: Zone? = null,
        parent: Route? = null,
        mode: RenderMode = RenderMode.FULL_PAGE,
        handler: TagConsumer<HTMLElement>.(Params) -> Any
    ): Route {
        val fullPath = if (parent != null) {
            parent.segments.toPath() + "/" + path
        } else {
            path
        }

        val segments = fullPath.split("/").filter {
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
        val route = Route(fullPath, segments, mode, handler)
        if (parent != null) {
            require(target != null) {"Target must be specified along with parent"}
            RouteTrie.addRoute(FragmentRoute(route.pattern, segments, mode, handler, parent, target))
        } else {
            RouteTrie.addRoute(route)
        }
        return route
    }
}