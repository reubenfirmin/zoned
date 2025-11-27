package zoned.framework.ui.layouts

/**
 * Type-safe HTMX target selector. Prevents raw string usage in HTMX targeting.
 *
 * Common usage:
 * - `target = myTarget` (HTMXTarget directly - most common)
 * - `target = HtmxThis` (target the triggering element)
 * - `target = HtmxClosest("tr")` (find closest ancestor)
 * - `target = HtmxFind(".content")` (find within triggering element)
 */
sealed interface HtmxSelector {
    val cssSelector: String
}

/**
 * Target the element that triggered the HTMX request.
 * Maps to hx-target="this"
 */
object HtmxThis : HtmxSelector {
    override val cssSelector = "this"
}

/**
 * Find the closest ancestor matching the CSS selector.
 * Maps to hx-target="closest {css}"
 *
 * Example: HtmxClosest("tr") finds the closest table row
 */
data class HtmxClosest(private val css: String) : HtmxSelector {
    override val cssSelector = "closest $css"
}

/**
 * Find an element within the triggering element matching the CSS selector.
 * Maps to hx-target="find {css}"
 */
data class HtmxFind(private val css: String) : HtmxSelector {
    override val cssSelector = "find $css"
}

/**
 * Find the next sibling element, optionally matching a CSS selector.
 * Maps to hx-target="next" or hx-target="next {css}"
 */
data class HtmxNext(private val css: String = "") : HtmxSelector {
    override val cssSelector = if (css.isEmpty()) "next" else "next $css"
}

/**
 * Find the previous sibling element, optionally matching a CSS selector.
 * Maps to hx-target="previous" or hx-target="previous {css}"
 */
data class HtmxPrevious(private val css: String = "") : HtmxSelector {
    override val cssSelector = if (css.isEmpty()) "previous" else "previous $css"
}
