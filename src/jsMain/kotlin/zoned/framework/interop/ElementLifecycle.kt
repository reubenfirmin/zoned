package zoned.framework.interop

import web.dom.document
import web.html.HTMLElement
import web.mutation.MutationObserver
import web.mutation.MutationObserverInit

private val destroyCallbacks = mutableMapOf<HTMLElement, MutableList<() -> Unit>>()
private var sharedObserver: MutationObserver? = null

/**
 * Register a callback to run when this element is removed from the DOM.
 * Useful for cleaning up timers, event listeners, websockets, etc.
 *
 * Removal of an ANCESTOR counts as removal too (e.g. a zone swap tearing down a subtree), which is
 * why a single shared observer watches the body subtree rather than each element's direct parent:
 * a parent-scoped observer would never fire for ancestor teardown. One observer serves every
 * registration — per body mutation the work is one connectivity check per registered element,
 * instead of the previous one-observer-per-element fan-out.
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
    destroyCallbacks.getOrPut(this) { mutableListOf() }.add(callback)
    ensureObserver()
}

private fun ensureObserver() {
    if (sharedObserver != null) return
    val observer = MutationObserver { mutations, obs ->
        // Only removals can disconnect an element; skip attribute/addition-only batches.
        if (mutations.none { it.removedNodes.length > 0 }) return@MutationObserver
        val removed = destroyCallbacks.keys.filter { !it.isConnected }
        removed.forEach { element ->
            destroyCallbacks.remove(element)?.forEach { it() }
        }
        if (destroyCallbacks.isEmpty()) {
            obs.disconnect()
            sharedObserver = null
        }
    }
    document.body.let {
        observer.observe(it, MutationObserverInit(childList = true, subtree = true))
        sharedObserver = observer
    }
}
