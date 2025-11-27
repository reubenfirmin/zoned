package zoned.framework.ui.components

import kotlinx.html.HTMLTag
import kotlinx.html.unsafe

/**
 * Renders user-generated HTML content safely within kotlinx.html templates.
 *
 * This function sanitizes HTML to be valid XHTML (required by kotlinx.html's unsafe block)
 * by ensuring self-closing tags are properly formatted.
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
 * Sanitizes HTML to be valid XHTML for kotlinx.html unsafe blocks.
 * Converts self-closing tags to proper XHTML format.
 */
private fun sanitizeToXhtml(html: String): String {
    return html
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "<br/>")
        .replace(Regex("<hr\\s*/?>", RegexOption.IGNORE_CASE), "<hr/>")
        .replace(Regex("<img([^>]*?)\\s*/?>", RegexOption.IGNORE_CASE), "<img$1/>")
        .replace(Regex("<input([^>]*?)\\s*/?>", RegexOption.IGNORE_CASE), "<input$1/>")
}
