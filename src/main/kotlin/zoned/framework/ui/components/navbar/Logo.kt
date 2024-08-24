package zoned.framework.ui.components.navbar

import kotlinx.html.*
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.libs.onClick

class Logo(val action: HTMXAction, consumer : TagConsumer<*>): A(mapOf("class" to "bg-white border-gray-200 dark:bg-gray-900"), consumer) {

    fun render() {
        onClick(action)
        href = "#"
        img(classes = "h-8 mr-3 inline") {
            src = "https://flowbite.com/docs/images/logo.svg"
            alt = "Interview Bot"
        }
        span("self-center text-2xl font-semibold whitespace-nowrap dark:text-white") { +"Interview Bot" }
    }
}

fun Navbar.logo(action: HTMXAction) = Logo(action, consumer).visit {
    render()
}
