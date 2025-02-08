package zoned.framework.routing

import kotlinx.html.TagConsumer
import web.html.HTMLElement

/**
 * Type safe routing.
 *
 *     object myresource: Routes<MyResource>(MyResource(), "/resource") {
 *         val main = route { "/view" to { someView() }}
 *         val main = route { "/view/{param1}/{param2}" to { params -> someView(params["param1"], params["param2"]) }}
 *     }
 *
 * This gives you the ability elsewhere in the app to call:
 *
 *    myresource.main.path
 *
 */
open class Routes<T : Any>(val resource: T, private val basePath: String) {

    fun route(block: T.() -> Pair<String, TagConsumer<HTMLElement>.(Params) -> Any>): Route {
        val (path, handler) = resource.block()
        val fullPath = (basePath + path).replace("//", "/")
        return RouteCreator.addRoute(fullPath, handler = handler)
    }

    fun fragment(zone: Zone, parent: Route, block: T.() -> Pair<String, TagConsumer<HTMLElement>.(Params) -> Any>): Route {
        val (path, handler) = resource.block()
        return RouteCreator.addRoute(path, zone, parent, handler)
    }
}