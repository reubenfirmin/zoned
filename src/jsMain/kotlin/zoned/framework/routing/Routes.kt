package zoned.framework.routing

import kotlinx.html.TagConsumer
import web.html.HTMLElement

/**
 * Type safe routing. Declare one collection per page group, each owning a base path:
 *
 *     object helpPages : Routes<HelpPages>(HelpPages(), "/help") {
 *         val shortcuts = route { "/shortcuts" to { shortcutsPage() }}
 *         val topic     = route { "/topic/{name}" to { params -> topicPage(params["name"]) }}
 *     }
 *
 * This gives you typed route handles elsewhere in the app:
 *
 *     helpPages.shortcuts.path()
 *
 * Defining a collection only COLLECTS its routes — nothing is live until the collection is passed
 * to [Router.start] (or [Router.mount]). There is no registration-by-side-effect: a collection you
 * never mount contributes no routes, and a typed [Route.path] reference to it fails loudly.
 */
open class Routes<T : Any>(val resource: T, private val basePath: String) {

    /** The collected routes, inserted into the trie by [Router.mount]. */
    internal val routes = mutableListOf<Route>()

    /** Set by [Router.mount]; gates [Route.path] so links to unmounted collections fail loudly. */
    internal var mounted = false

    /**
     * [target] (optional) makes this a zone-targeted route: the router swaps only that zone's
     * contents instead of touching the body. The zone element must already be in the DOM when the
     * route fires (build it at app startup, or use [fragment] with a parent for cold-load support).
     */
    fun route(mode: RenderMode = RenderMode.FULL_PAGE, target: Zone? = null, block: T.() -> Pair<String, TagConsumer<HTMLElement>.(Params) -> Any>): Route {
        val (path, handler) = resource.block()
        return register(RouteCreator.build(fullPath(path), target, mode = mode, handler = handler))
    }

    /**
     * A zone-targeted route at basePath + path. On a cold load (deep link) where [zone] is not in
     * the DOM yet, [parent] is rendered first to create it. NOTE: [parent] affects bootstrapping
     * only — it does NOT prefix this route's path (the collection's base path does).
     */
    fun fragment(zone: Zone, parent: Route, mode: RenderMode = RenderMode.FULL_PAGE, block: T.() -> Pair<String, TagConsumer<HTMLElement>.(Params) -> Any>): Route {
        val (path, handler) = resource.block()
        return register(RouteCreator.build(fullPath(path), zone, parent, mode, handler))
    }

    private fun fullPath(path: String) = (basePath + path).replace("//", "/")

    private fun register(route: Route): Route {
        route.owner = this
        routes += route
        return route
    }
}
