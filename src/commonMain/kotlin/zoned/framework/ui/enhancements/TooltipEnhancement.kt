package zoned.framework.ui.enhancements

import kotlinx.serialization.Serializable

/**
 * Enhancement for showing inline tooltips on hover.
 * Unlike browser title attributes, these appear instantly with custom styling.
 *
 * Usage:
 * ```kotlin
 * span {
 *     tooltip { text = "Latitude" }
 *     +"9.876543"
 * }
 * ```
 */
@ClientEnhancement
object TooltipEnhancement : Enhancement<TooltipConfig> {
    override val name = "tooltip"
    override val configSerializer = TooltipConfig.serializer()
}

/**
 * Configuration for the Tooltip enhancement
 */
@Serializable
data class TooltipConfig(
    /**
     * Text to display in the tooltip
     */
    var text: String = "",

    /**
     * Position of tooltip relative to element: "top", "bottom", "left", "right"
     */
    var position: String = "top"
) : EnhancementConfig
