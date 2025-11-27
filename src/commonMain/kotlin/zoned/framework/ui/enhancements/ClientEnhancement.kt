package zoned.framework.ui.enhancements

/**
 * Marker annotation for enhancement discovery by the Gradle plugin.
 *
 * Annotate your Enhancement object with this annotation to have it automatically
 * discovered during build and included in the generated DSL and registry.
 *
 * Example:
 * ```
 * @ClientEnhancement
 * object SortableEnhancement : Enhancement<SortableConfig> {
 *     override val name = "sortable"
 *     override val configSerializer = SortableConfig.serializer()
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ClientEnhancement
