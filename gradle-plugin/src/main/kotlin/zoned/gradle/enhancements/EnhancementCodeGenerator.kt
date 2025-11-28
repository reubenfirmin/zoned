package zoned.gradle.enhancements

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Gradle task that generates enhancement DSL and registry code
 */
open class EnhancementCodeGenerator : DefaultTask() {

    @TaskAction
    fun generate() {
        val scanner = EnhancementScanner(project.logger)
        val implScanner = ImplScanner(project.logger)

        // Find commonMain directory
        val commonMainDir = project.file("src/commonMain/kotlin")
        if (!commonMainDir.exists()) {
            project.logger.lifecycle("No commonMain directory found, skipping enhancement generation")
            return
        }

        // Scan for enhancements
        val enhancements = scanner.scanForEnhancements(commonMainDir)
        project.logger.lifecycle("Found ${enhancements.size} enhancements: ${enhancements.joinToString { it.name }}")

        if (enhancements.isEmpty()) {
            project.logger.lifecycle("No enhancements found, skipping code generation")
            return
        }

        // Scan for TagConsumer-based implementations in jsMain (only pattern supported)
        val jsMainDir = project.file("src/jsMain/kotlin")
        implScanner.scanForImplementations(jsMainDir)

        // Validate that all enhancements have TagConsumer implementations
        val missingImpls = enhancements.filter { enh ->
            !implScanner.hasImplementation(enh.objectName)
        }
        if (missingImpls.isNotEmpty()) {
            val message = buildString {
                appendLine("Missing enhancement implementations in jsMain:")
                appendLine()
                missingImpls.forEach { enh ->
                    appendLine("  - ${enh.objectName} requires TagConsumer implementation:")
                    appendLine("      @EnhancementImpl(${enh.objectName}::class)")
                    appendLine("      fun TagConsumer<HTMLElement>.init${enh.objectName}(config: ${enh.configClass}, children: List<Node>)")
                    appendLine()
                }
                appendLine("Note: Legacy patterns (EnhancementElement, raw Element) are no longer supported.")
                appendLine("All implementations must use the TagConsumer extension function pattern.")
            }
            throw org.gradle.api.GradleException(message)
        }

        // Generate JVM DSL
        generateJvmDSL(enhancements)

        // Generate JS Registry (TagConsumer only)
        generateJsRegistry(enhancements, implScanner)

        project.logger.lifecycle("Enhancement code generation complete")
    }

