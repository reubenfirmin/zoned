package zoned.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.DefaultTask
import java.io.File
import javax.inject.Inject
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.LogLevel.INFO
import org.gradle.api.logging.configuration.ShowStacktrace

abstract class BuildStyleTask @Inject constructor(
    private val layout: ProjectLayout
) : DefaultTask() {

    @get:OutputDirectory
    abstract val librarySrcDir: DirectoryProperty

    @get:InputFile
    abstract val inputCssFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    abstract val configFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    private lateinit var npxTask: NpxTask

    init {
        // Set up default file locations
        val projectDir = layout.projectDirectory
        inputCssFile.convention(projectDir.file("src/jvmMain/resources/style.css"))
        configFile.convention(projectDir.file("tailwind.config.js"))
        outputFile.convention(projectDir.file("dist/output.css"))
        librarySrcDir.convention(layout.buildDirectory.dir("tmp/library-src"))

        // This task should run after build
        dependsOn("build")
    }

    @TaskAction
    fun execute() {
        setupNpxTask()
        setupTailwind()
        npxTask.exec()
    }

    private fun setupNpxTask() {
        npxTask = project.tasks.create("buildStyleNpx", NpxTask::class.java)
        npxTask.command.set("@tailwindcss/cli")

        // Set up NODE_PATH
        val jsDir = project.layout.buildDirectory.dir("js/node_modules").get().asFile
        if (!jsDir.exists()) {
            throw Exception("Need to build before running build-style")
        }
        npxTask.environment.set(mapOf(
            "NODE_PATH" to project.rootDir.toPath().relativize(jsDir.toPath()).toString()
        ))
    }

    private fun setupTailwind() {
        logger.lifecycle("Running tailwind!")
        // Create temp file in project root
        val tempInputFile = layout.projectDirectory.file(".temp.css").asFile
        logger.warn(tempInputFile.absolutePath)

        // Ensure output directory exists
        outputFile.get().asFile.parentFile.mkdirs()

        // Extract library sources if they exist
        project.configurations.findByName("compileClasspath")?.let { classpath ->
            classpath.find { it.name.contains("zoned") }?.let { jar ->
                val sourcesJar = jar.toString().replace(".jar", "-sources.jar")
                if (File(sourcesJar).exists()) {
                    librarySrcDir.get().asFile.apply {
                        deleteRecursively()
                        mkdirs()
                    }
                    project.copy { copySpec ->
                        copySpec.from(project.zipTree(sourcesJar))
                        copySpec.into(librarySrcDir.get())
                    }
                } else {
                    println("Couldn't find sources jar!")
                }
            }
        }

        val inputCss = inputCssFile.get().asFile.readText()

        if (inputCss.contains("@zoned")) {
            println("Replacing @zoned token")
            val configDirective = if (configFile.get().asFile.exists()) {
                "@config \"./tailwind.config.js\";"
            } else ""

            val sourceDirectives = listOf(
                "src/jvmMain/kotlin/",
                "src/jsMain/kotlin/",
                "build/tmp/library-src",
                "build/js/node_modules/flowbite/"
            ).joinToString("\n") { "@source \"$it\";" }

            val processedCss = """
                |@import "tailwindcss";
                |$configDirective
                |$sourceDirectives
                |${inputCss.replace("@zoned", "")}
                """.trimMargin()

            tempInputFile.writeText(processedCss)
        }

        // Set up the args for the npx command
        npxTask.args.set(listOf(
            "-i", if (inputCss.contains("@zoned")) {
                tempInputFile.absolutePath
            } else {
                inputCssFile.asFile.get().absolutePath
            },
            "-o", outputFile.asFile.get().absolutePath
        ))

        println("${npxTask.command.get()} ${npxTask.args.get()} ${npxTask.environment.get()}}")
    }

    @InputFiles
    fun getSourceFiles() = project.fileTree("src/jvmMain/kotlin") +
            project.fileTree("src/jsMain/kotlin") +
            project.fileTree(librarySrcDir)
}