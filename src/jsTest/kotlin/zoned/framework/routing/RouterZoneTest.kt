package zoned.framework.routing

import kotlinx.browser.window
import kotlinx.html.div
import kotlinx.html.id
import web.dom.ElementId
import web.dom.document
import zoned.framework.hasDom
import zoned.framework.interop.addToBody
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RouterZoneTest {

    @BeforeTest fun setup() = RouteTrie.clear()
    @AfterTest fun teardown() = RouteTrie.clear()

    @Test
    fun zoneTargetedRouteSwapsOnlyTheZone() {
        if (!hasDom) return
        val originalPath = window.location.pathname
        addToBody {
            div {
                id = "rz-wrap"
                div { id = "rz-sib"; +"sibling" }
                div {
                    id = "rz-zone"
                    div { id = "rz-old"; +"old" }
                }
            }
        }
        try {
            RouteCreator.addRoute("/router-zone-test", target = Zone("rz-zone")) { _ ->
                div { id = "rz-new"; +"swapped" }
            }

            Router.navigate("/router-zone-test")

            val new = document.getElementById(ElementId("rz-new"))
            assertNotNull(new, "route content is rendered")
            assertEquals("rz-zone", new.parentElement?.id?.toString(), "route content lands inside the zone")
            assertEquals(null, document.getElementById(ElementId("rz-old")), "previous zone content is swapped out")
            assertEquals("sibling", document.getElementById(ElementId("rz-sib"))?.textContent, "content outside the zone survives")
        } finally {
            document.getElementById(ElementId("rz-wrap"))?.remove()
            window.history.pushState(null, "", originalPath)
        }
    }

    @Test
    fun coldLoadRendersParentRouteToCreateMissingZone() {
        if (!hasDom) return
        val originalPath = window.location.pathname
        // PARTIAL parent appends the zone's page without clearing the (test runner's) body.
        val parent = RouteCreator.addRoute("/router-zone-cold", mode = RenderMode.PARTIAL) { _ ->
            div {
                id = "rz-cold-wrap"
                div { id = "rz-cold-zone" }
            }
        }
        // The parent affects cold-load bootstrapping only — the fragment declares its own full path.
        RouteCreator.addRoute("/router-zone-cold/frag", Zone("rz-cold-zone"), parent, RenderMode.PARTIAL) { _ ->
            div { id = "rz-cold-content"; +"fragment" }
        }
        try {
            // Deep link straight to the fragment: the zone doesn't exist yet.
            Router.navigate("/router-zone-cold/frag")

            assertNotNull(document.getElementById(ElementId("rz-cold-wrap")), "parent route rendered to create the zone")
            assertEquals("fragment", document.getElementById(ElementId("rz-cold-content"))?.textContent)
        } finally {
            document.getElementById(ElementId("rz-cold-wrap"))?.remove()
            window.history.pushState(null, "", originalPath)
        }
    }
}
