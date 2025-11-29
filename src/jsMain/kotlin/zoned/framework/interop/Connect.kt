package zoned.framework.interop

import kotlinx.html.TagConsumer
import web.dom.Element
import web.dom.ElementId
import web.dom.document
import web.html.HTMLCollection
import web.html.HTMLElement
import zoned.framework.dom.ElementTrackingConsumer

fun Element.clear() {
    while (firstChild != null) {
        removeChild(firstChild!!)
    }
}

/**
 * Build child elements using kotlinx.html DSL with element tracking.
 * Returns an ElementTrackingConsumer that supports synchronous ref binding
 * and mount callbacks.
 */
fun HTMLElement.appendTo(): ElementTrackingConsumer = ElementTrackingConsumer(this)

fun <T : Element> HTMLCollection<T>.firstOrNull(): T? {
    return if (length > 0) get(0) else null
}

fun getHtmlElement(id: String): HTMLElement? {
    return document.getElementById(ElementId(id)) as? HTMLElement
}

/**
 * Append content to document.body using the DSL.
 * Useful for modals, tooltips, and other body-level elements.
 */
fun addToBody(block: TagConsumer<HTMLElement>.() -> Unit) {
    val body = document.body ?: error("document.body not available")
    ElementTrackingConsumer(body).block()
}