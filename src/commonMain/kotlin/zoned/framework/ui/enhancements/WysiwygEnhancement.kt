package zoned.framework.ui.enhancements

import kotlinx.serialization.Serializable

/**
 * Enhancement for WYSIWYG rich text editing.
 * Uses contenteditable with a basic formatting toolbar on the client-side.
 *
 * The enhancement wraps the initial content to be edited.
 *
 * Usage:
 * ```kotlin
 * // Empty editor for new content
 * wysiwyg({
 *     inputName = "content"
 *     placeholder = "Add a note..."
 *     toolbar = "standard"
 * }) {
 *     // Empty - new content
 * }
 *
 * // Editor with existing content
 * wysiwyg({
 *     inputName = "content"
 * }) {
 *     unsafe { +existingHtmlContent }
 * }
 * ```
 */
@ClientEnhancement
object WysiwygEnhancement : Enhancement<WysiwygConfig> {
    override val name = "wysiwyg"
    override val configSerializer = WysiwygConfig.serializer()
}

/**
 * Configuration for @mention autocomplete in WYSIWYG
 */
@Serializable
data class MentionConfig(
    /**
     * Endpoint to search for users (e.g., "/mentions/search")
     * Should return JSON array: [{ key: "John Doe", value: "user-id-123" }, ...]
     */
    val searchEndpoint: String,

    /**
     * Trigger character (default: @)
     */
    val trigger: String = "@",

    /**
     * CSS class for the mention span (default: "mention")
     */
    val mentionClass: String = "mention"
)

/**
 * Configuration for the WYSIWYG enhancement.
 * Note: inputName and placeholder come from the wrapped textarea element.
 */
@Serializable
data class WysiwygConfig(
    /**
     * Minimum height of the editor in pixels
     */
    var minHeight: Int = 120,

    /**
     * Toolbar options: "minimal", "standard", or "full"
     */
    var toolbar: String = "minimal",

    /**
     * Optional @mention configuration. When set, enables @mention autocomplete.
     */
    var mentionConfig: MentionConfig? = null
) : EnhancementConfig

