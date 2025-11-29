package zoned.framework.interop

import web.dom.document
import web.html.HTMLElement
import web.mutation.MutationObserver
import web.mutation.MutationObserverInit

private val destroyCallbacks = mutableMapOf<HTMLElement, MutableList<() -> Unit>>()
private val observers = mutableMapOf<HTMLElement, MutationObserver>()

/**
 * Register a callback to run when this element is removed from the DOM.
 * Useful for cleaning up timers, event listeners, websockets, etc.
 *
 * Example:
 * ```kotlin
 * val intervalId = window.setInterval({ updateDisplay() }, 1000)
 * element.onDestroy {
 *     window.clearInterval(intervalId)
 * }
 * ```
 */
fun HTMLElement.onDestroy(callback: () -> Unit) {
    val callbacks = destroyCallbacks.getOrPut(this) { mutableListOf() }
    callbacks.add(callback)

    if (observers[this] == null) {
        val element = this
        val observer = MutationObserver { _, obs ->
            if (!document.documentElement!!.contains(element)) {
                destroyCallbacks[element]?.forEach { it() }
                destroyCallbacks.remove(element)
                observers.remove(element)
                obs.disconnect()
            }
        }
        document.body?.let {
            observer.observe(it, MutationObserverInit(childList = true, subtree = true))
        }
        observers[this] = observer
    }
}
