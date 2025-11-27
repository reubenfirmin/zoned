package zoned.framework.ui.libs;

import kotlinx.html.FlowContent
import kotlinx.html.HEAD
import kotlinx.html.script
import kotlinx.html.unsafe
import java.util.Properties

object Bundle {

    private val bundlePath: String by lazy {
        val props = Properties()
        val stream = Bundle::class.java.classLoader.getResourceAsStream("zoned-bundle.properties")
        if (stream != null) {
            props.load(stream)
            props.getProperty("bundle.path") ?: BundleConfig.BUNDLE_PATH
        } else {
            BundleConfig.BUNDLE_PATH
        }
    }

    /**
     * This is our js from jsMain! Requires a gradle round trip to get this into place.
     * The bundle path is automatically generated based on your project name via the zoned gradle plugin.
     */
    fun HEAD.bundleInit() {
        script {
            src = bundlePath
        }
    }

    fun FlowContent.makeSortable(columnIds: List<String>, group: String, dragClass: String, ghostClass: String) {
        script {
            unsafe {
                columnIds.forEach { columnId ->
                    raw("""                                        
                        window.bundle.makeSortable("$columnId", "$group", "$dragClass", "$ghostClass", "/deal-drop");
                    """.trimIndent())
                }
            }
        }
    }
}
