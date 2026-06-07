package zoned.framework.libs

import web.dom.Element

/**
 * Tribute.js - @mention autocomplete library
 * https://github.com/zurb/tribute
 *
 * The npm `tributejs` package exports the class as its default/CommonJS export,
 * so the module binding maps straight onto the constructor (same pattern as
 * [dragula]).
 */
@JsModule("tributejs")
@JsNonModule
external class Tribute(options: TributeOptions) {
    /**
     * Attach Tribute to an element (input, textarea, or contenteditable)
     */
    fun attach(element: Element)

    /**
     * Detach Tribute from an element
     */
    fun detach(element: Element)

    /**
     * Check if the menu is currently active
     */
    val isActive: Boolean
}

/**
 * Configuration options for Tribute
 */
external interface TributeOptions {
    /**
     * Trigger character to start autocomplete (default: @)
     */
    var trigger: String?

    /**
     * Array of values to match against, or async function to fetch values
     */
    var values: dynamic  // Array<TributeItem> | ((text: String, callback: (Array<TributeItem>) -> Unit) -> Unit)

    /**
     * Key to search against in value objects (default: "key")
     */
    var lookup: String?

    /**
     * Key to use when inserting the selected item (default: "value")
     */
    var fillAttr: String?

    /**
     * Template for the inserted content
     */
    var selectTemplate: ((item: TributeSelection) -> String)?

    /**
     * Template for menu items
     */
    var menuItemTemplate: ((item: TributeSelection) -> String)?

    /**
     * Template shown when no matches found
     */
    var noMatchTemplate: (() -> String)?

    /**
     * Allow spaces in lookup (default: false)
     */
    var allowSpaces: Boolean?

    /**
     * Autocomplete mode (default: false)
     */
    var autocompleteMode: Boolean?

    /**
     * Text to append after selection (default: " ")
     */
    var replaceTextSuffix: String?

    /**
     * Position menu relative to trigger (default: true)
     */
    var positionMenu: Boolean?

    /**
     * Container element for the menu (default: body)
     */
    var menuContainer: Element?

    /**
     * Minimum number of typed characters before the menu is shown (default: 0)
     */
    var menuShowMinLength: Int?

    /**
     * Require leading space before trigger (default: true)
     */
    var requireLeadingSpace: Boolean?

    /**
     * CSS class for the menu container
     */
    var containerClass: String?

    /**
     * CSS class for selected item
     */
    var selectClass: String?

    /**
     * CSS class for the item wrapper
     */
    var itemClass: String?

    /**
     * Search options for filtering
     */
    var searchOpts: TributeSearchOptions?
}

/**
 * Search options for Tribute
 */
external interface TributeSearchOptions {
    var pre: String?
    var post: String?
    var skip: Boolean?
}

/**
 * Item in the Tribute values array
 */
external interface TributeItem {
    var key: String
    var value: String
}

/**
 * Selection object passed to templates
 */
external interface TributeSelection {
    var original: TributeItem
    var string: String
}
