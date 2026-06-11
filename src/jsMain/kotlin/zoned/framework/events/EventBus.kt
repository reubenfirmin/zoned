package zoned.framework.events

import web.html.HTMLElement
import zoned.framework.interop.onDestroy
import kotlin.reflect.KClass

/**
 * A typed application event bus — the "why" half of the zone paradigm (zones are the "where").
 * [E] is the app's event marker type; subscribers register against concrete event classes.
 *
 * Two subscription scopes:
 *  - [on] / [off]: global. Lives until explicitly removed. A component that can be replaced
 *    (e.g. content rebuilt on a zone swap) must pair them — or use [zoned.framework.routing.ZoneView],
 *    which pairs them automatically.
 *  - [HTMLElement.on]: element-scoped. Auto-pruned when the element leaves the DOM at ANY depth
 *    (shared ElementLifecycle observer), and safe to register before the element is attached —
 *    a parent-scoped observer could do neither.
 *
 * Events fired while another event is dispatching are queued and run afterwards, in order.
 */
class EventBus<E : Any> {

    /** type -> global handlers. Public for diagnostics and tests. */
    val eventRegistry = mutableMapOf<KClass<out E>, MutableList<(E) -> Unit>>()

    /** type -> element -> handler (element-scoped, auto-pruned). Public for diagnostics and tests. */
    val componentRegistry = mutableMapOf<KClass<out E>, MutableMap<HTMLElement, (E) -> Unit>>()

    private val executionQueue = mutableListOf<E>()

    /** Subscribe globally. Pair with [off] (same [action] reference) if the subscriber can go away. */
    inline fun <reified T : E> on(noinline action: (T) -> Unit): EventBus<E> {
        @Suppress("UNCHECKED_CAST")
        eventRegistry.getOrPut(T::class) { mutableListOf() }.add(action as (E) -> Unit)
        return this
    }

    /** Remove a global subscription added via [on]. Unknown references are a no-op. */
    inline fun <reified T : E> off(noinline action: (T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        eventRegistry[T::class]?.remove(action as (E) -> Unit)
    }

    /**
     * Subscribe for as long as this element is in the DOM. Does not add duplicates (re-registering
     * the same element replaces its handler), and prunes itself when the element — or any ancestor —
     * is removed.
     */
    inline fun <reified T : E> HTMLElement.on(noinline action: (T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        componentRegistry.getOrPut(T::class) { mutableMapOf() }[this] = action as (E) -> Unit
        onDestroy { componentRegistry[T::class]?.remove(this) }
    }

    /** Remove every element-scoped subscription for [element]. */
    fun removeAll(element: HTMLElement) {
        componentRegistry.forEach { (_, map) -> map.remove(element) }
    }

    /**
     * Dispatch [event] to its subscribers. If a dispatch is already in progress, the event is
     * queued and runs once the current one (and anything queued before it) completes — handlers
     * never interleave.
     */
    fun fire(event: E) {
        executionQueue.add(event)
        if (!draining) drain()
    }

    private var draining = false

    private fun drain() {
        draining = true
        try {
            while (executionQueue.isNotEmpty()) {
                // Remove BEFORE dispatching: a throwing handler must not leave its event wedged at
                // the head of the queue (which would silence the bus for the rest of the session).
                // Anything still queued when a handler throws dispatches on the next fire().
                val event = executionQueue.removeFirst()
                val actions = eventRegistry[event::class].orEmpty() + componentRegistry[event::class]?.values.orEmpty()
                actions.forEach { it(event) }
            }
        } finally {
            draining = false
        }
    }
}
