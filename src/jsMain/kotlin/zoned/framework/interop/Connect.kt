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
    return document.getElementById(ElementId(id))
}

/**
 * Append content to document.body using the DSL.
 * Useful for modals, tooltips, and other body-level elements.
 */
fun addToBody(block: TagConsumer<HTMLElement>.() -> Unit) {
    val body = document.body
    ElementTrackingConsumer(body).block()
}

/**
 * Rebuild this element from scratch, preserving its position among its siblings. The old element
 * is detached BEFORE [block] runs, so id-based lookups (e.g. inside onMount handlers) resolve to
 * the replacement rather than the outgoing element; the replacement is then moved into the old
 * slot. Returns the new element, or null if this element has no parent.
 *
 * NOTE: the final positioning move blurs any focused descendant — callers rebuilding a focusable
 * control should re-focus it afterwards.
 */
fun HTMLElement.rebuildInPlace(block: TagConsumer<HTMLElement>.() -> HTMLElement): HTMLElement? {
    val parent = parentElement ?: return null
    val next = nextSibling
    parent.removeChild(this)
    val replacement = ElementTrackingConsumer(parent).block()
    parent.insertBefore(replacement, next)
    return replacement
}