    /**
     * Generate JVM-side DSL extension functions.
     *
     * Generates wrapper-style DSL functions that create a container div with
     * enhancement data attributes, and render child content inside.
     *
     * Example output:
     * ```
     * fun FlowContent.wysiwyg(configure: WysiwygConfig.() -> Unit = {}, content: FlowContent.() -> Unit = {}) {
     *     div {
     *         val config = WysiwygConfig().apply(configure)
     *         enhance(WysiwygEnhancement, config)
     *         content()
     *     }
     * }
     * ```
     */
    private fun generateJvmDSL(enhancements: List<DiscoveredEnhancement>) {
        // Use project-specific package to avoid conflicts
        val projectPackage = "${project.name.lowercase().replace("-", "")}.enhancements"
        val outputDir = File(project.buildDir, "generated/kotlin/${projectPackage.replace(".", "/")}")
        outputDir.mkdirs()

        // Project-specific DSL file name
        val dslFileName = "${project.name.replaceFirstChar { it.uppercase() }.replace("-", "")}EnhancementDSL"

        // Collect imports
        val imports = enhancements.flatMap { enh ->
            listOf(
                "import ${enh.packageName}.${enh.objectName}",
                "import ${enh.packageName}.${enh.configClass}"
            )
        }.distinct()

        // Generate extension function for each enhancement
        val extensionFunctions = enhancements.joinToString("\n\n") { enh ->
            // Convert hyphenated name to camelCase for valid Kotlin identifier
            val functionName = enh.name.toCamelCase()

            if (enh.clientBuildsContent) {
                // Config-only DSL: client builds all UI, no server content needed
                // configure must be LAST for trailing lambda syntax to work
                """
                |/**
                | * Type-safe DSL for ${enh.name} enhancement.
                | * Creates a wrapper div with enhancement data attributes.
                | * The client builds all UI from config - no server content is passed.
                | *
                | * Example:
                | * ```
                | * $functionName { /* config */ }
                | * $functionName("w-full h-full") { /* config */ }
                | * ```
                | */
                |fun FlowContent.$functionName(
                |    classes: String = "",
                |    configure: ${enh.configClass}.() -> Unit = {}
                |) {
                |    div(classes) {
                |        val config = ${enh.configClass}().apply(configure)
                |        enhance(${enh.objectName}, config)
                |    }
                |}
                """.trimMargin()
            } else {
                // Wrapper DSL: server provides content that client wraps
                // content must be LAST for trailing lambda syntax to work
                """
                |/**
                | * Type-safe DSL for ${enh.name} enhancement.
                | * Creates a wrapper div with enhancement data attributes.
                | * Child content is rendered inside and will be available to the client-side implementation.
                | *
                | * Example:
                | * ```
                | * $functionName({ /* config */ }) { /* content */ }
                | * $functionName("my-class", { /* config */ }) { /* content */ }
                | * ```
                | */
                |fun FlowContent.$functionName(
                |    classes: String = "",
                |    configure: ${enh.configClass}.() -> Unit = {},
                |    content: FlowContent.() -> Unit = {}
                |) {
                |    div(classes) {
                |        val config = ${enh.configClass}().apply(configure)
                |        enhance(${enh.objectName}, config)
                |        content()
                |    }
                |}
                """.trimMargin()
            }
        }

        val code = """
            |package $projectPackage
            |
            |import kotlinx.html.CommonAttributeGroupFacade
            |import kotlinx.html.FlowContent
            |import kotlinx.html.div
            |import kotlinx.serialization.json.Json
            |import zoned.framework.ui.enhancements.Enhancement
            |import zoned.framework.ui.enhancements.EnhancementConfig
            |${imports.joinToString("\n")}
            |
            |/**
            | * Auto-generated by Zoned Gradle Plugin for ${project.name}
            | * Generated from enhancements found in commonMain
            | *
            | * DO NOT EDIT THIS FILE MANUALLY
            | */
            |
            |/**
            | * Generic enhance function that adds data attributes for client-side discovery
            | */
            |fun <TConfig : EnhancementConfig> CommonAttributeGroupFacade.enhance(
            |    enhancement: Enhancement<TConfig>,
            |    config: TConfig
            |) {
            |    val configJson = Json.encodeToString(enhancement.configSerializer, config)
            |    attributes["data-enhancement"] = enhancement.name
            |    attributes["data-enhancement-config"] = configJson
            |}
            |
            |$extensionFunctions
        """.trimMargin()

        val outputFile = File(outputDir, "$dslFileName.kt")
        outputFile.writeText(code)
        project.logger.lifecycle("Generated JVM DSL: ${outputFile.absolutePath}")
    }

