package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class DeleteIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, false, outlined(), 24 to 24, consumer=consumer) {

    override fun paths() = listOf("m15 9-6 6m0-6 6 6m6-3a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z")
}

fun FlowContent.deleteIcon(classes: String) = DeleteIcon(classes, consumer).visit { render() }