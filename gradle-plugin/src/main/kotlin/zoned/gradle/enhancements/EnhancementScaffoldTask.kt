package zoned.gradle.enhancements

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

/**
 * Gradle task that scaffolds a new enhancement with both definition and implementation files.
 *
 * Usage: ./gradlew scaffold-enhancement --name=tooltip --package=myapp.enhancements
 */
open class EnhancementScaffoldTask : DefaultTask() {

    @Input
    @Option(option = "name", description = "Enhancement name (e.g., 'tooltip' or 'drag-drop')")
    var enhancementName: String = ""

    @Input
    @Option(option = "package", description = "Package name (e.g., 'myapp.enhancements')")
    var packageName: String = ""

    @TaskAction
    fun scaffold() {
        require(enhancementName.isNotBlank()) { "Enhancement name required. Use --name=<name>" }
        require(packageName.isNotBlank()) { "Package name required. Use --package=<package>" }

        val objectName = enhancementName.toPascalCase() + "Enhancement"
        val configName = enhancementName.toPascalCase() + "Config"
        val initFnName = "init$objectName"
        val packagePath = packageName.replace(".", "/")

        // Generate commonMain definition with expect declaration
        val commonDir = project.file("src/commonMain/kotlin/$packagePath")
        commonDir.mkdirs()

        val definitionFile = File(commonDir, "$objectName.kt")
        if (definitionFile.exists()) {
            project.logger.warn("File already exists: ${definitionFile.absolutePath}")
        } else {
            definitionFile.writeText(generateDefinition(packageName, objectName, configName, enhancementName, initFnName))
            project.logger.lifecycle("Created: ${definitionFile.absolutePath}")
        }

        // Generate jsMain implementation with actual declaration
        val jsDir = project.file("src/jsMain/kotlin/$packagePath")
        jsDir.mkdirs()

        val implFile = File(jsDir, "${objectName}Impl.kt")
        if (implFile.exists()) {
            project.logger.warn("File already exists: ${implFile.absolutePath}")
        } else {
            implFile.writeText(generateImplementation(packageName, objectName, configName, initFnName))
            project.logger.lifecycle("Created: ${implFile.absolutePath}")
        }

        project.logger.lifecycle("""
            |
            |Enhancement '$enhancementName' scaffolded successfully!
            |
            |Files created:
            |  - src/commonMain/kotlin/$packagePath/$objectName.kt
            |  - src/jsMain/kotlin/$packagePath/${objectName}Impl.kt
            |
            |Next steps:
            |  1. Add config properties to $configName (use 'var' not 'val')
            |  2. Implement client-side behavior in $initFnName()
            |  3. Run ./gradlew build
            |  4. Use in HTML: div { ${enhancementName.toCamelCase()} { ... } }
        """.trimMargin())
    }

    private fun generateDefinition(
        pkg: String,
        objectName: String,
        configName: String,
        name: String,
        initFnName: String
    ) = """
        |package $pkg
        |
        |import kotlinx.serialization.Serializable
        |import zoned.framework.ui.enhancements.ClientEnhancement
        |import zoned.framework.ui.enhancements.Enhancement
        |import zoned.framework.ui.enhancements.EnhancementConfig
        |
        |/**
        | * Enhancement for [TODO: describe what this enhancement does].
        | *
        | * Usage:
        | * ```kotlin
        | * div {
        | *     ${name.toCamelCase()} {
        | *         // Configure options here
        | *     }
        | * }
        | * ```
        | */
        |@ClientEnhancement
        |object $objectName : Enhancement<$configName> {
        |    override val name = "$name"
        |    override val configSerializer = $configName.serializer()
        |}
        |
        |/**
        | * Configuration for the $name enhancement.
        | *
        | * NOTE: Use 'var' for properties to enable DSL configuration syntax.
        | */
        |@Serializable
        |data class $configName(
        |    /** Example property - replace with your actual config options. */
        |    var example: String = ""
        |) : EnhancementConfig
        |
    """.trimMargin()

    private fun generateImplementation(
        pkg: String,
        objectName: String,
        configName: String,
        initFnName: String
    ) = """
        |package $pkg
        |
        |import zoned.framework.ui.enhancements.EnhancementElement
        |
        |/**
        | * Client-side implementation of the $objectName enhancement.
        | * This function is auto-discovered by the generated enhancement registry.
        | */
        |fun $initFnName(element: EnhancementElement, config: $configName) {
        |    // TODO: Implement your enhancement logic here
        |    //
        |    // Example:
        |    // element.css {
        |    //     display = Display.flex
        |    //     gap = 8.px
        |    // }
        |    //
        |    // element.appendChild(element.create.div {
        |    //     className = "my-component"
        |    //     textContent = config.example
        |    // })
        |
        |    console.log("$objectName initialized with config:", config)
        |}
        |
    """.trimMargin()

    private fun String.toPascalCase() = split("-").joinToString("") {
        it.replaceFirstChar { c -> c.uppercase() }
    }

    private fun String.toCamelCase() = split("-").mapIndexed { i, s ->
        if (i == 0) s.lowercase() else s.replaceFirstChar { it.uppercase() }
    }.joinToString("")
}
