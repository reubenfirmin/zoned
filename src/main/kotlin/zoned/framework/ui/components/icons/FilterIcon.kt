package zoned.framework.ui.components.icons

import zoned.framework.ui.components.icons.IconAttributes.filled
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit

class FilterIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, true, filled(), consumer=consumer) {

    override fun paths() = listOf(
        "M3 3a1 1 0 011-1h12a1 1 0 011 1v3a1 1 0 01-.293.707L12 11.414V15a1 1 0 01-.293.707l-2 2A1 " +
            "1 0 018 17v-5.586L3.293 6.707A1 1 0 013 6V3z")
}

fun FlowContent.filterIcon(classes: String) {
    FilterIcon(classes, consumer).visit {
        render()
    }
}