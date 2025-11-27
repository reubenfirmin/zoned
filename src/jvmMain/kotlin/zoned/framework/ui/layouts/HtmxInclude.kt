package zoned.framework.ui.layouts

/**
 * Type-safe HTMX include selector. Prevents raw string usage in hx-include.
 *
 * Common usage:
 * - `include = HtmxInclude.Zone(myTarget)` (include inputs from a zone)
 * - `include = HtmxInclude.ByClass("sidebar-state")` (include by CSS class)
 * - `include = HtmxInclude.ClosestForm` (include the closest form)
 * - `include = HtmxInclude.This` (include the triggering element)
 */
sealed interface HtmxInclude {
    val cssSelector: String

    /**
     * Include inputs from a specific zone/target.
     */
    class Zone(private val target: HTMXTarget) : HtmxInclude {
        override val cssSelector = target.cssSelector
    }

    /**
     * Include elements matching a CSS class.
     * Example: HtmxInclude.ByClass("sidebar-state") -> ".sidebar-state"
     */
    class ByClass(private val className: String) : HtmxInclude {
        override val cssSelector = ".$className"
    }

    /**
     * Include elements matching an ID.
     * Example: HtmxInclude.ById("my-input") -> "#my-input"
     */
    class ById(private val id: String) : HtmxInclude {
        override val cssSelector = "#$id"
    }

    /**
     * Include the closest form element.
     */
    object ClosestForm : HtmxInclude {
        override val cssSelector = "closest form"
    }

    /**
     * Include the triggering element itself.
     */
    object This : HtmxInclude {
        override val cssSelector = "this"
    }
}
