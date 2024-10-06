package zoned.framework.ui.components.navbar

import kotlinx.html.*
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.components.html
import zoned.framework.ui.libs.onClick

class Logo(val action: HTMXAction, val text: String, val image: html.() -> Unit, consumer : TagConsumer<*>): A(mapOf("class" to
        "bg-white border-gray-200 dark:bg-gray-900"), consumer) {

    fun render() {
        onClick(action)
        href = "#"
        image()
        span("self-center text-2xl font-semibold whitespace-nowrap dark:text-white") {
            +this@Logo.text
        }
    }
}

fun Navbar.logo(action: HTMXAction, text: String, image: html.() -> Unit) = Logo(action, text, image, consumer).visit {
    render()
}
