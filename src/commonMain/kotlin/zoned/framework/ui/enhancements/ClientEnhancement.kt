package zoned.framework.ui.enhancements

/**
 * Marker annotation for enhancement discovery by the Gradle plugin.
 *
 * Annotate your Enhancement object with this annotation to have it automatically
 * discovered during build and included in the generated DSL and registry.
 *
 * @param clientBuildsContent When true, the client builds all UI from config alone.
 *        The generated DSL will be config-only: `fileUpload { inputName = "..." }`
 *        When false (default), server provides content that the client wraps:
 *        `tooltip({ text = "..." }) { span { +"Hover me" } }`
 *
 * Example (wrapper enhancement - server provides content):
 * ```
 * @ClientEnhancement
 * object TooltipEnhancement : Enhancement<TooltipConfig> { ... }
 *
 * // DSL usage:
 * tooltip({ text = "Help" }) {
 *     span { +"Hover for help" }
 * }
 * ```
 *
 * Example (config-only enhancement - client builds UI):
 * ```
 * @ClientEnhancement(clientBuildsContent = true)
 * object FileUploadEnhancement : Enhancement<FileUploadConfig> { ... }
 *
 * // DSL usage:
 * fileUpload { inputName = "document" }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ClientEnhancement(
    val clientBuildsContent: Boolean = false
)
