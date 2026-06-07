package zoned.framework.ui.components

import kotlinx.html.HTMLTag
import kotlinx.html.unsafe
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities

/**
 * Renders user-generated HTML content safely within kotlinx.html templates.
 *
 * This function sanitizes HTML to be valid XHTML (required by kotlinx.html's unsafe block)
 * by ensuring tags are well-formed (void elements self-closed, entities escaped).
 *
 * Use this for content from WYSIWYG editors or other user inputs that contain HTML.
 *
 * Example:
 * ```kotlin
 * div("prose") {
 *     userHtml(note.content)
 * }
 * ```
 *
 * @param html The HTML string to render (typically from a WYSIWYG editor)
 */
fun HTMLTag.userHtml(html: String) {
    unsafe { +sanitizeToXhtml(html) }
}

/**
 * Sanitizes HTML into well-formed XHTML for kotlinx.html unsafe blocks.
 *
 * Parses the (possibly malformed) input with jsoup and re-serializes it in XML syntax: void
 * elements are self-closed (`<br/>`, `<img .../>`), bare/undeclared ampersands are escaped, and
 * `>` characters inside attribute values are handled correctly — none of which a regex can do
 * reliably. jsoup also repairs unbalanced tags, which is desirable for untrusted WYSIWYG input.
 */
internal fun sanitizeToXhtml(html: String): String {
    val doc = Jsoup.parseBodyFragment(html)
    doc.outputSettings()
        .syntax(Document.OutputSettings.Syntax.xml)   // self-close void tags, well-formed output
        .escapeMode(Entities.EscapeMode.xhtml)        // escape `&`, normalize entities
        .prettyPrint(false)                           // don't reflow user whitespace
    return doc.body().html()
}
