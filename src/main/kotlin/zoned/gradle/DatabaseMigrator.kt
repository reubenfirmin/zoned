package org.example.zoned.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import zoned.gradle.DatabaseSetup

open class DatabaseMigrator : DefaultTask() {

    @TaskAction
    fun migrate() {
        val setup = DatabaseSetup()
        // TODO support snapshot and rollback
        println("MIGRATING")
        setup.getFlyway(project, true).migrate()
    }
}