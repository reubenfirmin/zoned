package zoned.gradle

import org.example.zoned.gradle.DatabaseCleaner
import org.example.zoned.gradle.DatabaseMigrator
import org.example.zoned.gradle.JooqGenerator
import org.gradle.api.Plugin
import org.gradle.api.Project

class ZonedPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register("db-clean", DatabaseCleaner::class.java)
        project.tasks.register("db-migrate", DatabaseMigrator::class.java)
        project.tasks.register("model-generate", JooqGenerator::class.java)
    }
}
