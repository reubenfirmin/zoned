package zoned.gradle.bundle

import org.gradle.api.Project

/**
 * Auto-configures JS bundle output filename for Kotlin/JS webpack
 * Client projects no longer need to manually configure outputFileName
 */
object JsBundleConfigurator {

    fun configure(project: Project) {
        val bundleName = "${project.name}.bundle.js"
        project.extensions.extraProperties.set("zonedBundleName", bundleName)
        project.logger.lifecycle("Zoned bundle name: $bundleName")

        // Auto-configure webpack outputFileName after project evaluation
        project.afterEvaluate {
            try {
                val kotlin = project.extensions.findByName("kotlin")
                if (kotlin != null) {
                    // Get the targets collection
                    val targetsMethod = kotlin.javaClass.getMethod("targets")
                    val targets = targetsMethod.invoke(kotlin)

                    // Find the JS target
                    val getByNameMethod = targets.javaClass.getMethod("findByName", String::class.java)
                    val jsTarget = getByNameMethod.invoke(targets, "js")

                    if (jsTarget != null) {
                        // Get browser configuration
                        val browserMethod = jsTarget.javaClass.getMethod("browser")
                        val browser = browserMethod.invoke(jsTarget)

                        // Get commonWebpackConfig
                        val webpackConfigMethod = browser.javaClass.getMethod("commonWebpackConfig", org.gradle.api.Action::class.java)
                        webpackConfigMethod.invoke(browser, org.gradle.api.Action<Any> { config ->
                            try {
                                val setOutputFileNameMethod = config.javaClass.getMethod("setOutputFileName", String::class.java)
                                setOutputFileNameMethod.invoke(config, bundleName)
                                project.logger.lifecycle("Auto-configured webpack outputFileName: $bundleName")
                            } catch (e: Exception) {
                                project.logger.warn("Could not auto-configure webpack outputFileName: ${e.message}")
                            }
                        })
                    }
                }
            } catch (e: Exception) {
                project.logger.warn("Could not auto-configure JS bundle: ${e.message}")
            }
        }
    }
}

/**
 * Extension function to get the zoned bundle name
 */
fun Project.zonedBundleName(): String {
    return "${project.name}.bundle.js"
}
