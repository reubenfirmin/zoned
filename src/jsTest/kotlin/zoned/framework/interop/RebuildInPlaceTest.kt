package zoned.framework.interop

import kotlinx.html.div
import kotlinx.html.id
import web.dom.ElementId
import web.dom.document
import web.html.HTMLElement
import zoned.framework.hasDom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RebuildInPlaceTest {

    @Test
    fun rebuildPreservesPositionAmongSiblings() {
        if (!hasDom) return
        addToBody {
            div {
                id = "rb-wrap"
                div { id = "rb-a" }
                div { id = "rb-b"; +"old" }
                div { id = "rb-c" }
            }
        }
        try {
            val old = document.getElementById(ElementId("rb-b")) as HTMLElement
            val replacement = old.rebuildInPlace {
                div { id = "rb-b"; +"new" }
            }
            assertNotNull(replacement)
            assertEquals("new", replacement.textContent)

            val wrap = document.getElementById(ElementId("rb-wrap"))!!
            val order = (0 until wrap.childElementCount).map { wrap.children[it].id.toString() }
            assertEquals(listOf("rb-a", "rb-b", "rb-c"), order, "the rebuilt element keeps its slot")
        } finally {
            document.getElementById(ElementId("rb-wrap"))?.remove()
        }
    }
}
