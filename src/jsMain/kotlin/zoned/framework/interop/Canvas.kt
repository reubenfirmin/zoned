package zoned.framework.interop

import web.canvas.CanvasRenderingContext2D
import web.html.HTMLCanvasElement

/**
 * Get a 2D rendering context from a canvas element.
 *
 * kotlin-browser 2025.x requires a RenderingContextId<T,O> for getContext(),
 * but doesn't provide pre-built IDs for standard contexts. This extension
 * provides type-safe access to CanvasRenderingContext2D.
 */
fun HTMLCanvasElement.getContext2D(): CanvasRenderingContext2D? {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return asDynamic().getContext("2d") as? CanvasRenderingContext2D
}
