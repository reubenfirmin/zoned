package zoned.framework.ui.components.icons

import zoned.framework.ui.components.icons.IconAttributes.filled
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit

class DropdownIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, true, filled(), consumer=consumer) {

    override fun paths() = listOf("M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 " +
            "01-1.414 0l-4-4a1 1 0 010-1.414z")
}

fun FlowContent.dropdownIcon(classes: String) {
    DropdownIcon(classes, consumer).visit {
        render()
    }
}