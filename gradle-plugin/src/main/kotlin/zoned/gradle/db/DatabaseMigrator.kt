package zoned.gradle.db

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import zoned.gradle.DatabaseSetup

open class DatabaseMigrator : DefaultTask() {

    @TaskAction
    fun migrate() {
        val paths = setOf(
            "${project.rootDir.absolutePath}/src/jvmMain/resources/db/migration",
            "${project.rootDir.absolutePath}/migration",
            "${project.rootDir.absolutePath}/migrations",
            "${project.rootDir.absolutePath}/postgres/migration",
            "${project.rootDir.absolutePath}/postgres/migrations",
        )

        val setup = DatabaseSetup(logger)
        // TODO support snapshot and rollback
        logger.lifecycle("MIGRATING")


        // Determine the actual path being used
        val chosenPath = paths.firstOrNull { java.io.File(it).exists() }
            ?: throw IllegalStateException("No valid migration directory found in defined paths.")

        logger.warn("Migration path: $chosenPath")
        // Log if the directory exists and list its contents
        val migrationDir = java.io.File(chosenPath)
        logger.warn("Files in directory: ${migrationDir.listFiles()?.joinToString { it.name } ?: "none"}")

        val flyway = setup.getFlyway(project, true, migrationDir.absolutePath)
        val info = flyway.validateWithResult()
        logger.warn(info.allErrorMessages)
        val result = flyway.migrate()
        logger.warn("Migrations executed: ${result.migrationsExecuted}")
        logger.warn("Success: ${result.success}")
    }
}