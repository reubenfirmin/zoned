package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

// note we want stroke = "currentColor"
class PlusCircleIcon(classes: String, color: String, consumer: TagConsumer<*>):
    Icon(classes, false, outlined(), 24 to 24, color, consumer) {

    override fun paths() = listOf("M12 7.8v8.4M7.8 12h8.4m4.8 0a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z")
}

// XXX probably doesn't need color anymore
fun FlowContent.plusCircleIcon(classes: String, color: String) = PlusCircleIcon(classes, color, consumer).visit {
    render()
}