package zoned.framework.ui.enhancements

import kotlinx.serialization.Serializable

/**
 * Enhancement for making lists sortable via drag-and-drop.
 * Uses the SortableJS library on the client-side.
 */
@ClientEnhancement
object SortableEnhancement : Enhancement<SortableConfig> {
    override val name = "sortable"
    override val configSerializer = SortableConfig.serializer()
}

/**
 * Configuration for the Sortable enhancement
 */
@Serializable
data class SortableConfig(
    /**
     * ID to set on the sortable container element.
     * Useful for identifying the drop target in onEnd callbacks.
     */
    var containerId: String? = null,

    /**
     * Drag-and-drop group name. Elements with the same group can be dragged between each other.
     */
    var group: String = "default",

    /**
     * Animation speed in milliseconds
     */
    var animation: Int = 150,

    /**
     * CSS selector for draggable items within the container.
     */
    var draggable: String? = null,

    /**
     * Allow sorting within this container
     */
    var sort: Boolean = true,

    /**
     * Can pull items out of this container
     */
    var pull: Boolean = true,

    /**
     * Can drop items into this container
     */
    var put: Boolean = true,

    /**
     * CSS class applied to the ghost element during drag
     */
    var ghostClass: String = "sortable-ghost",

    /**
     * CSS class applied to the chosen (dragged) element
     */
    var chosenClass: String = "sortable-chosen",

    /**
     * CSS class applied during drag operation
     */
    var dragClass: String = "sortable-drag",

    /**
     * URL to POST when drop completes (typesafe route serialized by server).
     * Server receives: itemId, fromContainerId, toContainerId, oldIndex, newIndex
     */
    var onDropUrl: String? = null,

    /**
     * HTMX target selector for response
     */
    var htmxTarget: String? = null,

    /**
     * HTMX swap strategy
     */
    var htmxSwap: String? = null,

    /**
     * CSS class applied to drop targets when an item is dragged over them
     */
    var dropTargetClass: String = "sortable-drag-over",

    /**
     * CSS selector for elements that should NOT trigger drag (e.g., links, buttons).
     * Clicks on these elements will work normally instead of starting a drag.
     * Default: "a, button, input, select, textarea" to allow interaction with common elements.
     */
    var filter: String = "a, button, input, select, textarea"
) : EnhancementConfig
