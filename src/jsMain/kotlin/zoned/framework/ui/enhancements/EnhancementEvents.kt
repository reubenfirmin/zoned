package zoned.framework.ui.enhancements

import web.dom.Document
import web.dom.document
import web.events.Event
import web.events.EventType
import web.events.addEventListener
import web.events.removeEventListener
import web.html.HTMLElement
import web.keyboard.KeyboardEvent
import web.mouse.MouseEvent
import web.input.InputEvent
import zoned.framework.interop.onDestroy

/**
 * Event listener bindings for HTMLElement in enhancement implementations.
 *
 * PREFERRED APPROACH: Use kotlinx.html DSL with appendTo() and the event extensions
 * from zoned.framework.dom.Events (onClick, onKeyDown, etc. on CommonAttributeGroupFacade).
 * This gives you type-safe DOM construction with properly typed event handlers.
 *
 * Example (preferred):
 * ```kotlin
 * element.appendTo().div {
 *     id = "my-element"
 *     onClick { e -> handleClick(e) }
 *     onKeyDown { e -> handleKey(e) }
 * }
 * ```
 *
 * WHEN TO USE THESE HTMLElement EXTENSIONS:
 * - Attaching handlers to existing DOM elements (not created via DSL)
 * - Enhancement implementations that receive an Element from server-rendered HTML
 * - Document-level event listeners (use web.dom.document.onClick, etc.)
 *
 * Example (when necessary):
 * ```kotlin
 * fun makeMyEnhancement(element: Element, config: MyConfig) {
 *     val htmlElement = element as HTMLElement
 *     htmlElement.onClick { e -> handleClick(e) }
 * }
 * ```
 */

private inline fun <reified E : Event> HTMLElement.addTypedEventListener(
    type: String,
    noinline handler: (E) -> Unit
) = addEventListener(EventType<E>(type), handler)

// Mouse events
fun HTMLElement.onClick(handler: (MouseEvent) -> Unit) =
    addTypedEventListener("click", handler)

fun HTMLElement.onMouseEnter(handler: (MouseEvent) -> Unit) =
    addTypedEventListener("mouseenter", handler)

fun HTMLElement.onMouseLeave(handler: (MouseEvent) -> Unit) =
    addTypedEventListener("mouseleave", handler)

fun HTMLElement.onMouseOver(handler: (MouseEvent) -> Unit) =
    addTypedEventListener("mouseover", handler)

fun HTMLElement.onMouseOut(handler: (MouseEvent) -> Unit) =
    addTypedEventListener("mouseout", handler)

fun HTMLElement.onContextMenu(handler: (MouseEvent) -> Unit) =
    addTypedEventListener("contextmenu", handler)

// Keyboard events
fun HTMLElement.onKeyDown(handler: (KeyboardEvent) -> Unit) =
    addTypedEventListener("keydown", handler)

fun HTMLElement.onKeyUp(handler: (KeyboardEvent) -> Unit) =
    addTypedEventListener("keyup", handler)

// Input events
fun HTMLElement.onInput(handler: (InputEvent) -> Unit) =
    addTypedEventListener("input", handler)

// Form element events
fun web.html.HTMLInputElement.onChange(handler: (web.events.Event) -> Unit) =
    addEventListener(EventType<Event>("change"), handler)

fun web.html.HTMLFormElement.onReset(handler: (web.events.Event) -> Unit) =
    addEventListener(EventType<Event>("reset"), handler)

// Document-level events
fun Document.onClick(handler: (MouseEvent) -> Unit) =
    addEventListener(EventType<MouseEvent>("click"), handler)

fun Document.onKeyDown(handler: (KeyboardEvent) -> Unit) =
    addEventListener(EventType<KeyboardEvent>("keydown"), handler)

/**
 * Register a document-level keydown handler whose lifetime is tied to [owner]:
 * the listener is added immediately and automatically removed when [owner]
 * leaves the DOM (via [onDestroy], which also fires on ancestor teardown).
 *
 * Use for enhancement overlays/modals that need global keys (Escape, arrows)
 * while open, instead of pairing a raw `document.addEventListener` with a
 * hand-managed `removeEventListener` (which leaks if the cleanup is missed).
 *
 * ```kotlin
 * val overlay = mountOverlay { root ->
 *     onGlobalKeyDown(root) { e -> if (e.key == "Escape") root.remove() }
 * }
 * ```
 */
fun onGlobalKeyDown(owner: HTMLElement, handler: (KeyboardEvent) -> Unit) {
    document.addEventListener(EventType<KeyboardEvent>("keydown"), handler)
    owner.onDestroy {
        document.removeEventListener(EventType<KeyboardEvent>("keydown"), handler)
    }
}
