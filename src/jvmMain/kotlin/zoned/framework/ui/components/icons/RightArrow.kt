package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class RightArrow(classes: String, color: String? = null, consumer: TagConsumer<*>): Icon(classes, false, outlined(), 24 to 24, color, consumer) {

    override fun paths() = listOf("m16.2 19 4.8-7-4.8-7H3l4.8 7L3 19h13.2Z")
}

fun FlowContent.rightArrowIcon(classes: String, color: String? = null) = RightArrow(classes, color, consumer).visit {
    render()
}