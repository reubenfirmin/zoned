package zoned.framework.ui.components

import kotlinx.html.*

class Spinner(val elementId: String?, classes: String?, consumer: TagConsumer<*>): DIV(mapOf("classes" to (classes ?: "")), consumer) {

    fun FlowContent.render(block: html.() -> Unit) {
        div("fixed z-50 inset-0 overflow-auto bg-black bg-opacity-50 flex flex-col items-center justify-center") {
            block()
            img {
                if (this@Spinner.elementId != null) {
                    this.id = this@Spinner.elementId
                }
                src = "/static/spinner.svg"
            }
        }
    }
}

/**
 * Tip - use the block to include an onLoad that triggers an action.
 * TODO move spinner svg to framework too
 */
fun FlowContent.spinner(elementId: String? = null, classes: String? = null, block: html.() -> Unit) = Spinner(elementId, classes, consumer).visit {
    render(block)
}