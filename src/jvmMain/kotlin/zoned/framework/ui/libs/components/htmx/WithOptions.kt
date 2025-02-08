package zoned.framework.ui.libs.components.htmx

/**
 * Options that may or may not be used by components in this package. (Ensure they are actually wired up!)
 */
data class WithOptions(val attrs: Map<String, String> = mapOf(),
                        val classes: String = "",
                        val params: Map<String, String> = mapOf(),
                        val id: String? = null)