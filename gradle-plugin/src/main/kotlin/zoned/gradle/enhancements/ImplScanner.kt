package zoned.gradle.enhancements

import org.gradle.api.logging.Logger
import java.io.File

/**
 * Scans jsMain source for enhancement implementation functions.
 *
 * ONLY accepts the TagConsumer pattern:
 *   @EnhancementImpl(FooEnhancement::class)
 *   fun TagConsumer<HTMLElement>.initFooEnhancement(config: FooConfig, children: List<Node>)
 */
class ImplScanner(private val logger: Logger) {

    // Matches @EnhancementImpl(FooEnhancement::class) followed by TagConsumer extension function
    private val tagConsumerImplRegex = """@EnhancementImpl\s*\(\s*(\w+)\s*::\s*class\s*\)\s*(?:\n\s*)?fun\s+TagConsumer\s*<[^>]+>\s*\.(init\w+)\s*\(""".toRegex()

    // Maps enhancement object name -> implementation function name
    private val implementations = mutableMapOf<String, String>()

    /**
     * Scan jsMain directory for TagConsumer-based enhancement implementations.
     * @return Set of enhancement object names that have valid implementations
     */
    fun scanForImplementations(jsMainDir: File): Set<String> {
        if (!jsMainDir.exists()) {
            logger.info("ImplScanner: jsMain directory not found at ${jsMainDir.absolutePath}")
            return emptySet()
        }

        implementations.clear()

        jsMainDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val content = file.readText()

                tagConsumerImplRegex.findAll(content).forEach { match ->
                    val enhancementName = match.groupValues[1]
                    val funcName = match.groupValues[2]
                    implementations[enhancementName] = funcName
                    logger.info("ImplScanner: Found TagConsumer impl '$funcName' for $enhancementName in ${file.name}")
                }
            }

        return implementations.keys
    }

    /**
     * Check if a valid TagConsumer implementation exists for the enhancement.
     */
    fun hasImplementation(objectName: String): Boolean {
        return objectName in implementations
    }

    /**
     * Get the implementation function name for an enhancement.
     */
    fun getImplementationName(objectName: String): String? {
        return implementations[objectName]
    }
}
