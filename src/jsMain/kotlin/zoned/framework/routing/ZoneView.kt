package zoned.framework.routing

import kotlinx.html.TagConsumer
import web.html.HTMLElement
import zoned.framework.events.EventBus

/**
 * A [zone] paired with how to render it from a model — the unit of UI in a zoned app.
 *
 * The lifecycle invariant lives here, once: [swap] ALWAYS runs the previous content's cleanups
 * (and detaches its subscriptions) before rendering the new content. Subscriptions registered
 * via [on] during [render] belong to that render's content and cannot outlive it — the
 * leak-on-re-render and forgot-to-dispose bug classes are unrepresentable.
 *
 * This is not a component model: there is no reconciler and no state-driven re-render. [render]
 * runs exactly when the app calls [swap] with an explicit model.
 */
abstract class ZoneView<M, E : Any>(val zone: Zone, val bus: EventBus<E>) {

    @PublishedApi
    internal val cleanups = mutableListOf<() -> Unit>()

    /** The model currently rendered into the zone (null before the first [swap] and after [dispose]). */
    var currentModel: M? = null
        private set

    /** Build this view's content for [model]. Runs inside the zone swap. */
    protected abstract fun TagConsumer<HTMLElement>.render(model: M)

    /** Subscribe for the lifetime of the CURRENT content; auto-detached on the next [swap]/[dispose]. */
    protected inline fun <reified T : E> on(noinline handler: (T) -> Unit) {
        bus.on(handler)
        onDispose { bus.off(handler) }
    }

    /** Register cleanup to run before the current content is replaced (save state, kill timers...). */
    fun onDispose(cleanup: () -> Unit) {
        cleanups += cleanup
    }

    /** Replace the zone's contents for [model]. Teardown of the previous content is unconditional. */
    fun swap(model: M) {
        dispose()
        currentModel = model
        zone.swap { render(model) }
    }

    /** Run and clear the current content's cleanups. Idempotent. */
    fun dispose() {
        currentModel = null
        val toRun = cleanups.toList()
        cleanups.clear()
        toRun.forEach { it() }
    }
}
