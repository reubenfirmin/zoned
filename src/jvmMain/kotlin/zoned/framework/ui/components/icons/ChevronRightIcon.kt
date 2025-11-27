package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class ChevronRightIcon(classes: String, consumer: TagConsumer<*>):
    Icon(classes, false, outlined(), 24 to 24, null, consumer) {

    override fun paths() = listOf("M9 5l7 7-7 7")
}

fun FlowContent.chevronRightIcon(classes: String) = ChevronRightIcon(classes, consumer).visit {
    render()
}
