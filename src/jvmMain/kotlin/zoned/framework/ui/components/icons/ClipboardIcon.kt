package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class ClipboardIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, false, outlined(), 24 to 24, consumer=consumer) {

    override fun paths() = listOf(
        "M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h12a2 2 0 002-2V7a2 2 0 00-2-2h-2M8 5a2 2 0 002 2h4a2 2 0 002-2M8 5a2 2 0 012-2h4a2 2 0 012 2"
    )

}

fun FlowContent.clipboardIcon(classes: String) = ClipboardIcon(classes, consumer).visit { render() }
