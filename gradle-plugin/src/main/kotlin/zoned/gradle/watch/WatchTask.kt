package zoned.gradle.watch

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

abstract class WatchTask @Inject constructor(private val project: Project) : org.gradle.api.DefaultTask() {
    init {
        description = "Watch for changes and rebuild styles"
        group = "application"
    }

    @TaskAction
    fun watch() {
        println("Starting Watch Task...")

        val kotlinSourceDir = project.file("src/jsMain/kotlin")
        val templateCssFile = project.file("src/jsMain/resources/template.css")
        val outputCssFile = project.file("src/jsMain/resources/style.css")

        println("Watching directories:")
        println("  Kotlin source: $kotlinSourceDir")
        println("  Template CSS: $templateCssFile")
        println("  Output CSS: $outputCssFile")

        val executor = Executors.newSingleThreadScheduledExecutor()
        var jsBrowserRunProcess: Process? = null

        fun executeJsBrowserRun() {
            println("Executing jsBrowserRun task...")

            jsBrowserRunProcess?.destroy()

            try {
                val processBuilder = ProcessBuilder("./gradlew", "jsBrowserRun")
                processBuilder.redirectErrorStream(true)
                jsBrowserRunProcess = processBuilder.start()

                // Read the output in a separate thread
                Thread {
                    val reader = BufferedReader(InputStreamReader(jsBrowserRunProcess!!.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        println(line)
                    }
                }.start()

            } catch (e: Exception) {
                println("Error in jsBrowserRun task: ${e.message}")
                e.printStackTrace()
            }
        }

        fun shutdown() {
            println("Shutting down watch task...")
            executor.shutdownNow()
            jsBrowserRunProcess?.destroy()
        }

        val shutdownHook = Thread { shutdown() }
        Runtime.getRuntime().addShutdownHook(shutdownHook)

        try {
            executor.scheduleWithFixedDelay({
                try {
                    val outputCssModifiedTime = if (outputCssFile.exists()) Files.getLastModifiedTime(outputCssFile.toPath()).toMillis() else 0L
                    val templateCssChanged = Files.getLastModifiedTime(templateCssFile.toPath()).toMillis() > outputCssModifiedTime
                    val kotlinSourceChanged = kotlinSourceDir.walk().filter { it.isFile }.any {
                        Files.getLastModifiedTime(it.toPath()).toMillis() > outputCssModifiedTime
                    }

                    if (templateCssChanged || kotlinSourceChanged) {
                        println("Changes detected! Rebuilding styles...")
                        outputCssFile.delete()
                        project.tasks.getByName("tailwind").actions.forEach { it.execute(project.tasks.getByName("tailwind")) }

                        println("Restarting jsBrowserRun...")
                        executeJsBrowserRun()
                    }
                } catch (e: Exception) {
                    println("Error in watch task: ${e.message}")
                    e.printStackTrace()
                }
            }, 0, 1, TimeUnit.SECONDS)

            println("Starting initial jsBrowserRun...")
            executeJsBrowserRun()

            println("Watch task is now running. Press Ctrl+C to stop.")
            while (!Thread.currentThread().isInterrupted) {
                Thread.sleep(1000)
            }
        } finally {
            shutdown()
            Runtime.getRuntime().removeShutdownHook(shutdownHook)
        }
    }
}