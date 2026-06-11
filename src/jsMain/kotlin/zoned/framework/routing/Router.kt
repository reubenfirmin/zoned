package zoned.framework.routing

import kotlinx.browser.window
import kotlinx.html.*
import web.dom.document
import zoned.framework.Debugger
import zoned.framework.interop.appendTo
import zoned.framework.interop.clear

/**
 * Set up new routes either using the Routes builder, or the route() helper functions (found in Route).
 */
object Router {

    // A handler may call navigate() (the root-route redirect pattern). Re-entering handleRoute
    // mid-route would let the OUTER route finish on top of the redirect's result (title, render).
    // Instead the inner call just flags a pending pass; the loop re-runs for the new URL last.
    private var handling = false
    private var pendingHandle = false

    private fun handleRoute() {
        if (handling) {
            pendingHandle = true
            return
        }
        handling = true
        try {
            do {
                pendingHandle = false
                handleCurrentLocation()
            } while (pendingHandle)
        } finally {
            handling = false
        }
    }

    private fun handleCurrentLocation() {
        val path = window.location.pathname

        RouteTrie.findRoute(path)?.apply {
            val (route, params) = this

            when {
                // Zone-targeted route: swap only the declared zone — everything else on the page
                // (sibling layers, badges, the surrounding chrome) is untouched.
                route is FragmentRoute -> {
                    if (route.target.resolve() == null) {
                        // Cold load (deep link): the page owning the zone hasn't rendered yet, so
                        // run the parent route first (honouring its own render mode) to create it.
                        route.parent?.let { parent -> render(parent, params) }
                    }
                    route.target.swap { route.handler(this, Params(params)) }
                }
                else -> render(route, params)
            }

            route.metadata.title?.let { document.title = it(Params(params)) }

            Debugger.debug("Navigating to ${window.location.pathname}")
        } ?: notFound(path)
    }

    /** Body-level rendering for non-zone routes, per the route's [RenderMode]. */
    private fun render(route: Route, params: Map<String, String>) {
        when (route.mode) {
            RenderMode.FULL_PAGE -> {
                document.body.apply {
                    clear()
                    // Route handler builds DOM synchronously via ElementTrackingConsumer
                    route.handler(appendTo(), Params(params))
                }
            }
            RenderMode.PARTIAL -> {
                // App handles DOM manipulation, just invoke handler
                route.handler(document.body.appendTo(), Params(params))
            }
        }
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

    /** Insert the collections' routes into the trie. Passing a collection IS its registration. */
    fun mount(vararg tables: Routes<*>) {
        for (table in tables) {
            if (table.mounted) continue
            table.routes.forEach(RouteTrie::addRoute)
            table.mounted = true
        }
    }

    /**
     * Mount the given collections and start routing. A collection never passed here contributes
     * no routes (and any typed [Route.path] link to it fails loudly naming the missing mount).
     */
    fun start(vararg tables: Routes<*>) {
        mount(*tables)
        check(!RouteTrie.isEmpty()) {
            "Router.start: no routes registered — pass your Routes collections to start()"
        }
        window.onpopstate = {
            handleRoute()
        }
        handleRoute()
    }
}

