package zoned.framework.ui.components

import kotlinx.html.FlowContent
import kotlinx.html.*

object SidebarWidth {
    // need to be string literals so that tailwind grabs them
    val width = "w-80"
    val margin = "ml-80"
}

open class Sidebar(side: Side, absolute: Boolean, consumer: TagConsumer<*>): ASIDE(mapOf(
    "class" to "top-30 z-40 ${SidebarWidth.width} h-full transition-transform -translate-x-full sm:translate-x-0 " +
        if (side == Side.LEFT) {
            "left-0"
        } else {
            "right-0"
        } +
        if (absolute) {
            " absolute"
        } else {
            ""
        }
    ), consumer) {

    fun render(block: Sidebar.() -> Unit) {
        attributes["aria-label"] = "Sidebar"
        id = "default-sidebar"
        div("h-full px-3 py-4 overflow-y-auto bg-gray-50 dark:bg-gray-800") {
            this@Sidebar.block()
        }
    }
}

enum class Side {
    LEFT,
    RIGHT
}

fun FlowContent.sidebar(side: Side, absolute: Boolean, block: Sidebar.() -> Unit) {
    Sidebar(side, absolute, consumer).visit {
        render(block)
    }
}
