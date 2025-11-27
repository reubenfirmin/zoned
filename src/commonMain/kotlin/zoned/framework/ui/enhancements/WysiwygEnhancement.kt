package zoned.framework.ui.enhancements

import kotlinx.serialization.Serializable

/**
 * Enhancement for WYSIWYG rich text editing.
 * Uses contenteditable with a basic formatting toolbar on the client-side.
 *
 * Usage:
 * ```kotlin
 * div {
 *     wysiwyg {
 *         inputName = "content"
 *         toolbar = "standard"
 *     }
 * }
 * ```
 */
@ClientEnhancement
object WysiwygEnhancement : Enhancement<WysiwygConfig> {
    override val name = "wysiwyg"
    override val configSerializer = WysiwygConfig.serializer()
}

/**
 * Configuration for the WYSIWYG enhancement
 */
@Serializable
data class WysiwygConfig(
    /**
     * Name attribute for the hidden input that stores the HTML content
     */
    var inputName: String = "content",

    /**
     * Initial HTML content
     */
    var initialContent: String = "",

    /**
     * Placeholder text when editor is empty
     */
    var placeholder: String = "",

    /**
     * Minimum height of the editor in pixels
     */
    var minHeight: Int = 120,

    /**
     * Theme: "snow" (toolbar) or "bubble" (tooltip)
     */
    var theme: String = "snow",

    /**
     * Toolbar options: "minimal", "standard", or "full"
     */
    var toolbar: String = "minimal"
) : EnhancementConfig

