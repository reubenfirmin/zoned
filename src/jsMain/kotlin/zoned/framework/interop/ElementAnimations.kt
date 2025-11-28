package zoned.framework.interop

import web.html.HTMLElement
import web.timers.setTimeout

enum class Direction { UP, DOWN, LEFT, RIGHT }

private fun transition(property: String, durationMs: Int, timing: String = "ease-in-out") =
    "$property ${durationMs}ms $timing"

/**
 * Fade element in from opacity 0 to 1.
 */
fun HTMLElement.fadeIn(durationMs: Int, onComplete: (() -> Unit)? = null) {
    style.opacity = "0"
    style.transition = transition("opacity", durationMs)
    // Force reflow to ensure transition triggers
    offsetHeight
    style.opacity = "1"
    onComplete?.let { cb -> setTimeout({ cb() }, durationMs) }
}

/**
 * Fade element out from current opacity to 0.
 */
fun HTMLElement.fadeOut(durationMs: Int, onComplete: (() -> Unit)? = null) {
    style.transition = transition("opacity", durationMs)
    style.opacity = "0"
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
    style.transition = transition("transform", durationMs)
    style.transform = transform
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
    style.transform = startTransform
    style.transition = transition("transform", durationMs)
    offsetHeight  // Force reflow
    style.transform = "translate(0, 0)"
    onComplete?.let { cb -> setTimeout({ cb() }, durationMs) }
}

/**
 * Collapse element height to 0 (accordion-style).
 */
fun HTMLElement.collapse(durationMs: Int, onComplete: (() -> Unit)? = null) {
    style.overflow = "hidden"
    style.height = "${scrollHeight}px"
    style.transition = transition("height", durationMs)
    offsetHeight  // Force reflow
    style.height = "0px"
    onComplete?.let { cb -> setTimeout({ cb() }, durationMs) }
}

/**
 * Expand element from height 0 to its natural height (accordion-style).
 */
fun HTMLElement.expand(durationMs: Int, onComplete: (() -> Unit)? = null) {
    style.overflow = "hidden"
    val targetHeight = scrollHeight
    style.height = "0px"
    style.transition = transition("height", durationMs)
    offsetHeight  // Force reflow
    style.height = "${targetHeight}px"
    setTimeout({
        style.height = "auto"
        style.overflow = ""
        onComplete?.invoke()
    }, durationMs)
}
