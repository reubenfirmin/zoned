package zoned.framework.dom

import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.id
import web.dom.Node
import web.html.HTMLElement
import zoned.framework.libs.HTMXHelper
import kotlin.random.Random

// JS interop helper for setting innerHTML
@Suppress("UNUSED_PARAMETER")
private fun setInnerHtmlJs(element: HTMLElement, html: String): Unit =
    js("element.innerHTML = html")

/**
 * Sets the innerHTML of an HTMLElement.
 * Uses JS interop to work around HtmlSource typing.
 */
fun setInnerHtml(element: HTMLElement, html: String) {
    setInnerHtmlJs(element, html)
}

private fun generateHelperId(): String {
    val charPool: List<Char> = ('a'..'z') + ('0'..'9')
    return "dom-" + (1..7)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

private fun CommonAttributeGroupFacade.ensureIdForHelper(): String {
    return try {
        if (id.isBlank()) throw RuntimeException("blank")
        id
    } catch (e: Exception) {
        val newId = generateHelperId()
        id = newId
        newId
    }
}

/**
 * Insert child nodes into this element.
 * Useful for re-inserting server-rendered content in enhancement implementations.
 *
 * The nodes are appended after the element is added to the DOM.
 *
 * Usage:
 * ```
 * fun TagConsumer<HTMLElement>.initMyEnhancement(config: Config, children: List<Node>) {
 *     div("wrapper") {
 *         div("toolbar") { ... }
 *         div("content") {
 *             insertChildren(children)  // Re-insert server content here
 *         }
 *     }
 * }
 * ```
 */
fun CommonAttributeGroupFacade.insertChildren(nodes: List<Node>) {
    if (nodes.isEmpty()) return

    val elementId = ensureIdForHelper()
    DomBehavior.queue(elementId) { element ->
        nodes.forEach { node ->
            element.appendChild(node)
        }
        // Re-process HTMX attributes on re-inserted nodes
        // (detaching nodes loses HTMX event bindings)
        HTMXHelper.htmx.process(element)
    }
}

/**
 * Insert a single child node into this element.
 */
fun CommonAttributeGroupFacade.insertChild(node: Node) {
    val elementId = ensureIdForHelper()
    DomBehavior.queue(elementId) { element ->
        element.appendChild(node)
        // Re-process HTMX attributes on re-inserted node
        HTMXHelper.htmx.process(element)
    }
}
