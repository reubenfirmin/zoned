package zoned.framework.ui.enhancements

import kotlinx.serialization.Serializable

/**
 * Enhancement for showing server-rendered context menus on right-click.
 * The menu content is fetched via HTMX GET, allowing the server to render
 * menu items with HTMX attributes for actions.
 */
@ClientEnhancement
object ContextMenuEnhancement : Enhancement<ContextMenuConfig> {
    override val name = "context-menu"
    override val configSerializer = ContextMenuConfig.serializer()
}

/**
 * Configuration for the ContextMenu enhancement
 */
@Serializable
data class ContextMenuConfig(
    /**
     * URL to GET menu content (typesafe route serialized by server)
     */
    var menuUrl: String = "",

    /**
     * CSS selector for menu container element
     */
    var menuTarget: String = "",

    /**
     * Data attributes to include in request (e.g., "propertyId" reads data-property-id)
     */
    var dataAttributes: List<String> = emptyList()
) : EnhancementConfig
