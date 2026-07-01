package zoned.framework.ui.enhancements

import kotlinx.html.DIV
import kotlinx.html.div
import web.dom.document
import web.html.HTMLElement
import zoned.framework.dom.Ref
import zoned.framework.dom.ref
import zoned.framework.interop.appendTo

/**
 * Mount a full-screen overlay (modal, lightbox, PDF viewer, etc.) as a direct
 * child of `<body>` so it isn't clipped by an ancestor's `overflow` or trapped
 * in a lower stacking context.
 *
 * [build] populates and styles the overlay root `<div>` and receives that root
 * element, so handlers can reference it (backdrop-click checks, [onGlobalKeyDown]
 * cleanup owner, teardown). Returns the root element; call [HTMLElement.remove]
 * to tear the overlay down.
 *
 * This replaces hand-rolled `document.body.appendTo().div { ... }` (and bare
 * `document.body.appendChild`) in enhancement implementations, keeping the
 * body-level mount in one place.
 *
 * ```kotlin
 * mountOverlay { root ->
 *     val close = { root.remove() }
 *     css { /* fixed backdrop */ }
 *     onClick { e -> if (e.target == root) close() }   // click backdrop to dismiss
 *     onGlobalKeyDown(root) { e -> if (e.key == "Escape") close() }
 *     // ... overlay content ...
 * }
 * ```
 */
fun mountOverlay(build: DIV.(root: HTMLElement) -> Unit): HTMLElement {
    val ref = Ref<HTMLElement>()
    document.body.appendTo().div {
        ref(ref)
        build(ref.element)
    }
    return ref.element
}
