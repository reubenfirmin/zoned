package zoned.framework.ui.components.icons

import kotlinx.html.div
import kotlinx.html.stream.createHTML
import org.jsoup.Jsoup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BellIconTest {

    private fun renderBell(filled: Boolean): String =
        createHTML().div { bellIcon(filled = filled) }

    private fun pathData(html: String): String =
        Jsoup.parse(html).select("path").joinToString("|") { it.attr("d") }

    @Test
    fun `filled bell renders different geometry than outline bell`() {
        // A solid/filled bell is a distinct shape from the outlined bell; if both
        // variants emit the same path the `filled` flag is silently a no-op.
        assertNotEquals(
            pathData(renderBell(filled = false)),
            pathData(renderBell(filled = true)),
            "filled and outline bell must use different path geometry"
        )
    }

    @Test
    fun `filled bell fills with currentColor and has no stroke`() {
        val doc = Jsoup.parse(renderBell(filled = true))
        val svg = doc.selectFirst("svg")!!
        assertEquals("currentColor", svg.attr("fill"))
        assertTrue(
            doc.select("path").all { it.attr("stroke").isEmpty() },
            "filled icon paths should not set a stroke"
        )
    }

    @Test
    fun `outline bell has no fill and strokes with currentColor`() {
        val doc = Jsoup.parse(renderBell(filled = false))
        val svg = doc.selectFirst("svg")!!
        assertEquals("none", svg.attr("fill"))
        assertTrue(
            doc.select("path").all { it.attr("stroke") == "currentColor" },
            "outline icon paths should stroke with currentColor"
        )
    }
}
