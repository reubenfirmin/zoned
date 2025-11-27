package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class CheckCircleIcon(classes: String, consumer: TagConsumer<*>):
    Icon(classes, false, outlined(), 24 to 24, null, consumer) {

    override fun paths() = listOf("M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z")
}

fun FlowContent.checkCircleIcon(classes: String) = CheckCircleIcon(classes, consumer).visit {
    render()
}
