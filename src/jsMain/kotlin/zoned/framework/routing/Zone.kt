package zoned.framework.routing

import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.TagConsumer
import kotlinx.html.id
import web.dom.ElementId
import web.dom.document
import web.html.HTMLElement
import zoned.framework.dom.ElementTrackingConsumer
import zoned.framework.interop.clear
import zoned.framework.interop.rebuildInPlace

/**
 * A typed handle to a named region of the page — the swap target for htmx-style partial updates.
 *
 * Declare zones once (e.g. in an app-level registry object), mark the element with
 * [Companion.zone] when building it, and update it with [swap]. Routes can render straight into a
 * zone by declaring it as their target (see `Routes.route(target = ...)` / `Routes.fragment`).
 */
data class Zone(val elementId: String) {

    /** The zone's live element, or null if it isn't currently in the DOM. */
    fun resolve(): HTMLElement? = document.getElementById(ElementId(elementId))

    /**
     * Swap this zone's contents: clear the element and build [block] into it. Everything outside
     * the zone is untouched. onMount handlers inside [block] run once the new content is attached.
     *
     * @throws IllegalStateException if the zone is not in the DOM
     */
    fun swap(block: TagConsumer<HTMLElement>.() -> Unit) {
        val el = resolve() ?: error("Zone '$elementId' is not in the DOM")
        el.clear()
        ElementTrackingConsumer(el).block()
    }

    /**
     * Replace the zone's ELEMENT (not just its children) with freshly built content, preserving
     * its slot among siblings — for zones whose own attributes/classes change per render. Use
     * [swap] when the element is a stable container and only its contents change. Returns the new
     * element, or null if the zone (or its parent) is absent.
     */
    fun rebuild(block: TagConsumer<HTMLElement>.() -> HTMLElement): HTMLElement? =
        resolve()?.rebuildInPlace(block)

    companion object {

        @Deprecated("Use zone.resolve() — same lookup, typed HTMLElement? return", ReplaceWith("zone.resolve()"))
        fun id(zone: Zone) = document.getElementById(ElementId(zone.elementId))

        /** Mark the element being built as [zone]'s element. */
        fun CommonAttributeGroupFacade.zone(zone: Zone) {
            this.id = zone.elementId
        }
    }
}
