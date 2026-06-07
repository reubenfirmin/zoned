package zoned.framework.ui.components

import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [sanitizeToXhtml] must turn arbitrary (possibly malformed) WYSIWYG HTML into well-formed
 * XHTML for kotlinx.html's `unsafe` block. These tests pin the contract: well-formedness,
 * self-closed void elements, escaped bare ampersands, and correct handling of `>` inside
 * attribute values (the case a regex could not get right).
 */
class UserHtmlTest {

    /** Parses `sanitized` as strict XML; with no DOCTYPE only numeric refs and the five XML
     *  predefined entities are legal — i.e. exactly the XHTML subset the output must satisfy. */
    private fun assertWellFormedXml(sanitized: String) {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        builder.parse(ByteArrayInputStream("<root>$sanitized</root>".toByteArray(StandardCharsets.UTF_8)))
    }

    @Test
    fun `self-closes void elements`() {
        assertEquals("<br />", sanitizeToXhtml("<br>"))
        assertEquals("<hr />", sanitizeToXhtml("<hr>"))
        assertEquals("""<img src="x.png" />""", sanitizeToXhtml("""<img src="x.png">"""))
    }

    @Test
    fun `handles a greater-than inside an attribute value`() {
        val out = sanitizeToXhtml("""<img alt="a > b" src="x">""")
        assertWellFormedXml(out)
        val img = Jsoup.parse(out).selectFirst("img")!!
        assertEquals("a > b", img.attr("alt"))
        assertEquals("x", img.attr("src"))
    }

    @Test
    fun `escapes bare ampersands`() {
        assertEquals("Smith &amp; Co", sanitizeToXhtml("Smith & Co"))
        assertWellFormedXml(sanitizeToXhtml("Smith & Co"))
    }

    @Test
    fun `produces well-formed xml for entity-heavy input`() {
        // smart quotes, nbsp, an undeclared-in-XML named entity, a bare ampersand, and a void tag
        assertWellFormedXml(sanitizeToXhtml("caf&eacute; &ldquo;hi&rdquo; &nbsp; &copy; A & B<br>"))
    }

    @Test
    fun `repairs unbalanced tags into well-formed output`() {
        assertWellFormedXml(sanitizeToXhtml("<p>oops <strong>bold"))
    }

    @Test
    fun `leaves already well-formed content untouched`() {
        val input = "<p>Hello <strong>world</strong></p>"
        assertEquals(input, sanitizeToXhtml(input))
    }
}
