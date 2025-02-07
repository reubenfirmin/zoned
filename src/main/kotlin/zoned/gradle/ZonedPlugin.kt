package zoned.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import zoned.framework.gradle.BuildStyleTask

class ZonedPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register("db-clean", DatabaseCleaner::class.java)
        project.tasks.register("db-migrate", DatabaseMigrator::class.java)
        project.tasks.register("model-generate", JooqGenerator::class.java)
        project.tasks.register("build-style", BuildStyleTask::class.java)

        project.tasks.named("build").configure {
            it.finalizedBy("build-style")
        }

        // we get a dupe on the gradle-node-plugin
        project.tasks.withType(Tar::class.java).configureEach { tar ->
            tar.rootSpec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }

        project.tasks.withType(Zip::class.java).configureEach { zip ->
            zip.rootSpec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
