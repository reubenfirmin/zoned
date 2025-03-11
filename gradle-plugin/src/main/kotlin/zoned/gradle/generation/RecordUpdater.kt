package zoned.gradle.generation

import org.gradle.api.Project
import java.io.File

/**
 * Task that updates jOOQ Record classes to properly support sealed Entity classes.
 * This runs after code generation to adapt the standard jOOQ records to work with
 * the sealed class pattern used for entities with an ID primary key.
 */
open class RecordUpdater(val project: Project){


    fun updateRecords() {
        println("Updating jOOQ Record classes to support sealed entities...")

        // Find the source directory (same as in JooqGenerator)
        val sourceDir = findSourceDirectory()
        val modelPackage = findModelPackage(sourceDir)

        // Directory where jOOQ generates the record classes
        val recordsDir = File("$sourceDir/${modelPackage.replace('.', '/')}/jooq/tables/records")
        if (!recordsDir.exists() || !recordsDir.isDirectory) {
            println("Records directory not found: ${recordsDir.absolutePath}")
            return
        }

        // Directory where entity POJOs are generated
        val pojosDir = File("$sourceDir/${modelPackage.replace('.', '/')}/jooq/tables/pojos")
        if (!pojosDir.exists() || !pojosDir.isDirectory) {
            println("POJOs directory not found: ${pojosDir.absolutePath}")
            return
        }

        // Find all entity classes that use the sealed class pattern
        val sealedEntityClasses = findSealedEntityClasses(pojosDir)
        if (sealedEntityClasses.isEmpty()) {
            println("No sealed entity classes found, no updates needed")
            return
        }

        println("Found ${sealedEntityClasses.size} sealed entity classes to process")

        // For each record class that corresponds to a sealed entity
        recordsDir.listFiles { file ->
            file.isFile && file.name.endsWith("Record.kt")
        }?.forEach { recordFile ->
            val sealed = sealedEntityClasses.any { entity ->
                recordFile.name.startsWith(entity)
            }

            updateRecordFile(recordFile, modelPackage, sealed)
        }

        println("Record update completed")
    }

    /**
     * Find all entity classes that use the sealed class pattern
     */
    private fun findSealedEntityClasses(pojosDir: File): List<String> {
        val sealedClasses = mutableListOf<String>()

        pojosDir.listFiles { file -> file.isFile && file.name.endsWith(".kt") }?.forEach { file ->
            val content = file.readText()
            if (content.contains("sealed class") && content.contains("data class Existing")) {
                // Extract the class name without extension
                sealedClasses.add(file.nameWithoutExtension)
            }
        }

        return sealedClasses
    }

    /**
     * Update a specific record file to use the sealed class Existing variant
     */
    private fun updateRecordFile(recordFile: File, modelPackage: String, wasSealed: Boolean) {
        println("Updating record file: ${recordFile.name}, sealed: $wasSealed")

        val content = recordFile.readText()
        val recordClassName = recordFile.nameWithoutExtension
        val entityClassName = recordClassName.removeSuffix("Record")

        // Look for specific text patterns that we know exist in the constructor
        // This is a much more direct approach that doesn't rely on complex regex patterns
        val startPattern = "constructor(value:"
        val metadataFields = listOf(
            "this.created = value.created",
            "this.modified = value.modified",
            "this.deleted = value.deleted"
        )

        // Find the start of the constructor section
        val constructorStart = content.indexOf(startPattern)
        if (constructorStart == -1) {
            println("Could not find constructor in ${recordFile.name}")
            return
        }

        // Find the end by looking for the closing brace of the constructor
        // We'll need to track balanced braces to find the right closing one
        var pos = constructorStart
        var openBraces = 0
        var constructorEnd = -1

        while (pos < content.length) {
            when (content[pos]) {
                '{' -> openBraces++
                '}' -> {
                    openBraces--
                    if (openBraces == 0) {
                        constructorEnd = pos + 1
                        break
                    }
                }
            }
            pos++
        }

        if (constructorEnd == -1) {
            println("Could not find end of constructor in ${recordFile.name}")
            return
        }

        // Extract the constructor code
        val constructorCode = content.substring(constructorStart, constructorEnd)

        // Create a modified version with metadata fields removed
        var modifiedCode = constructorCode
        for (field in metadataFields) {
            modifiedCode = modifiedCode.replace(field, "")
        }

        // Update sealed class references if needed
        if (wasSealed) {
            // Find the package path in the constructor
            val packagePathPattern = """value: ([^?]+)\?""".toRegex()
            val match = packagePathPattern.find(modifiedCode)
            if (match != null) {
                val path = match.groupValues[1].trim()
                if (!path.endsWith(".Existing")) {
                    modifiedCode = modifiedCode.replace(path, "$path.Existing")
                }
            }
        }

        // Replace the constructor in the file content
        val updatedContent = content.replace(constructorCode, modifiedCode)

        // Write the updated content back to the file
        recordFile.writeText(updatedContent)

        println("Successfully updated record constructor in ${recordFile.name}")
    }

    private fun findSourceDirectory(): String {
        val kotlinDir = project.file("src/main/kotlin")
        val jvmMainDir = project.file("src/jvmMain/kotlin")

        return when {
            kotlinDir.exists() -> kotlinDir.absolutePath
            jvmMainDir.exists() -> jvmMainDir.absolutePath
            else -> throw IllegalStateException("Neither src/main/kotlin nor src/jvmMain/kotlin directory found")
        }
    }

    private fun findModelPackage(sourceDir: String): String {
        val sourceDirFile = File(sourceDir)
        val modelDir = sourceDirFile.walkTopDown()
            .filter { it.isDirectory && it.name == "model" }
            .firstOrNull()
            ?: throw IllegalStateException("No model directory found in $sourceDir")

        return modelDir.toRelativeString(sourceDirFile)
            .replace(File.separator, ".")
            .trimStart('.')
    }
}