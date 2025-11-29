package zoned.framework.dom

import kotlinx.html.Tag
import web.html.HTMLElement

/**
 * A reference to an HTMLElement that is populated synchronously during DSL execution.
 * Use this instead of getElementById to capture elements created via DSL.
 *
 * Usage:
 * ```
 * val buttonRef = Ref<HTMLButtonElement>()
 *
 * button {
 *     ref(buttonRef)
 *     +"Click me"
 * }
 *
 * // In an event handler or onMount:
 * onClick {
 *     buttonRef.element.disabled = true
 * }
 * ```
 *
 * Note: The ref is bound synchronously when ref() is called, so it's
 * immediately available in subsequent code within the same DSL block.
 */
class Ref<T : HTMLElement> {
    private var _element: T? = null

    /**
     * The referenced element.
     * @throws IllegalStateException if accessed before the element is bound
     */
    val element: T
        get() = _element ?: error("Ref not yet bound - element may not exist in DOM yet")

    /**
     * Whether the ref has been populated.
     */
    val isSet: Boolean
        get() = _element != null

    /**
     * Get the element or null if not yet bound.
     */
    fun getOrNull(): T? = _element

    internal fun set(el: T) {
        _element = el
    }
}

/**
 * Capture a reference to this element.
 * The ref is populated synchronously during DSL execution.
 *
 * Usage:
 * ```
 * val myRef = Ref<HTMLDivElement>()
 * div {
 *     ref(myRef)
 *     // myRef.element is now available
 * }
 * ```
 *
 * @throws IllegalStateException if called outside ElementTrackingConsumer context
 */
fun <T : HTMLElement> Tag.ref(ref: Ref<T>) {
    val tracker = getCurrentTracker()
        ?: error("ref() requires ElementTrackingConsumer context")
    val element = tracker.currentElement()
        ?: error("ref() called outside element context")
    @Suppress("UNCHECKED_CAST")
    ref.set(element as T)
}
