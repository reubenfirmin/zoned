package zoned.framework.ui.libs

import java.util.Properties

/**
 * Bundle configuration loaded from zoned-bundle.properties
 * The gradle plugin writes this file with the bundle name based on project name
 */
object BundleConfig {
    private val props = Properties().apply {
        val stream = BundleConfig::class.java.classLoader.getResourceAsStream("zoned-bundle.properties")
        if (stream != null) {
            load(stream)
            println("✓ Loaded bundle config from zoned-bundle.properties")
        } else {
            println("✗ Could not find zoned-bundle.properties, using defaults")
        }
    }

    val BUNDLE_NAME: String = props.getProperty("bundle.name", "main.bundle.js").also {
        println("Bundle name: $it")
    }
    val BUNDLE_PATH: String = props.getProperty("bundle.path", "/static/main.bundle.js")
}