    /**
     * Generate JS-side auto-discovery registry.
     *
     * All implementations use the TagConsumer pattern:
     * - Captures server-rendered children before clearing
     * - Clears the element
     * - Uses appendTo(element) to get TagConsumer
     * - Calls the extension function with config and children
     */
    private fun generateJsRegistry(
        enhancements: List<DiscoveredEnhancement>,
        implScanner: ImplScanner
    ) {
        // Use project-specific package to avoid conflicts
        val projectPackage = "${project.name.lowercase().replace("-", "")}.enhancements"
        val outputDir = File(project.buildDir, "generated/kotlin-js/${projectPackage.replace(".", "/")}")
        outputDir.mkdirs()

        // Project-specific registry name
        val registryName = "${project.name.replaceFirstChar { it.uppercase() }.replace("-", "")}EnhancementRegistry"

        // Collect imports for enhancement objects, configs, and impl functions
        val imports = enhancements.flatMap { enh ->
            val implName = implScanner.getImplementationName(enh.objectName)
                ?: throw IllegalStateException("No implementation found for ${enh.objectName}")
            listOf(
                "import ${enh.packageName}.${enh.objectName}",
                "import ${enh.packageName}.${enh.configClass}",
                "import ${enh.packageName}.$implName"
            )
        }.distinct()

        // Generate registration calls
        val registrations = enhancements.joinToString("\n        ") { enh ->
            """register("${enh.name}", ::_init${enh.objectName})"""
        }

        // Generate handler functions - all use TagConsumer pattern
        val handlers = enhancements.joinToString("\n\n    ") { enh ->
            val implName = implScanner.getImplementationName(enh.objectName)!!
            """
            |private fun _init${enh.objectName}(element: Element, configJson: String) {
            |    val config = Json.decodeFromString(${enh.objectName}.configSerializer, configJson)
            |    val htmlElement = element as HTMLElement
            |
            |    // Capture server-rendered children before clearing
            |    val children = mutableListOf<Node>()
            |    while (htmlElement.firstChild != null) {
            |        children.add(htmlElement.removeChild(htmlElement.firstChild!!))
            |    }
            |
            |    // Rebuild element using TagConsumer DSL
            |    htmlElement.appendTo().apply {
            |        $implName(config, children)
            |    }
            |}
            """.trimMargin()
        }

        val code = """
            |package $projectPackage
            |
            |import kotlinx.serialization.json.Json
            |import web.dom.Element
            |import web.dom.Node
            |import web.dom.document
            |import web.html.HTMLElement
            |import zoned.framework.interop.appendTo
            |${imports.joinToString("\n")}
            |
            |/**
            | * Auto-generated enhancement registry for ${project.name}
            | * Generated by Zoned Gradle Plugin
            | *
            | * All handlers use the TagConsumer pattern:
            | * 1. Capture server-rendered children
            | * 2. Clear element
            | * 3. Rebuild using TagConsumer DSL with captured children
            | *
            | * DO NOT EDIT THIS FILE MANUALLY
            | */
            |object $registryName {
            |
            |    private val handlers = mutableMapOf<String, (Element, String) -> Unit>()
            |
            |    init {
            |        $registrations
            |    }
            |
            |    private fun register(name: String, handler: (Element, String) -> Unit) {
            |        handlers[name] = handler
            |    }
            |
            |    /**
            |     * Initialize all enhancements found in the DOM.
            |     * Call this after page load or after HTMX content swaps.
            |     */
            |    fun initialize(root: Element? = document.body) {
            |        val actualRoot = root ?: return // Skip if no root element (e.g., during early script execution)
            |        val elements = actualRoot.querySelectorAll("[data-enhancement]")
            |        for (i in 0 until elements.length) {
            |            val element = elements.item(i) ?: continue
            |
            |            // Skip if already enhanced
            |            if (element.hasAttribute("data-enhanced")) continue
            |
            |            val enhancementName = element.getAttribute("data-enhancement") ?: continue
            |            val configJson = element.getAttribute("data-enhancement-config") ?: "{}"
            |
            |            // Skip if not handled by this registry (another registry may handle it)
            |            val handler = handlers[enhancementName] ?: continue
            |            try {
            |                handler(element, configJson)
            |                element.setAttribute("data-enhanced", "true")
            |            } catch (e: Exception) {
            |                console.error("Failed to initialize enhancement '${'$'}enhancementName':", e)
            |            }
            |        }
            |    }
            |
            |    $handlers
            |}
        """.trimMargin()

        val outputFile = File(outputDir, "${registryName}.kt")
        outputFile.writeText(code)
        project.logger.lifecycle("Generated JS Registry: ${outputFile.absolutePath}")
    }
}

/**
 * Convert a hyphenated name to camelCase for valid Kotlin identifiers.
 * Example: "leaflet-map" -> "leafletMap"
 */
private fun String.toCamelCase(): String {
    return this.split("-").mapIndexed { index, part ->
        if (index == 0) part.lowercase()
        else part.replaceFirstChar { it.uppercase() }
    }.joinToString("")
}
