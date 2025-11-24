package zoned.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import zoned.gradle.bundle.BundleConfigGenerator
import zoned.gradle.bundle.JsBundleConfigurator
import zoned.gradle.db.DatabaseCleaner
import zoned.gradle.db.DatabaseMigrator
import zoned.gradle.generation.JooqGenerator
import zoned.gradle.style.BuildStyleTask

class ZonedPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.tasks.register("db-clean", DatabaseCleaner::class.java)
        project.tasks.register("db-migrate", DatabaseMigrator::class.java)
        project.tasks.register("model-generate", JooqGenerator::class.java)
        project.tasks.register("build-style", BuildStyleTask::class.java)

        // Register bundle config generator task
        val bundleConfigTask = project.tasks.register("generate-bundle-config", BundleConfigGenerator::class.java)

        // Make sure bundle config is generated before Kotlin compilation
        project.afterEvaluate {
            project.tasks.findByName("compileKotlinJvm")?.dependsOn(bundleConfigTask)
        }

        // Auto-configure JS bundle output filename
        JsBundleConfigurator.configure(project)

        // Create task to write bundle properties directly to processedResources
        val writeBundlePropsTask = project.tasks.register("writeBundleProperties") {
            it.doLast {
                val bundleName = "${project.name}.bundle.js"
                project.logger.lifecycle("Writing bundle config: $bundleName")

                // Write to processedResources which will end up in the JAR
                val resourcesDir = project.file("${project.buildDir}/processedResources/jvm/main")
                resourcesDir.mkdirs()
                val propsFile = project.file("${project.buildDir}/processedResources/jvm/main/zoned-bundle.properties")
                propsFile.writeText("bundle.name=$bundleName\nbundle.path=/static/$bundleName\n")
            }
        }

        // Make jvmMainClasses depend on writeBundleProperties so it runs before the JAR is created
        project.afterEvaluate {
            project.tasks.findByName("jvmMainClasses")?.dependsOn(writeBundlePropsTask)
        }

        // Add generated source directory to Kotlin sourcesets
        project.afterEvaluate {
            try {
                val kotlin = project.extensions.findByName("kotlin")
                if (kotlin != null) {
                    val sourceSetsMethod = kotlin.javaClass.getMethod("sourceSets")
                    val sourceSets = sourceSetsMethod.invoke(kotlin)
                    val getByNameMethod = sourceSets.javaClass.getMethod("getByName", String::class.java)
                    val jvmMain = getByNameMethod.invoke(sourceSets, "jvmMain")
                    val kotlinMethod = jvmMain.javaClass.getMethod("getKotlin")
                    val kotlinSourceSet = kotlinMethod.invoke(jvmMain)
                    val srcDirMethod = kotlinSourceSet.javaClass.getMethod("srcDir", Any::class.java)
                    srcDirMethod.invoke(kotlinSourceSet, "${project.buildDir}/generated/kotlin")
                }
            } catch (e: Exception) {
                project.logger.warn("Could not add generated sources to Kotlin sourceset: ${e.message}")
            }
        }

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
