package zoned.gradle.enhancements

import org.gradle.api.logging.Logger
import java.io.File

/**
 * Scans jsMain source for enhancement implementation functions.
 * Looks for both patterns:
 * - Legacy: fun makeFooEnhancement(...)
 * - New: actual fun initFooEnhancement(...)
 */
class ImplScanner(private val logger: Logger) {

    // Matches: fun makeFooEnhancement( or actual fun initFooEnhancement(
    private val functionRegex = """(?:actual\s+)?fun\s+((?:make|init)\w+Enhancement)\s*\(""".toRegex()

    /**
     * Scan jsMain directory for enhancement implementation functions.
     * @return Set of function names found (e.g., "makeTooltipEnhancement", "initTooltipEnhancement")
     */
    fun scanForImplementations(jsMainDir: File): Set<String> {
        if (!jsMainDir.exists()) {
            logger.info("ImplScanner: jsMain directory not found at ${jsMainDir.absolutePath}")
            return emptySet()
        }

        val implementations = mutableSetOf<String>()

        jsMainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val content = file.readText()
                functionRegex.findAll(content).forEach { match ->
                    val funcName = match.groupValues[1]
                    implementations.add(funcName)
                    logger.info("ImplScanner: Found implementation '$funcName' in ${file.name}")
                }
            }

        return implementations
    }

    /**
     * Check if an implementation exists for a given enhancement.
     * Checks both legacy (makeFoo) and new (initFoo) patterns.
     */
    fun hasImplementation(implementations: Set<String>, objectName: String): Boolean {
        val legacyName = "make$objectName"
        val newName = "init$objectName"
        return legacyName in implementations || newName in implementations
    }

    /**
     * Get the implementation function name for an enhancement.
     * Prefers new pattern (initFoo) over legacy (makeFoo).
     */
    fun getImplementationName(implementations: Set<String>, objectName: String): String? {
        val newName = "init$objectName"
        val legacyName = "make$objectName"
        return when {
            newName in implementations -> newName
            legacyName in implementations -> legacyName
            else -> null
        }
    }
}
