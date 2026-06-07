package zoned.framework.ui.components.icons

import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.visit
import zoned.framework.ui.components.icons.IconAttributes.filled
import zoned.framework.ui.components.icons.IconAttributes.outlined

/**
 * Bell icon for notifications.
 * Supports both outlined (stroke) and filled variants.
 *
 * @param filled When true, renders as a solid bell. When false, renders as an outline.
 */
class BellIcon(classes: String, isFilled: Boolean, consumer: TagConsumer<*>): Icon(
    classes,
    isFilled,
    if (isFilled) filled() else outlined(),
    24 to 24,
    consumer = consumer
) {

    override fun paths(): List<String> {
        return if (filled) {
            // Heroicons bell solid (single fill-rule:evenodd path)
            listOf("M5.25 9a6.75 6.75 0 0 1 13.5 0v.75c0 2.123.8 4.057 2.118 5.52a.75.75 0 0 1-.297 1.206c-1.544.57-3.16.99-4.831 1.243a3.75 3.75 0 1 1-7.48 0 24.585 24.585 0 0 1-4.831-1.244.75.75 0 0 1-.298-1.205A8.217 8.217 0 0 0 5.25 9.75V9Z")
        } else {
            // Heroicons bell outline
            listOf("M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0")
        }
    }
}

/**
 * Render a bell icon.
 *
 * @param classes CSS classes
 * @param filled When true, renders as a solid bell. When false (default), renders as an outline.
 */
fun FlowContent.bellIcon(classes: String = "w-5 h-5", filled: Boolean = false) =
    BellIcon(classes, filled, consumer).visit { render() }
