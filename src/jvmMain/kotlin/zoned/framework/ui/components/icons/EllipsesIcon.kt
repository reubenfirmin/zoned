package zoned.framework.ui.components.icons

import zoned.framework.ui.components.icons.IconAttributes.filled
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit

class EllipsesIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, true, filled(), consumer=consumer) {

    override fun paths() = listOf("M6 10a2 2 0 11-4 0 2 2 0 014 0zM12 10a2 2 0 11-4 0 2 2 0 014 0zM16 12a2 2 0 100-4 " +
            "2 2 0 000 4z")
}

fun FlowContent.ellipsesIcon(classes: String) {
    EllipsesIcon(classes, consumer).visit {
        render()
    }
}