package zoned.framework.ui.components.kanban

import kotlinx.html.*

class Column(val heading: String, val elementId: String, consumer : TagConsumer<*>):
    DIV(mapOf("class" to "bg-gray-200 dark:bg-gray-900 h-full p-2 space-y-5 min-w-kanban"), consumer) {

    fun render(block : Column.() -> Unit = {}) {
        id = elementId
        // TODO was h1Style
        h1() {
            +this@Column.heading
        }
        block()
    }
}

fun Board.column(heading: String, id: String, block : Column.() -> Unit = {}) = Column(heading, id, consumer).visit {
    render(block)
}
