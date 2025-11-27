package zoned.framework.interop

import web.html.HTMLElement
import web.timers.setTimeout

enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * Fade element in from opacity 0 to 1.
 */
fun HTMLElement.fadeIn(durationMs: Int, onComplete: (() -> Unit)? = null) {
    asDynamic().style.opacity = 0
    asDynamic().style.transition = "opacity ${durationMs}ms ease-in-out"
    // Force reflow to ensure transition triggers
    offsetHeight
    asDynamic().style.opacity = 1
    onComplete?.let { cb -> setTimeout({ cb() }, durationMs) }
}

/**
 * Fade element out from current opacity to 0.
 */
fun HTMLElement.fadeOut(durationMs: Int, onComplete: (() -> Unit)? = null) {
    asDynamic().style.transition = "opacity ${durationMs}ms ease-in-out"
    asDynamic().style.opacity = 0
    onComplete?.let { cb -> setTimeout({ cb() }, durationMs) }
}

/**
 * Fade element out, then remove it from the DOM.
 */
fun HTMLElement.fadeOutAndRemove(durationMs: Int) {
    fadeOut(durationMs) { remove() }
}

/**
 * Slide element out in the specified direction.
 */
fun HTMLElement.slideOut(direction: Direction, durationMs: Int, onComplete: (() -> Unit)? = null) {
    val transform = when (direction) {
        Direction.UP -> "translateY(-100%)"
        Direction.DOWN -> "translateY(100%)"
        Direction.LEFT -> "translateX(-100%)"
        Direction.RIGHT -> "translateX(100%)"
    }
    asDynamic().style.transition = "transform ${durationMs}ms ease-in-out"
    asDynamic().style.transform = transform
    onComplete?.let { cb -> setTimeout({ cb() }, durationMs) }
}

/**
 * Slide element in from the specified direction to its natural position.
 */
fun HTMLElement.slideIn(direction: Direction, durationMs: Int, onComplete: (() -> Unit)? = null) {
    val startTransform = when (direction) {
        Direction.UP -> "translateY(-100%)"
        Direction.DOWN -> "translateY(100%)"
        Direction.LEFT -> "translateX(-100%)"
        Direction.RIGHT -> "translateX(100%)"
    }
    asDynamic().style.transform = startTransform
    asDynamic().style.transition = "transform ${durationMs}ms ease-in-out"
    offsetHeight  // Force reflow
    asDynamic().style.transform = "translate(0, 0)"
    onComplete?.let { cb -> setTimeout({ cb() }, durationMs) }
}

/**
 * Collapse element height to 0 (accordion-style).
 */
fun HTMLElement.collapse(durationMs: Int, onComplete: (() -> Unit)? = null) {
    asDynamic().style.overflow = "hidden"
    asDynamic().style.height = "${scrollHeight}px"
    asDynamic().style.transition = "height ${durationMs}ms ease-in-out"
    offsetHeight  // Force reflow
    asDynamic().style.height = "0px"
    onComplete?.let { cb -> setTimeout({ cb() }, durationMs) }
}

/**
 * Expand element from height 0 to its natural height (accordion-style).
 */
fun HTMLElement.expand(durationMs: Int, onComplete: (() -> Unit)? = null) {
    asDynamic().style.overflow = "hidden"
    val targetHeight = scrollHeight
    asDynamic().style.height = "0px"
    asDynamic().style.transition = "height ${durationMs}ms ease-in-out"
    offsetHeight  // Force reflow
    asDynamic().style.height = "${targetHeight}px"
    setTimeout({
        asDynamic().style.height = "auto"
        asDynamic().style.overflow = ""
        onComplete?.invoke()
    }, durationMs)
}
