package zoned.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import zoned.gradle.DatabaseSetup

open class DatabaseCleaner : DefaultTask() {

    @TaskAction
    fun clean() {
        val setup = DatabaseSetup(logger)
        println("WARNING - cleaning")
        setup.getFlyway(project, false, "").clean()
    }

}