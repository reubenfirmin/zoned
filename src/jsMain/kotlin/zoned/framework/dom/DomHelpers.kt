package zoned.framework.dom

import kotlinx.html.Tag
import web.dom.Node
import web.html.HTMLElement
import zoned.framework.libs.HTMXHelper

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

/**
 * Insert child nodes into this element.
 * Useful for re-inserting server-rendered content in enhancement implementations.
 *
 * The nodes are appended synchronously during DSL execution.
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
 *
 * @throws IllegalStateException if called outside ElementTrackingConsumer context
 */
fun Tag.insertChildren(nodes: List<Node>) {
    if (nodes.isEmpty()) return

    val tracker = getCurrentTracker()
        ?: error("insertChildren() requires ElementTrackingConsumer context")
    val element = tracker.currentElement()
        ?: error("insertChildren() called outside element context")

    nodes.forEach { node ->
        element.appendChild(node)
    }
    // Re-process HTMX attributes on re-inserted nodes
    // (detaching nodes loses HTMX event bindings)
    HTMXHelper.htmx.process(element)
}

/**
 * Insert a single child node into this element.
 *
 * @throws IllegalStateException if called outside ElementTrackingConsumer context
 */
fun Tag.insertChild(node: Node) {
    val tracker = getCurrentTracker()
        ?: error("insertChild() requires ElementTrackingConsumer context")
    val element = tracker.currentElement()
        ?: error("insertChild() called outside element context")

    element.appendChild(node)
    // Re-process HTMX attributes on re-inserted node
    HTMXHelper.htmx.process(element)
}
