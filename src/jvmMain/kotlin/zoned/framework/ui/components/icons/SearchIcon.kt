package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class SearchIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, false, outlined(), consumer=consumer) {
    override fun paths() = listOf("m19 19-4-4m0-7A7 7 0 1 1 1 8a7 7 0 0 1 14 0Z")
}

fun FlowContent.searchIcon(classes: String) {
    SearchIcon(classes, consumer).visit {
        render()
    }
}