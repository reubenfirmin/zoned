package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.outlined

class ShareIcon(classes: String, consumer: TagConsumer<*>):
    Icon(classes, false, outlined(), 24 to 24, null, consumer) {

    override fun paths() = listOf("M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z")
}

fun FlowContent.shareIcon(classes: String) = ShareIcon(classes, consumer).visit {
    render()
}
