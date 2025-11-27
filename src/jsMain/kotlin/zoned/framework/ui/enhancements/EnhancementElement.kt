package zoned.framework.ui.enhancements

import kotlinx.css.CssBuilder
import web.dom.Document
import web.dom.Element
import web.html.HTMLElement
import zoned.framework.interop.Direction
import zoned.framework.interop.collapse
import zoned.framework.interop.css
import zoned.framework.interop.expand
import zoned.framework.interop.fadeIn
import zoned.framework.interop.fadeOut
import zoned.framework.interop.fadeOutAndRemove
import zoned.framework.interop.onDestroy
import zoned.framework.interop.slideIn
import zoned.framework.interop.slideOut

/**
 * Type-safe wrapper for DOM elements in enhancement implementations.
 * Provides a clean DSL for styling, events, and DOM manipulation.
 *
 * Usage:
 * ```kotlin
 * actual fun initMyEnhancement(element: EnhancementElement, config: MyConfig) {
 *     element.css {
 *         display = Display.flex
 *         gap = 8.px
 *     }
 *
 *     element.appendChild(element.create.div {
 *         className = "toolbar"
 *         css { background = Color("#1f2937") }
 *         onClick { println("clicked!") }
 *     })
 * }
 * ```
 */
class EnhancementElement(val raw: HTMLElement) {

    /** The underlying Document for creating new elements */
    private val document: Document get() = raw.ownerDocument

    /** Factory for creating child elements with DSL syntax */
    val create: ElementFactory = ElementFactory(document)

    // ========== Styling ==========

    /**
     * Apply type-safe CSS styles using kotlin-css CssBuilder.
     */
    fun css(block: CssBuilder.() -> Unit) = raw.css(block)

    // ========== Event Handlers ==========

    fun onClick(handler: () -> Unit) {
        raw.asDynamic().onclick = { handler() }
    }

    fun onClick(handler: (dynamic) -> Unit) {
        raw.asDynamic().onclick = handler
    }

    fun onInput(handler: () -> Unit) {
        raw.asDynamic().oninput = { handler() }
    }

    fun onInput(handler: (dynamic) -> Unit) {
        raw.asDynamic().oninput = handler
    }

    fun onMouseOver(handler: () -> Unit) {
        raw.asDynamic().onmouseover = { handler() }
    }

    fun onMouseOut(handler: () -> Unit) {
        raw.asDynamic().onmouseout = { handler() }
    }

    fun onKeyDown(handler: (dynamic) -> Unit) {
        raw.asDynamic().onkeydown = handler
    }

    fun onKeyUp(handler: (dynamic) -> Unit) {
        raw.asDynamic().onkeyup = handler
    }

    fun onFocus(handler: () -> Unit) {
        raw.asDynamic().onfocus = { handler() }
    }

    fun onBlur(handler: () -> Unit) {
        raw.asDynamic().onblur = { handler() }
    }

    // ========== DOM Manipulation ==========

    fun appendChild(child: Element) {
        raw.appendChild(child)
    }

    fun appendChild(child: EnhancementElement) {
        raw.appendChild(child.raw)
    }

    fun removeChild(child: Element) {
        raw.removeChild(child)
    }

    fun clear() {
        raw.innerHTML = ""
    }

    fun focus() {
        raw.focus()
    }

    // ========== Properties ==========

    var innerHTML: String
        get() = raw.innerHTML
        set(value) { raw.innerHTML = value }

    var textContent: String
        get() = raw.textContent ?: ""
        set(value) { raw.textContent = value }

    var className: String
        get() = raw.className
        set(value) { raw.className = value }

    /** Element ID - useful for passing to libraries like Leaflet that need element IDs */
    val id: String get() = raw.id

    // ========== Attributes ==========

    fun setAttribute(name: String, value: String) {
        raw.setAttribute(name, value)
    }

    fun getAttribute(name: String): String? {
        return raw.getAttribute(name)
    }

    fun hasAttribute(name: String): Boolean {
        return raw.hasAttribute(name)
    }

    fun removeAttribute(name: String) {
        raw.removeAttribute(name)
    }

    // ========== Queries ==========

    fun querySelector(selector: String): Element? {
        return raw.querySelector(selector)
    }

    fun querySelectorAll(selector: String) = raw.querySelectorAll(selector)

    // ========== Lifecycle ==========

    /**
     * Register a callback to run when this element is removed from the DOM.
     * Useful for cleaning up timers, event listeners, websockets, etc.
     */
    fun onDestroy(callback: () -> Unit) = raw.onDestroy(callback)

    // ========== Animations ==========

    /** Fade element in from opacity 0 to 1. */
    fun fadeIn(durationMs: Int, onComplete: (() -> Unit)? = null) = raw.fadeIn(durationMs, onComplete)

    /** Fade element out from current opacity to 0. */
    fun fadeOut(durationMs: Int, onComplete: (() -> Unit)? = null) = raw.fadeOut(durationMs, onComplete)

    /** Fade element out, then remove it from the DOM. */
    fun fadeOutAndRemove(durationMs: Int) = raw.fadeOutAndRemove(durationMs)

    /** Slide element in from the specified direction to its natural position. */
    fun slideIn(direction: Direction, durationMs: Int, onComplete: (() -> Unit)? = null) = raw.slideIn(direction, durationMs, onComplete)

    /** Slide element out in the specified direction. */
    fun slideOut(direction: Direction, durationMs: Int, onComplete: (() -> Unit)? = null) = raw.slideOut(direction, durationMs, onComplete)

    /** Collapse element height to 0 (accordion-style). */
    fun collapse(durationMs: Int, onComplete: (() -> Unit)? = null) = raw.collapse(durationMs, onComplete)

    /** Expand element from height 0 to its natural height (accordion-style). */
    fun expand(durationMs: Int, onComplete: (() -> Unit)? = null) = raw.expand(durationMs, onComplete)

    companion object {
        /**
         * Wrap an Element, returning null if it's not an HTMLElement.
         */
        fun wrap(element: Element): EnhancementElement? {
            return (element as? HTMLElement)?.let { EnhancementElement(it) }
        }

        /**
         * Wrap an Element, throwing if it's not an HTMLElement.
         */
        fun wrapOrThrow(element: Element): EnhancementElement {
            return wrap(element) ?: error("Enhancement requires an HTMLElement, got ${element::class.simpleName}")
        }
    }
}
