package zoned.framework.ui.components.navbar

import zoned.framework.ui.tags.svg
import kotlinx.html.*

class MobileMenu(consumer : TagConsumer<*>):
    BUTTON(mapOf("class" to "inline-flex items-center p-2 w-10 h-10 justify-center text-sm text-gray-500 " +
            "rounded-lg md:hidden hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-200 " +
            "dark:text-gray-400 dark:hover:bg-gray-700 dark:focus:ring-gray-600"), consumer) {

    fun render(menuAttribute: String, block : MobileMenu.() -> Unit) {
        attributes["data-collapse-toggle"] = menuAttribute
        type = ButtonType.button
        attributes["aria-controls"] = menuAttribute
        attributes["aria-expanded"] = "false"
        span("sr-only") { +"""Open main menu""" }
        svg("w-5 h-5") {
            attributes["aria-hidden"] = "true"
            fill = "none"
            viewBox = "0 0 17 14"
            path {
                stroke = "currentColor"
                attributes["stroke-linecap"] = "round"
                attributes["stroke-linejoin"] = "round"
                attributes["stroke-width"] = "2"
                d = "M1 1h15M1 7h15M1 13h15"
            }
        }
        block()
    }
}

fun Navbar.mobileMenu(menuAttribute: String, block : MobileMenu.() -> Unit = {}) = MobileMenu(consumer).visit {
    render(menuAttribute, block)
}
