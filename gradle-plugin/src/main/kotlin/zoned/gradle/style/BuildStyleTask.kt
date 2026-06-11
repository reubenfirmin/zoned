package zoned.gradle.style

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import com.github.gradle.node.npm.task.NpxTask
import org.gradle.api.DefaultTask
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Runs tailwind CSS build; output not portable across machines")
abstract class BuildStyleTask @Inject constructor(
    private val layout: ProjectLayout
) : DefaultTask() {

    @get:OutputDirectory
    abstract val librarySrcDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputCssFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val configFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    private lateinit var npxTask: NpxTask

    init {
        // Set up default file locations. Full-stack projects keep the source CSS under
        // jvmMain; frontend-only (JS) projects have no jvmMain, so fall back to jsMain.
        val projectDir = layout.projectDirectory
        val jvmCss = projectDir.file("src/jvmMain/resources/style.css")
        val jsCss = projectDir.file("src/jsMain/resources/style.css")
        inputCssFile.convention(if (jvmCss.asFile.exists()) jvmCss else jsCss)
        // Only point at tailwind.config.js when it exists: @Optional allows an UNSET property, but
        // a set-by-convention path to a missing file still fails Gradle's input validation at
        // configuration time — before execute() could skip — breaking typed-CSS-only projects.
        val tailwindConfig = projectDir.file("tailwind.config.js")
        if (tailwindConfig.asFile.exists()) {
            configFile.convention(tailwindConfig)
        }
        outputFile.convention(projectDir.file("dist/output.css"))
        librarySrcDir.convention(layout.buildDirectory.dir("tmp/library-src"))

        // we had "build" here, but that's too heavyweight if there are tests
        dependsOn("kotlinNpmInstall")
    }

    @TaskAction
    fun execute() {
        // Apps with fully-typed styling (css{} + styleSheet{}) have no tailwind.config.js — the
        // style step is a no-op for them, so watch scripts / CI can invoke build-style universally.
        if (!configFile.isPresent) {
            logger.lifecycle("build-style: no tailwind.config.js — skipping (typed-CSS-only project)")
            return
        }
        setupNpxTask()
        setupTailwind()
        npxTask.exec()
    }

    private fun setupNpxTask() {
        npxTask = project.tasks.create("buildStyleNpx", NpxTask::class.java)
        
        // Use the bundled tailwindcss from build/js/node_modules instead of npx
        val tailwindCli = project.layout.buildDirectory.dir("js/node_modules/@tailwindcss/cli/dist/index.mjs").get().asFile
        npxTask.command.set("node")
        npxTask.args.set(listOf(tailwindCli.absolutePath))

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
        // Try jvmCompileClasspath first (for KMP projects), fall back to compileClasspath
        val configuration = project.configurations.findByName("jvmCompileClasspath")
            ?: project.configurations.findByName("compileClasspath")

        configuration?.let { classpath ->
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
                    logger.lifecycle("Extracted zoned sources from: $sourcesJar")
                } else {
                    logger.warn("Couldn't find sources jar at: $sourcesJar")
                }
            } ?: logger.warn("No zoned jar found in classpath")
        }

        val inputCss = inputCssFile.get().asFile.readText()

        if (inputCss.contains("@zoned")) {
            println("Replacing @zoned token")
            val sourceDirectives = listOf(
                "src/jvmMain/kotlin/**/*.kt",
                "src/jsMain/kotlin/**/*.kt",
                "build/tmp/library-src/**/*.kt",
                "build/js/node_modules/flowbite/**/*.js"
            ).joinToString("\n") { "@source \"$it\";" }

            val processedCss = """
                |@import "tailwindcss";
                |@source inline("sm:block md:block lg:block xl:block 2xl:block");
                |@source inline("sm:hidden md:hidden lg:hidden xl:hidden 2xl:hidden");
                |@source inline("sm:flex md:flex lg:flex xl:flex 2xl:flex");
                |@source inline("sm:flex-row md:flex-row lg:flex-row xl:flex-row 2xl:flex-row");
                |@source inline("sm:w-auto md:w-auto lg:w-auto xl:w-auto 2xl:w-auto");
                |$sourceDirectives
                |${inputCss.replace("@zoned", "")}
                """.trimMargin()

            tempInputFile.writeText(processedCss)
        }

        // Set up the args for the node command
        val currentArgs = npxTask.args.get().toMutableList()
        currentArgs.addAll(listOf(
            "-i", if (inputCss.contains("@zoned")) {
                tempInputFile.absolutePath
            } else {
                inputCssFile.asFile.get().absolutePath
            },
            "-o", outputFile.asFile.get().absolutePath,
            "--minify"
        ))
        npxTask.args.set(currentArgs)

        println("${npxTask.command.get()} ${npxTask.args.get()} ${npxTask.environment.get()}}")
    }

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getSourceFiles() = project.fileTree("src/jvmMain/kotlin") +
            project.fileTree("src/jsMain/kotlin") +
            project.fileTree(librarySrcDir)
}