package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.filled
import zoned.framework.ui.components.icons.IconAttributes.outlined

class ArrowRightIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, false, outlined(), consumer = consumer) {

    override fun paths() = listOf("M1 5h12m0 0L9 1m4 4L9 9")
}

fun FlowContent.arrowRightIcon(classes: String) {

    ArrowRightIcon(classes, consumer).visit {
        render()
    }
}

