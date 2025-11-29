package zoned.framework.dom

import kotlinx.html.Tag
import kotlinx.html.TagConsumer
import kotlinx.html.Unsafe
import kotlinx.html.dom.JSDOMBuilder
import web.events.Event
import web.html.HTMLElement

/**
 * Stack of active ElementTrackingConsumers to handle nesting.
 * When addToBody() is called inside another DSL block, we push
 * the new tracker and restore the previous one when done.
 */
private val trackerStack = mutableListOf<ElementTrackingConsumer>()

/**
 * Get the current ElementTrackingConsumer, or null if not inside a tracked DSL context.
 */
fun getCurrentTracker(): ElementTrackingConsumer? = trackerStack.lastOrNull()

/**
 * A TagConsumer wrapper that:
 * 1. Exposes currentElement() for synchronous ref binding
 * 2. Tracks mount callbacks per element depth and executes them on tag end
 * 3. Handles element finalization and appending to parent
 * 4. Properly handles nesting via a tracker stack
 */
class ElementTrackingConsumer(
    private val parent: HTMLElement
) : TagConsumer<HTMLElement> {

    private val builder = JSDOMBuilder<HTMLElement>(parent.ownerDocument!!)
    private val mountCallbacks = mutableListOf<Pair<Int, () -> Unit>>()
    private var depth = 0

    /**
     * Returns the current element being built, or null if not inside a tag.
     */
    fun currentElement(): HTMLElement? = builder.currentElement()

    /**
     * Queue a callback to run when the current tag ends (after children are built).
     * Used for onMount behavior.
     */
    fun queueMountCallback(callback: () -> Unit) {
        mountCallbacks.add(depth to callback)
    }

    override fun onTagStart(tag: Tag) {
        if (depth == 0) {
            // Push ourselves onto the stack when starting our first tag
            trackerStack.add(this)
        }
        depth++
        builder.onTagStart(tag)
    }

    override fun onTagEnd(tag: Tag) {
        // Note: Mount callbacks are NOT executed here because the element
        // isn't in the DOM yet. They're deferred to finalize() after appendChild.
        builder.onTagEnd(tag)
        depth--

        if (depth == 0) {
            // Pop ourselves from the stack when our last tag ends
            trackerStack.removeLastOrNull()
        }
    }

    override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) {
        builder.onTagAttributeChange(tag, attribute, value)
    }

    override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) {
        builder.onTagEvent(tag, event, value)
    }

    override fun onTagContent(content: CharSequence) {
        builder.onTagContent(content)
    }

    override fun onTagContentEntity(entity: kotlinx.html.Entities) {
        builder.onTagContentEntity(entity)
    }

    override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
        builder.onTagContentUnsafe(block)
    }

    override fun onTagComment(content: CharSequence) {
        builder.onTagComment(content)
    }

    override fun finalize(): HTMLElement {
        // Pop ourselves from the stack if we're still on it
        // (in case finalize is called without going through onTagEnd)
        if (trackerStack.lastOrNull() === this) {
            trackerStack.removeLastOrNull()
        }
        val element = builder.finalize()
        parent.appendChild(element)

        // Execute ALL mount callbacks now that element is in the DOM.
        // This is critical for libraries like Leaflet that need to measure
        // the container's dimensions.
        mountCallbacks.forEach { (_, callback) -> callback() }
        mountCallbacks.clear()

        return element
    }
}
