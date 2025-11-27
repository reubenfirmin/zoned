package zoned.gradle.enhancements

import org.gradle.api.logging.Logger
import java.io.File

/**
 * Scans the codebase for enhancement definitions marked with @ClientEnhancement
 */
class EnhancementScanner(private val logger: Logger) {

    /**
     * Scans the commonMain source directory for enhancement definitions
     */
    fun scanForEnhancements(commonMainDir: File): List<DiscoveredEnhancement> {
        if (!commonMainDir.exists()) {
            logger.lifecycle("commonMain directory does not exist: ${commonMainDir.absolutePath}")
            return emptyList()
        }

        val enhancements = mutableListOf<DiscoveredEnhancement>()
        val seenNames = mutableSetOf<String>()

        commonMainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val content = file.readText()

                // Look for @ClientEnhancement annotation
                if (content.contains("@ClientEnhancement")) {
                    val enhancement = parseEnhancement(file, content)
                    if (enhancement != null && !seenNames.contains(enhancement.name)) {
                        enhancements.add(enhancement)
                        seenNames.add(enhancement.name)
                        logger.lifecycle("Found enhancement: ${enhancement.name} (${enhancement.objectName})")
                    }
                }
            }

        return enhancements
    }

    /**
     * Parse an enhancement definition from a Kotlin file
     */
    private fun parseEnhancement(file: File, content: String): DiscoveredEnhancement? {
        // Extract package name
        val packageRegex = """package\s+([\w.]+)""".toRegex()
        val packageName = packageRegex.find(content)?.groupValues?.get(1)
        if (packageName == null) {
            logger.warn("Could not extract package from ${file.name}")
            return null
        }

        // Extract object name and config type from: object Foo : Enhancement<BarConfig>
        val objectRegex = """object\s+(\w+)\s*:\s*Enhancement<(\w+)>""".toRegex()
        val match = objectRegex.find(content)
        if (match == null) {
            logger.warn("Could not extract Enhancement object from ${file.name}")
            return null
        }

        val objectName = match.groupValues[1]
        val configClass = match.groupValues[2]

        // Extract name from: override val name = "sortable" or "leaflet-map"
        val nameRegex = """override\s+val\s+name\s*=\s*"([\w-]+)"""".toRegex()
        val name = nameRegex.find(content)?.groupValues?.get(1) ?: objectName.lowercase()

        return DiscoveredEnhancement(
            name = name,
            objectName = objectName,
            configClass = configClass,
            packageName = packageName
        )
    }
}
