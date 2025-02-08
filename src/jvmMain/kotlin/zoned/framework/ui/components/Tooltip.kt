package zoned.framework.ui.components

import kotlinx.html.*
import kotlinx.html.TagConsumer

class Tooltip(val elementId: String, consumer: TagConsumer<*>): DIV(mapOf(), consumer) {

    fun render(toolTipBlock: Tooltip.() -> Unit, block: Tooltip.() -> Unit) {
        attributes["data-tooltip-target"] = "${elementId}-tooltip"

        block()

        div("absolute z-10 invisible inline-block px-3 py-2 text-sm font-medium text-white transition-opacity " +
                "duration-300 bg-gray-900 rounded-lg shadow-sm opacity-0 tooltip dark:bg-gray-700") {
            id = "${this@Tooltip.elementId}-tooltip"
            role = "tooltip"
            this@Tooltip.toolTipBlock()
            div("tooltip-arrow") {
                attributes["data-popper-arrow"] = "true"
            }
        }
    }
}

/**
 * Wrap a component in this (i.e. put the component inside the block). Add a block that you want to show in the tooltip.
 * Give it a unique id. You're welcome.
 */
fun FlowContent.tooltip(elementId: String, toolTipBlock: Tooltip.() -> Unit, block: Tooltip.() -> Unit) {

    Tooltip(elementId, consumer).visit {
        render(toolTipBlock, block)
    }
}