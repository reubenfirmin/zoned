package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class FolderIcon(classes: String, consumer: TagConsumer<*>):
    Icon(classes, false, outlined(), 24 to 24, null, consumer) {

    override fun paths() = listOf("M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z")
}

fun FlowContent.folderIcon(classes: String) = FolderIcon(classes, consumer).visit {
    render()
}
