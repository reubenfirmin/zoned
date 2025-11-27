package zoned.framework.ui.components

import kotlinx.html.*

open class Card(classes: List<String>, hover: Boolean, consumer: TagConsumer<*>): DIV(mapOf("class" to "block p-6 bg-white border " +
        "border-gray-200 rounded-lg shadow dark:bg-gray-800 dark:border-gray-700 " +
        (if (hover) "hover:bg-gray-100 dark:hover:bg-gray-700 " else "") +
        classes.joinToString(" ")),
    consumer) {

    open fun render(block: Card.() -> Unit) {
        block()
    }
}

fun FlowContent.card(classes: List<String> = listOf(), hover: Boolean = false, block: Card.() -> Unit = {}) {
    Card(classes, hover, consumer).visit {
        render(block)
    }
}