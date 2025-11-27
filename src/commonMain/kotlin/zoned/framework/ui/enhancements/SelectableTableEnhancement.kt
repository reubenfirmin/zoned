package zoned.framework.ui.enhancements

import kotlinx.serialization.Serializable

/**
 * Enhancement for tables with selectable rows and bulk drag-drop operations.
 * Adds checkboxes to rows, supports shift-click range selection, and allows
 * dragging multiple selected items to other containers.
 */
@ClientEnhancement
object SelectableTableEnhancement : Enhancement<SelectableTableConfig> {
    override val name = "selectable-table"
    override val configSerializer = SelectableTableConfig.serializer()
}

/**
 * Configuration for the SelectableTable enhancement
 */
@Serializable
data class SelectableTableConfig(
    /**
     * CSS selector for the rows (e.g., "tr" for table rows)
     */
    var rowSelector: String = "tr",

    /**
     * CSS selector for the checkbox within each row
     */
    var checkboxSelector: String = ".row-select",

    /**
     * CSS selector for the "select all" checkbox in the header
     */
    var selectAllSelector: String = ".select-all",

    /**
     * Data attribute name that holds the item ID on each row
     */
    var itemIdAttribute: String = "data-item-id",

    /**
     * CSS class applied to selected rows
     */
    var selectedClass: String = "bg-blue-900/30",

    /**
     * Enable shift-click range selection
     */
    var shiftSelect: Boolean = true,

    /**
     * Drag-and-drop group name (same as SortableConfig)
     */
    var dragGroup: String = "default",

    /**
     * Can pull items out of this container
     */
    var dragPull: Boolean = true,

    /**
     * Can drop items into this container
     */
    var dragPut: Boolean = false,

    /**
     * Animation speed in milliseconds
     */
    var dragAnimation: Int = 150,

    /**
     * CSS class applied to ghost element during drag
     */
    var ghostClass: String = "sortable-ghost",

    /**
     * CSS class applied to chosen element during drag
     */
    var chosenClass: String = "sortable-chosen",

    /**
     * CSS selector for the drag handle within each row (e.g., ".drag-handle").
     * If specified, only this element can initiate drag. If null, the whole row is draggable.
     */
    var dragHandle: String? = null,

    /**
     * URL to POST when drop completes. Receives: itemIds (comma-separated), targetContainerId
     */
    var onDropUrl: String? = null,

    /**
     * HTMX target selector for response
     */
    var htmxTarget: String? = null,

    /**
     * CSS selector for the floating action bar container
     */
    var actionBarSelector: String? = null,

    /**
     * URL for bulk archive action
     */
    var archiveUrl: String? = null,

    /**
     * URL to fetch move-to menu options
     */
    var moveMenuUrl: String? = null
) : EnhancementConfig
