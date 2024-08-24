package zoned.framework.ui.components.kanban

import kotlinx.html.*

class Board(val heading: String?, consumer: TagConsumer<*>):
    DIV(mapOf("class" to "overflow-y-auto w-full h-full bg-gray-50 dark:bg-gray-900"), consumer) {

    fun render(block : Board.() -> Unit = {}) {
        if (heading != null) {
            h2(classes = "text-2xl dark:text-white") {
                +this@Board.heading
            }
        }

        div(classes = "flex justify-stretch items-start space-x-2 h-full") {
            // render any children
            this@Board.block()
        }
    }
}

fun FlowContent.board(heading: String? = null, block : Board.() -> Unit = {}) = Board(heading, consumer).visit {
    render(block)
}
