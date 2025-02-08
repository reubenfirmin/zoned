package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.filled

class EmailIcon(classes: String, consumer: TagConsumer<*>): Icon(classes, true, filled(), consumer=consumer) {
    override fun paths() = listOf(
        "m10.036 8.278 9.258-7.79A1.979 1.979 0 0 0 18 0H2A1.987 1.987 0 0 0 .641.541l9.395 7.737Z",
        "M11.241 9.817c-.36.275-.801.425-1.255.427-.428 0-.845-.138-1.187-.395L0 2.6V14a2 2 0 0 0 2 2h16a2 " +
                "2 0 0 0 2-2V2.5l-8.759 7.317Z")
}

fun FlowContent.emailIcon(classes: String) = EmailIcon(classes, consumer).visit {
    render()
}