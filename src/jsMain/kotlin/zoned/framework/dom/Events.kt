package zoned.framework.dom

import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.Tag
import kotlinx.html.js.*
import web.dnd.DragEvent
import web.events.Event
import web.html.HTMLElement
import web.input.InputEvent
import web.keyboard.KeyboardEvent
import web.mouse.MouseEvent
import zoned.framework.interop.onDestroy as elementOnDestroy

/**
 * DSL event handlers - delegate to kotlinx.html's typed events.
 * These use consumer.onTagEvent() which immediately attaches listeners.
 */

// Standard event listeners
fun CommonAttributeGroupFacade.onClick(handler: (MouseEvent) -> Unit) { onClickFunction = handler }
fun CommonAttributeGroupFacade.onMouseEnter(handler: (MouseEvent) -> Unit) { onMouseEnterFunction = handler }
fun CommonAttributeGroupFacade.onMouseLeave(handler: (MouseEvent) -> Unit) { onMouseLeaveFunction = handler }
fun CommonAttributeGroupFacade.onContextMenu(handler: (MouseEvent) -> Unit) { onContextMenuFunction = handler }
fun CommonAttributeGroupFacade.onSubmit(handler: (Event) -> Unit) { onSubmitFunction = handler }
fun CommonAttributeGroupFacade.onChange(handler: (Event) -> Unit) { onChangeFunction = handler }
fun CommonAttributeGroupFacade.onKeyUp(handler: (KeyboardEvent) -> Unit) { onKeyUpFunction = handler }
fun CommonAttributeGroupFacade.onKeyDown(handler: (KeyboardEvent) -> Unit) { onKeyDownFunction = handler }
fun CommonAttributeGroupFacade.onLoad(handler: (Event) -> Unit) { onLoadFunction = handler }
fun CommonAttributeGroupFacade.onError(handler: (Event) -> Unit) { onErrorFunction = handler }
fun CommonAttributeGroupFacade.onInput(handler: (InputEvent) -> Unit) { onInputFunction = handler }
fun CommonAttributeGroupFacade.onScroll(handler: (Event) -> Unit) { onScrollFunction = handler }

// Drag and drop events
fun CommonAttributeGroupFacade.onDragStart(handler: (DragEvent) -> Unit) { onDragStartFunction = handler }
fun CommonAttributeGroupFacade.onDragEnd(handler: (DragEvent) -> Unit) { onDragEndFunction = handler }
fun CommonAttributeGroupFacade.onDragOver(handler: (DragEvent) -> Unit) { onDragOverFunction = handler }
fun CommonAttributeGroupFacade.onDragEnter(handler: (DragEvent) -> Unit) { onDragEnterFunction = handler }
fun CommonAttributeGroupFacade.onDragLeave(handler: (DragEvent) -> Unit) { onDragLeaveFunction = handler }
fun CommonAttributeGroupFacade.onDrop(handler: (DragEvent) -> Unit) { onDropFunction = handler }


/**
 * Executed when the current tag ends (after children are built).
 * The handler runs synchronously during DSL execution, before the element
 * is appended to its parent.
 *
 * @throws IllegalStateException if called outside ElementTrackingConsumer context
 */
fun Tag.onMount(handler: () -> Unit) {
    val tracker = getCurrentTracker()
        ?: error("onMount() requires ElementTrackingConsumer context")
    tracker.queueMountCallback(handler)
}

/**
 * Executed when element is removed from dom.
 * Useful for cleanup (timers, event listeners, etc.)
 *
 * @throws IllegalStateException if called outside ElementTrackingConsumer context
 */
fun Tag.onDestroy(handler: () -> Unit) {
    val tracker = getCurrentTracker()
        ?: error("onDestroy() requires ElementTrackingConsumer context")
    val element = tracker.currentElement() as HTMLElement
    element.elementOnDestroy(handler)
}
