package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class CheckIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, false, outlined(), 24 to 24, consumer=consumer) {

    override fun paths() = listOf("M8.5 11.5 11 14l4-4m6 2a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z")

}

fun FlowContent.checkIcon(classes: String) = CheckIcon(classes, consumer).visit { render() }