package zoned.framework.ui.components

import kotlinx.html.*
import zoned.framework.ui.components.buttons.ButtonAction
import zoned.framework.ui.components.buttons.ibutton
import zoned.framework.ui.components.icons.xIcon

class Banner(val elementId: String, val bannerText: String, val ctaText: String, val action: ButtonAction, consumer: TagConsumer<*>): DIV(
    mapOf("class" to "fixed z-50 flex flex-col md:flex-row justify-between " +
        "w-[calc(100%-2rem)] p-4 -translate-x-1/2 bg-white border border-gray-100 rounded-lg shadow-sm lg:max-w-7xl " +
        "left-1/2 top-20 dark:bg-gray-700 dark:border-gray-600"), consumer) {

    fun render() {
        id = elementId
        tabIndex = "-1"
        div("flex flex-col items-start mb-3 me-4 md:items-center md:flex-row md:mb-0") {
            p("flex items-center text-md font-normal text-gray-700 dark:text-gray-200") {
                +this@Banner.bannerText
            }
        }
        div("flex items-center flex-shrink-0") {
            ibutton(zoned.framework.ui.components.buttons.ButtonType.PRIMARY, this@Banner.ctaText, this@Banner.action)
            // TODO convert to iButton?
            button(classes = "flex-shrink-0 inline-flex justify-center w-7 h-7 items-center text-gray-400 hover:bg-gray-200 hover:text-gray-900 " +
                    "rounded-lg text-sm p-1.5 dark:hover:bg-gray-600 dark:hover:text-white") {
                attributes["data-dismiss-target"] = "#${this@Banner.elementId}"
                type = ButtonType.button
                xIcon("w-3 h-3")
                span("sr-only") {
                    +"Close banner"
                }
            }
        }
    }
}

fun FlowContent.banner(elementId: String, bannerText: String, ctaText: String, action: ButtonAction) =
    Banner(elementId, bannerText, ctaText, action, consumer).visit {
        render()
    }
