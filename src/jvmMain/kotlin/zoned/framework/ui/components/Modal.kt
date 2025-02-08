package zoned.framework.ui.components

import kotlinx.html.*

class Modal(private val modalId: String, consumer: TagConsumer<*>):
    DIV(mapOf("class" to "hidden overflow-y-auto overflow-x-hidden fixed top-0 right-0 left-0 z-50 justify-center " +
            "items-center w-full md:inset-0 h-[calc(100%-1rem)] max-h-full"), consumer) {

    fun render(block: FlowContent.() -> Unit) {
        id = modalId
        tabIndex = "-1"
        attributes["aria-hidden"] = "true"
        attributes["data-modal-backdrop"] = "static"
        div("relative p-4 w-full max-w-2xl max-h-full") {
            div("relative bg-white rounded-lg shadow dark:bg-gray-700") {
                block()
            }
        }
    }
}

fun FlowContent.modal(modalId: String, block: FlowContent.() -> Unit) = Modal(modalId, consumer).visit {
    render(block)
}