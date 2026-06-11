package zoned.framework.routing

import kotlinx.html.div
import kotlinx.html.id
import web.dom.ElementId
import web.dom.document
import zoned.framework.hasDom
import zoned.framework.interop.addToBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ZoneTest {

    @Test
    fun resolveFindsTheZoneElement() {
        if (!hasDom) return
        addToBody { div { id = "zone-resolve-test" } }
        try {
            assertNotNull(Zone("zone-resolve-test").resolve())
            assertNull(Zone("zone-not-in-dom").resolve())
        } finally {
            document.getElementById(ElementId("zone-resolve-test"))?.remove()
        }
    }

    @Test
    fun swapReplacesZoneContentsOnly() {
        if (!hasDom) return
        addToBody {
            div {
                id = "zone-swap-wrapper"
                div { id = "zone-swap-sibling"; +"untouched" }
                div {
                    id = "zone-swap-test"
                    div { id = "zone-swap-old"; +"old content" }
                }
            }
        }
        try {
            val zone = Zone("zone-swap-test")
            zone.swap { div { id = "zone-swap-new"; +"new content" } }

            assertNull(document.getElementById(ElementId("zone-swap-old")), "old zone content is cleared")
            assertNotNull(document.getElementById(ElementId("zone-swap-new")), "new content is rendered into the zone")
            assertEquals(
                "untouched",
                document.getElementById(ElementId("zone-swap-sibling"))?.textContent,
                "content outside the zone is untouched",
            )
            // The new content lives INSIDE the zone element (the zone element itself survives).
            assertEquals(
                "zone-swap-test",
                document.getElementById(ElementId("zone-swap-new"))?.parentElement?.id?.toString(),
            )
        } finally {
            document.getElementById(ElementId("zone-swap-wrapper"))?.remove()
        }
    }

    @Test
    fun swapThrowsWhenZoneNotInDom() {
        if (!hasDom) return
        assertFails { Zone("zone-missing").swap { div { } } }
    }

    @Test
    fun rebuildReplacesTheZoneElementPreservingItsSlot() {
        if (!hasDom) return
        addToBody {
            div {
                id = "zr-wrap"
                div { id = "zr-a" }
                div { id = "zr-zone"; +"old" }
                div { id = "zr-c" }
            }
        }
        try {
            val replaced = Zone("zr-zone").rebuild {
                div { id = "zr-zone"; +"new" }
            }
            assertNotNull(replaced)
            val wrap = document.getElementById(ElementId("zr-wrap"))!!
            val order = (0 until wrap.childElementCount).map { wrap.children[it].id.toString() }
            assertEquals(listOf("zr-a", "zr-zone", "zr-c"), order, "rebuilt zone keeps its slot")
            assertEquals("new", document.getElementById(ElementId("zr-zone"))?.textContent)
        } finally {
            document.getElementById(ElementId("zr-wrap"))?.remove()
        }
    }

    @Test
    fun rebuildOnMissingZoneReturnsNull() {
        if (!hasDom) return
        assertNull(Zone("zr-none").rebuild { div { } })
    }
}
