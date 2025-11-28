package zoned.framework.dom

import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.id
import web.html.HTMLElement
import kotlin.random.Random

/**
 * A reference to an HTMLElement that will be populated during DOM flush.
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
 * // In an event handler (runs after flush):
 * onClick {
 *     buttonRef.element.disabled = true
 * }
 * ```
 *
 * Note: Accessing `element` before DOM flush throws an error.
 * Event handlers registered via DSL (onClick, onInput, etc.) are safe
 * because they also run after flush.
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

private fun generateRefId(): String {
    val charPool: List<Char> = ('a'..'z') + ('0'..'9')
    return "ref-" + (1..7)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

/**
 * Capture a reference to this element.
 * The ref will be populated after the element is added to the DOM.
 *
 * Usage:
 * ```
 * val myRef = Ref<HTMLDivElement>()
 * div {
 *     ref(myRef)
 *     // ...
 * }
 * // Later in event handlers:
 * myRef.element.classList.add("active")
 * ```
 */
fun <T : HTMLElement> CommonAttributeGroupFacade.ref(ref: Ref<T>) {
    // Ensure the element has an ID so DomBehavior can find it
    val elementId = try {
        if (id.isBlank()) throw RuntimeException("blank")
        id
    } catch (e: Exception) {
        val newId = generateRefId()
        id = newId
        newId
    }

    DomBehavior.queue(elementId) { element ->
        @Suppress("UNCHECKED_CAST")
        ref.set(element as T)
    }
}
