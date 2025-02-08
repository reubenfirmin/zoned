package zoned.framework.ui.components

import kotlinx.html.*

class TitledCard(val title: String, classes: List<String>, consumer: TagConsumer<*>): Card(classes.plus("max-w-sm"), consumer) {
    override fun render(block: Card.() -> Unit) {
        h5("mb-2 text-2xl font-bold tracking-tight text-gray-900 dark:text-white") {
            +this@TitledCard.title
        }
        p("font-normal text-gray-700 dark:text-gray-400") {
            this@TitledCard.block()
        }
    }
}

fun FlowContent.titledCard(title: String, classes: List<String> = listOf(), block: Card.() -> Unit = {}) {
    TitledCard(title,classes,  consumer).visit {
        render(block)
    }
}