package zoned.framework.ui.enhancements

/**
 * Enhancement initialization is handled by apps in their MainBundle.
 * Apps should call their registry's initialize() method:
 * - On page load
 * - After HTMX content swaps (in setupHTMX callback)
 *
 * Example in app's MainBundle:
 * ```
 * setupHTMX({
 *     ZonedEnhancementRegistry.initialize()
 *     MyAppEnhancementRegistry.initialize()
 * })
 * ZonedEnhancementRegistry.initialize()
 * MyAppEnhancementRegistry.initialize()
 * ```
 */
