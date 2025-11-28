package zoned.framework.ui.enhancements

import kotlinx.html.TagConsumer
import web.dom.Element
import web.html.HTMLElement
import zoned.framework.interop.appendTo

/**
 * Minimal wrapper for DOM elements in enhancement implementations.
 * Use [raw] to access the underlying HTMLElement, and [appendTo] to build DOM with kotlinx.html.
 *
 * Usage:
 * ```kotlin
 * fun initMyEnhancement(element: EnhancementElement, config: MyConfig) {
 *     element.raw.appendTo().div {
 *         id = "toolbar"
 *         onClick { e -> println("clicked!") }
 *     }
 * }
 * ```
 */
class EnhancementElement(val raw: HTMLElement) {

    /** Build child elements using kotlinx.html DSL */
    fun appendTo(): TagConsumer<HTMLElement> = raw.appendTo()

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
