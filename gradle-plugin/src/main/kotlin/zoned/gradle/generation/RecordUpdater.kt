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

        val transforms: MutableList<(String) -> String> = mutableListOf()
        transforms.add { it.replace("this.created = value.created", "") }
        transforms.add { it.replace("this.modified = value.modified", "") }
        transforms.add { it.replace("this.deleted = value.deleted", "") }

        if (wasSealed && !content.contains("$entityClassName.Existing")) {
            transforms.add { it.replace("pojos.${entityClassName}", "pojos.$entityClassName.Existing") }
        }

        // Find the constructor that takes the POJO as a parameter
        val pojoPackage = "$modelPackage.jooq.tables.pojos"
        val constructorPattern = """constructor\(value:\s+$pojoPackage\.$entityClassName\?\):\s+this\(\)\s+\{
(?:\s+if\s+\(value\s+!=\s+null\)\s+\{
(?:.*?)
\s+\}\s*
\})""".toRegex(RegexOption.DOT_MATCHES_ALL)

        val matchResult = constructorPattern.find(content)
        if (matchResult == null) {
            println("Could not find expected constructor pattern in ${recordFile.name}")
            return
        }

        // Extract the body of the constructor
        val constructorBody = matchResult.groupValues[0]

        // Create the new constructor with the Existing sealed class
        val newConstructor = transforms.fold(constructorBody) { acc, transform ->
            transform(acc)
        }

        // Replace the constructor in the file content
        val updatedContent = content.replace(constructorBody, newConstructor)

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