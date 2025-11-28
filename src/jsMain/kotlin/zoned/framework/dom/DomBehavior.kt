package zoned.framework.dom

import web.dom.Element
import web.dom.document
import web.dom.observers.*
import web.events.EventHandler
import web.window.window

/**
 * Bridge between the kotlinx.html DSL and kotlin-js. Manages deferred behaviors that need
 * to run after elements are added to the DOM.
 *
 * ## Execution Order
 * When elements are added to the DOM, [flush] is called which executes in this order:
 * 1. **Behaviors** (ref bindings) - populates [Ref] objects with their elements
 * 2. **Display callbacks** - sets up intersection observers for lazy loading
 * 3. **Mount callbacks** - runs [onMount] callbacks
 *
 * This order is critical: mount callbacks often access refs, so refs must be bound first.
 *
 * ## How it works
 * - DSL execution queues behaviors and callbacks (elements not yet in DOM)
 * - MutationObserver detects when elements are added to DOM
 * - [flush] is called to execute queued items in the correct order
 */
object DomBehavior {
    private val behaviors = mutableListOf<Pair<String, (Element) -> Unit>>()
    private val mountCallbacks = mutableMapOf<String, () -> Unit>()
    private val displayCallbacks = mutableListOf<Pair<String, () -> Unit>>()
    private lateinit var observer: IntersectionObserver
    private lateinit var mutationObserver: MutationObserver

    fun queue(id: String, behavior: (Element) -> Unit) {
        behaviors.add(id to behavior)
    }

    fun queueDisplay(id: String, callback: () -> Unit) {
        displayCallbacks.add(id to callback)
    }

    fun queueMount(id: String, callback: () -> Unit) {
        mountCallbacks[id] = callback
    }

    private fun executeMountCallback(id: String) {
        mountCallbacks[id]?.let { callback ->
            callback()
            mountCallbacks.remove(id)
        }
    }

    fun flush() {
        behaviors.forEach { (id, behavior) ->
            document.getElementById(id)?.let { element ->
                behavior(element.unsafeCast<Element>())
            }
        }
        behaviors.clear()

        displayCallbacks.forEach { (id, callback) ->
            document.getElementById(id)?.let { element ->
                observer.observe(element)
            }
        }

        // Execute mount callbacks for elements already in the DOM
        mountCallbacks.keys.toList().forEach { id ->
            document.getElementById(id)?.let {
                executeMountCallback(id)
            }
        }
    }

    private fun applyAll() {
        observer = IntersectionObserver({ entries, _ ->
            entries.forEach { entry ->
                if (entry.isIntersecting) {
                    val id = entry.target.id
                    displayCallbacks.find { it.first == id }?.second?.invoke()
                    observer.unobserve(entry.target)
                    displayCallbacks.removeAll { it.first == id }
                }
            }
        }, IntersectionObserverInit(
            root = null,
            rootMargin = "0px",
            threshold = arrayOf(0.0)
        ))

        // IMPORTANT: We must call flush() which binds refs BEFORE running mount callbacks.
        // Mount callbacks often access refs, so the order matters.
        mutationObserver = MutationObserver { mutations, _ ->
            var shouldFlush = false
            mutations.forEach { mutation ->
                when (mutation.type) {
                    MutationRecordType.childList -> {
                        if (mutation.addedNodes.length > 0 || mutation.removedNodes.length > 0) {
                            shouldFlush = true
                        }
                    }
                    MutationRecordType.attributes -> {
                        if (mutation.attributeName == "id") {
                            shouldFlush = true
                        }
                    }
                    else -> {}
                }
            }
            if (shouldFlush) {
                flush()
            }
        }

        mutationObserver.observe(document, MutationObserverInit (
            childList = true,
            subtree = true,
            attributes = true,
            attributeFilter = arrayOf("id")
        ))

        flush()
    }

    init {
        window.onload = EventHandler {
            applyAll()
        }
    }
}

