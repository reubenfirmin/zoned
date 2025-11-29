package zoned.framework.routing

import kotlinx.browser.window
import kotlinx.html.*
import web.dom.document
import zoned.framework.interop.appendTo
import zoned.framework.interop.clear

/**
 * Set up new routes either using the Routes builder, or the route() helper functions (found in Route).
 */
object Router {

    private fun handleRoute() {
        val path = window.location.pathname

        RouteTrie.findRoute(path)?.apply {
            val (route, params) = this

            when (route.mode) {
                RenderMode.FULL_PAGE -> {
                    document.body.apply {
                        clear()
                        val consumer = appendTo()
                        // Route handler builds DOM synchronously via ElementTrackingConsumer
                        route.handler(consumer, Params(params))
                    }
                }
                RenderMode.PARTIAL -> {
                    // App handles DOM manipulation, just invoke handler
                    val consumer = document.body.appendTo()
                    route.handler(consumer, Params(params))
                }
            }

            console.log("Navigating to ${window.location.pathname}")
        } ?: notFound(path)
    }

    private fun notFound(path: String) {
        document.body.apply {
            clear()
            appendTo().h1 {
                +"Sorry, couldn't find what you are looking for"
                console.log("Was looking for $path")
                console.log(RouteTrie.visualize())
            }
        }
    }

    fun navigate(path: String) {
        window.history.pushState(null, "", path)
        handleRoute()
    }

    fun start() {
        window.onpopstate = {
            handleRoute()
        }
        handleRoute()
    }
}

