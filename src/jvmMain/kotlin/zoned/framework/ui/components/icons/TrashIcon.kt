package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class TrashIcon(classes: String, consumer: TagConsumer<*>):
    Icon(classes, false, outlined(), 24 to 24, null, consumer) {

    override fun paths() = listOf("M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16")
}

fun FlowContent.trashIcon(classes: String) = TrashIcon(classes, consumer).visit {
    render()
}
