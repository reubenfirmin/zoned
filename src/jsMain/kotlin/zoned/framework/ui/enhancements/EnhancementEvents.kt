package zoned.framework.ui.enhancements

import web.dom.Document
import web.events.EventType
import web.events.addEventListener
import web.html.HTMLElement
import web.uievents.KeyboardEvent
import web.uievents.MouseEvent

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

// Mouse events
fun HTMLElement.onClick(handler: (MouseEvent) -> Unit) =
    addEventListener(EventType("click"), handler)

fun HTMLElement.onMouseEnter(handler: (MouseEvent) -> Unit) =
    addEventListener(EventType("mouseenter"), handler)

fun HTMLElement.onMouseLeave(handler: (MouseEvent) -> Unit) =
    addEventListener(EventType("mouseleave"), handler)

fun HTMLElement.onMouseOver(handler: (MouseEvent) -> Unit) =
    addEventListener(EventType("mouseover"), handler)

fun HTMLElement.onMouseOut(handler: (MouseEvent) -> Unit) =
    addEventListener(EventType("mouseout"), handler)

fun HTMLElement.onContextMenu(handler: (MouseEvent) -> Unit) =
    addEventListener(EventType("contextmenu"), handler)

// Keyboard events
fun HTMLElement.onKeyDown(handler: (KeyboardEvent) -> Unit) =
    addEventListener(EventType("keydown"), handler)

fun HTMLElement.onKeyUp(handler: (KeyboardEvent) -> Unit) =
    addEventListener(EventType("keyup"), handler)

// Input events
fun HTMLElement.onInput(handler: (web.uievents.InputEvent) -> Unit) =
    addEventListener(EventType("input"), handler)

// Form element events
fun web.html.HTMLInputElement.onChange(handler: (web.events.Event) -> Unit) =
    addEventListener(EventType("change"), handler)

fun web.html.HTMLFormElement.onReset(handler: (web.events.Event) -> Unit) =
    addEventListener(EventType("reset"), handler)

// Document-level events
fun Document.onClick(handler: (MouseEvent) -> Unit) =
    addEventListener(EventType("click"), handler)

fun Document.onKeyDown(handler: (KeyboardEvent) -> Unit) =
    addEventListener(EventType("keydown"), handler)
