package zoned.framework.routing

import kotlinx.browser.window
import web.dom.document
import zoned.framework.hasDom
import kotlin.test.Test
import kotlin.test.assertEquals

class RouterRedirectTest {

    private object RedirectPages : Routes<Unit>(Unit, "/rrt") {
        val target = route(mode = RenderMode.PARTIAL) { "/target" to { } }.title { "Target" }
        @Suppress("unused")
        val entry = route(mode = RenderMode.PARTIAL) { "" to { Router.navigate(target.path()) } }.title { "Entry" }
    }

    /**
     * A handler that redirects (the root-route pattern: "" navigates to a default child) must leave
     * the TARGET route's title and URL in place — the outer in-flight navigation must not finish on
     * top of the redirect's result.
     */
    @Test
    fun redirectFromInsideAHandlerLandsOnTheTargetTitle() {
        if (!hasDom) return
        Router.mount(RedirectPages)
        Router.navigate("/rrt")
        assertEquals("/rrt/target", window.location.pathname)
        assertEquals("Target", document.title)
    }
}
