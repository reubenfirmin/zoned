package zoned.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import zoned.gradle.DatabaseSetup

open class DatabaseMigrator : DefaultTask() {

    @TaskAction
    fun migrate() {
        val setup = DatabaseSetup(logger)
        // TODO support snapshot and rollback
        logger.lifecycle("MIGRATING")


        // Log the actual path being used
        logger.warn("Migration path: ${project.rootDir.absolutePath}/src/jvmMain/resources/db/migration")
        // Log if the directory exists and list its contents
        val migrationDir = java.io.File("${project.rootDir.absolutePath}/src/jvmMain/resources/db/migration")
        logger.warn("Directory exists: ${migrationDir.exists()}")
        logger.warn("Files in directory: ${migrationDir.listFiles()?.joinToString { it.name } ?: "none"}")

        val flyway = setup.getFlyway(project, true)
        val info = flyway.validateWithResult()
        logger.warn(info.allErrorMessages)
        val result = flyway.migrate()
        logger.warn("Migrations executed: ${result.migrationsExecuted}")
        logger.warn("Success: ${result.success}")
    }
}