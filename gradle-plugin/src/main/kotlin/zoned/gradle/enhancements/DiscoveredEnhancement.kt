package zoned.gradle.enhancements

/**
 * Represents an enhancement discovered during build-time scanning
 */
data class DiscoveredEnhancement(
    /**
     * The enhancement name (e.g., "sortable")
     */
    val name: String,

    /**
     * The Kotlin object name (e.g., "SortableEnhancement")
     */
    val objectName: String,

    /**
     * The config class name (e.g., "SortableConfig")
     */
    val configClass: String,

    /**
     * The package name (e.g., "zoned.framework.ui.enhancements")
     */
    val packageName: String,

    /**
     * Whether this enhancement uses the expect/actual pattern (initFoo) vs legacy (makeFoo).
     * True if "expect fun initFooEnhancement" was found in the definition.
     */
    val usesExpectActual: Boolean = false,

    /**
     * Whether the client builds all UI from config alone (no server content).
     * True if @ClientEnhancement(clientBuildsContent = true) was specified.
     * When true, the generated DSL is config-only: `fileUpload { inputName = "..." }`
     * When false, DSL includes content lambda: `tooltip({ text = "..." }) { ... }`
     */
    val clientBuildsContent: Boolean = false
) {
    /** Expected implementation function name for legacy pattern */
    val legacyImplName: String get() = "make$objectName"

    /** Expected implementation function name for expect/actual pattern */
    val expectActualImplName: String get() = "init$objectName"

    /** The implementation function name to use based on pattern */
    val implFunctionName: String get() = if (usesExpectActual) expectActualImplName else legacyImplName
}
