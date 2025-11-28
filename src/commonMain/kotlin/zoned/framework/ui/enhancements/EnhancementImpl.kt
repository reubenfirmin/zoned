package zoned.framework.ui.enhancements

/**
 * Marks a function as an enhancement implementation.
 *
 * This annotation is used by the Gradle plugin to discover which enhancements
 * have client-side implementations. It replaces fragile regex-based scanning.
 *
 * Usage:
 * ```kotlin
 * @EnhancementImpl(TooltipEnhancement::class)
 * fun TagConsumer<HTMLElement>.initTooltipEnhancement(config: TooltipConfig, children: List<Node>) {
 *     // implementation
 * }
 * ```
 *
 * The annotation value should reference the enhancement object (e.g., TooltipEnhancement).
 * The scanner will use this to match implementations to their enhancement definitions.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class EnhancementImpl(
    /**
     * The enhancement object this function implements.
     * Use KClass reference like TooltipEnhancement::class
     */
    val enhancement: kotlin.reflect.KClass<*>
)
