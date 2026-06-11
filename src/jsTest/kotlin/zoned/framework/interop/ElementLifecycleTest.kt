package zoned.framework.interop

import kotlinx.browser.window
import kotlinx.html.div
import kotlinx.html.id
import web.dom.ElementId
import web.dom.document
import web.html.HTMLElement
import zoned.framework.hasDom
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Characterization tests for [onDestroy] semantics that the shared-observer rewrite must preserve:
 * the callback fires for direct removal AND when an ancestor is removed.
 */
class ElementLifecycleTest {

    private fun assertEventually(check: () -> Boolean, message: String): Promise<Unit> =
        Promise { resolve, reject ->
            window.setTimeout({
                if (check()) resolve(Unit) else reject(AssertionError(message))
            }, 20)
        }

    @Test
    fun onDestroyFiresOnDirectRemoval(): Promise<Unit> {
        if (!hasDom) return Promise.resolve(Unit)
        var fired = false
        addToBody { div { id = "lc-direct" } }
        val el = document.getElementById(ElementId("lc-direct")) as HTMLElement
        el.onDestroy { fired = true }
        el.remove()
        return assertEventually({ fired }, "onDestroy should fire when the element is removed")
    }

    @Test
    fun onDestroyFiresWhenAncestorIsRemoved(): Promise<Unit> {
        if (!hasDom) return Promise.resolve(Unit)
        var fired = false
        addToBody {
            div {
                id = "lc-outer"
                div { id = "lc-inner" }
            }
        }
        val inner = document.getElementById(ElementId("lc-inner")) as HTMLElement
        inner.onDestroy { fired = true }
        document.getElementById(ElementId("lc-outer"))?.remove()
        return assertEventually({ fired }, "onDestroy should fire when an ancestor is removed")
    }

    @Test
    fun onDestroyDoesNotFireWhileElementStaysAttached(): Promise<Unit> {
        if (!hasDom) return Promise.resolve(Unit)
        var fired = false
        addToBody { div { id = "lc-stays" } }
        addToBody { div { id = "lc-noise" } }
        val el = document.getElementById(ElementId("lc-stays")) as HTMLElement
        el.onDestroy { fired = true }
        // Unrelated body mutation must not trigger the callback.
        document.getElementById(ElementId("lc-noise"))?.remove()
        return Promise { resolve, reject ->
            window.setTimeout({
                document.getElementById(ElementId("lc-stays"))?.remove()
                if (!fired) resolve(Unit) else reject(AssertionError("onDestroy fired while element was still attached"))
            }, 20)
        }
    }
}
