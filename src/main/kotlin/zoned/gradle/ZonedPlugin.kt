package zoned.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider

class ZonedPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Register the DatabaseSetup task
        val databaseSetupTask: TaskProvider<DatabaseSetup> = project.tasks.register("databaseSetup", DatabaseSetup::class.java)

        // Register individual tasks for clean, migrate, and jooqGenerate
        project.tasks.register("db-clean") { task ->
            task.dependsOn(databaseSetupTask)
            task.doLast {
                databaseSetupTask.get().clean()
            }
        }

        project.tasks.register("db-migrate") { task ->
            task.dependsOn(databaseSetupTask)
            task.doLast {
                val databaseSetup = databaseSetupTask.get()
                databaseSetup.migrate()
                databaseSetup.jooqGenerate()
            }
        }
    }
}