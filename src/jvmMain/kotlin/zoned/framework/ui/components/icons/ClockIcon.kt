package zoned.framework.ui.components.icons

import zoned.framework.ui.components.icons.IconAttributes.filled
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit

class ClockIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, true, filled(), consumer=consumer) {

    override fun paths() = listOf("M10 18a8 8 0 100-16 8 8 0 000 16zm1-12a1 1 0 10-2 0v4a1 1 0 00.293.707l2.828 " +
            "2.829a1 1 0 101.415-1.415L11 9.586V6z")
}

fun FlowContent.clockIcon(classes: String) {

    ClockIcon(classes, consumer).visit {
        render()
    }
}