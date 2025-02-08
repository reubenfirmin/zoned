package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class XIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, false, outlined(), consumer=consumer) {

    override fun paths() = listOf("m1 1 6 6m0 0 6 6M7 7l6-6M7 7l-6 6")
}

fun FlowContent.xIcon(classes: String) = XIcon(classes, consumer).visit {
    render()
}