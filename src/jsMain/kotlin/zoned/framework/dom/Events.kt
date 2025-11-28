package zoned.framework.dom

import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.id
import web.dom.document
import web.events.Event
import web.events.EventType
import web.events.addEventListener
import web.html.HTMLElement
import web.uievents.DragEvent
import web.uievents.InputEvent
import web.uievents.KeyboardEvent
import web.uievents.MouseEvent
import zoned.framework.interop.onDestroy as elementOnDestroy
import kotlin.random.Random

private fun generateRandomString(length: Int = 7): String {
    val charPool : List<Char> = ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}

private fun CommonAttributeGroupFacade.ensureId(): String {

    try {
        // just accessing id if it's unset currently throws an exception
        if (id.isBlank()) {
            console.log("id was blank; generating")
            // just in case kotlinx.html fixes ^ in future
            throw RuntimeException("id was blank")
        }
    } catch (e: Exception) {
        id = "element-${generateRandomString()}"
    }
    return id
}

private inline fun <reified E : Event> CommonAttributeGroupFacade.attachEvent(
    type: EventType<E>,
    noinline handler: (E) -> Unit
) {
    val id = ensureId()
    DomBehavior.queue(id) { element ->
        element.addEventListener(type, handler)
    }
}

// Standard event listeners
// XXX these all require that id is defined on the element first; the logic above that tries to generate an id isn't working
fun CommonAttributeGroupFacade.onClick(handler: (MouseEvent) -> Unit) = attachEvent<MouseEvent>(EventType("click"), handler)
fun CommonAttributeGroupFacade.onMouseEnter(handler: (MouseEvent) -> Unit) = attachEvent<MouseEvent>(EventType("mouseenter"), handler)
fun CommonAttributeGroupFacade.onMouseLeave(handler: (MouseEvent) -> Unit) = attachEvent<MouseEvent>(EventType("mouseleave"), handler)
fun CommonAttributeGroupFacade.onContextMenu(handler: (MouseEvent) -> Unit) = attachEvent<MouseEvent>(EventType("contextmenu"), handler)
fun CommonAttributeGroupFacade.onSubmit(handler: (Event) -> Unit) = attachEvent<Event>(EventType("submit"), handler)
fun CommonAttributeGroupFacade.onChange(handler: (Event) -> Unit) = attachEvent<Event>(EventType("change"), handler)
fun CommonAttributeGroupFacade.onKeyUp(handler: (KeyboardEvent) -> Unit) = attachEvent<KeyboardEvent>(EventType("keyup"), handler)
fun CommonAttributeGroupFacade.onKeyDown(handler: (KeyboardEvent) -> Unit) = attachEvent<KeyboardEvent>(EventType("keydown"), handler)
fun CommonAttributeGroupFacade.onLoad(handler: (Event) -> Unit) = attachEvent<Event>(EventType("load"), handler)
fun CommonAttributeGroupFacade.onError(handler: (Event) -> Unit) = attachEvent<Event>(EventType("error"), handler)
fun CommonAttributeGroupFacade.onInput(handler: (InputEvent) -> Unit) = attachEvent<InputEvent>(EventType("input"), handler)
fun CommonAttributeGroupFacade.onScroll(handler: (InputEvent) -> Unit) = attachEvent<InputEvent>(EventType("scroll"), handler)

// Drag and drop events
fun CommonAttributeGroupFacade.onDragStart(handler: (DragEvent) -> Unit) = attachEvent<DragEvent>(EventType("dragstart"), handler)
fun CommonAttributeGroupFacade.onDragEnd(handler: (DragEvent) -> Unit) = attachEvent<DragEvent>(EventType("dragend"), handler)
fun CommonAttributeGroupFacade.onDragOver(handler: (DragEvent) -> Unit) = attachEvent<DragEvent>(EventType("dragover"), handler)
fun CommonAttributeGroupFacade.onDragEnter(handler: (DragEvent) -> Unit) = attachEvent<DragEvent>(EventType("dragenter"), handler)
fun CommonAttributeGroupFacade.onDragLeave(handler: (DragEvent) -> Unit) = attachEvent<DragEvent>(EventType("dragleave"), handler)
fun CommonAttributeGroupFacade.onDrop(handler: (DragEvent) -> Unit) = attachEvent<DragEvent>(EventType("drop"), handler)


/**
 * Executed when element becomes visible
 */
fun CommonAttributeGroupFacade.onDisplay(handler: () -> Unit) {
    val id = ensureId()
    DomBehavior.queueDisplay(id, handler)
}

/**
 * Executed when element is added to dom
 */
fun CommonAttributeGroupFacade.onMount(handler: () -> Unit) {
    val id = ensureId()
    DomBehavior.queueMount(id, handler)
}

/**
 * Executed when element is removed from dom.
 * Useful for cleanup (timers, event listeners, etc.)
 */
fun CommonAttributeGroupFacade.onDestroy(handler: () -> Unit) {
    val id = ensureId()
    DomBehavior.queueMount(id) {
        (document.getElementById(id) as? HTMLElement)?.elementOnDestroy(handler)
    }
}
