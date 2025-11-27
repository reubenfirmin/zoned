package zoned.framework.ui.enhancements

import kotlinx.serialization.KSerializer

/**
 * Base interface for all UI enhancements.
 *
 * Enhancements are client-side behaviors that can be attached to DOM elements
 * via data attributes. The Gradle plugin scans for implementations and generates:
 * - Type-safe DSL functions for server-side usage (jvmMain)
 * - Auto-discovery registry for client-side initialization (jsMain)
 *
 * @param TConfig The configuration type for this enhancement, must implement [EnhancementConfig]
 */
interface Enhancement<TConfig : EnhancementConfig> {
    /**
     * Unique identifier for this enhancement (e.g., "sortable", "ace-editor")
     * This is used in the data-enhancement attribute
     */
    val name: String

    /**
     * Serializer for the configuration type.
     * Used to serialize configs to JSON on the server and deserialize on the client.
     */
    val configSerializer: KSerializer<TConfig>
}
