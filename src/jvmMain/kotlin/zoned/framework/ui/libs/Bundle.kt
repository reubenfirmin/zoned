package zoned.framework.ui.libs;

import kotlinx.html.FlowContent
import kotlinx.html.HEAD
import kotlinx.html.script
import kotlinx.html.unsafe

object Bundle {

    /**
     * This is our js from jsMain! Requires a gradle round trip to get this into place
     */
    fun HEAD.bundleInit() {
        script {
            src="/static/main.bundle.js"
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
