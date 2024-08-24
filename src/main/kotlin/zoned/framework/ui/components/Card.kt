package zoned.framework.ui.components

import kotlinx.html.*

open class Card(classes: List<String>, consumer: TagConsumer<*>): DIV(mapOf("class" to "block p-6 bg-white border " +
        "border-gray-200 rounded-lg shadow hover:bg-gray-100 dark:bg-gray-800 dark:border-gray-700 "
        + classes.joinToString(" ")),
    consumer) {

    open fun render(block: Card.() -> Unit) {
        block()
    }
}

// XXX inconsistent to take classes as list
fun FlowContent.card(classes: List<String> = listOf(), block: Card.() -> Unit = {}) {
    Card(classes, consumer).visit {
        render(block)
    }
}