package zoned.framework.ui.layouts

import java.util.concurrent.ConcurrentHashMap

/**
 * Type-safe HTMX target with optional runtime verification.
 * Implements HtmxSelector so it can be passed directly to HTMX functions.
 *
 * Example usage:
 * ```
 * companion object {
 *     val mainContent = HTMXTarget("content")
 * }
 *
 * div {
 *     zone(mainContent)  // Registers the target
 *     // Content here
 * }
 *
 * button {
 *     onClick(withAction(api::update, target = mainContent))  // Pass directly
 * }
 * ```
 *
 * @param id The HTML element ID (without # prefix)
 * @param verify Enable runtime verification that this target is rendered before use
 */
open class HTMXTarget(
    val id: String,
    private val verify: Boolean = false
) : HtmxSelector {
    override val cssSelector: String = "#$id"
    init {
        if (verify) {
            HTMXTargetRegistry.register(this)
        }
    }

    /**
     * Mark this target as rendered in the current request context
     */
    fun markRendered() {
        if (verify) {
            HTMXTargetRegistry.markRendered(this)
        }
    }

    /**
     * Verify this target has been rendered (throws if not)
     */
    fun verifyRendered() {
        if (verify && !HTMXTargetRegistry.isRendered(this)) {
            throw IllegalStateException(
                "HTMX target '${id}' (selector: ${cssSelector}) was referenced but not rendered in the DOM. " +
                "Ensure the element with id='${id}' exists before targeting it."
            )
        }
    }
}

/**
 * Thread-local registry for tracking rendered HTMX targets
 * Useful for catching missing target elements at development time
 */
object HTMXTargetRegistry {
    private val renderedTargets = ThreadLocal.withInitial { mutableSetOf<String>() }
    private val allTargets = ConcurrentHashMap<String, HTMXTarget>()

    fun register(target: HTMXTarget) {
        allTargets[target.id] = target
    }

    fun markRendered(target: HTMXTarget) {
        renderedTargets.get().add(target.id)
    }

    fun isRendered(target: HTMXTarget): Boolean {
        return renderedTargets.get().contains(target.id)
    }

    /**
     * Clear rendered targets for current thread (call at end of request)
     */
    fun clearRendered() {
        renderedTargets.get().clear()
    }

    /**
     * Get all registered targets (for debugging)
     */
    fun getAllTargets(): Map<String, HTMXTarget> = allTargets.toMap()
}
