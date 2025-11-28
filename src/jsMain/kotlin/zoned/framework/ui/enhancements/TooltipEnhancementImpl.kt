package zoned.framework.ui.enhancements

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Node
import web.dom.Element
import web.html.HTMLElement

/**
 * Client-side implementation of the Tooltip enhancement.
 * Creates an instant, styled tooltip on hover.
 */
fun makeTooltipEnhancement(element: Element, config: TooltipConfig) {
    val htmlElement = element as HTMLElement
    var tooltipEl: HTMLElement? = null

    // Create tooltip element lazily on first hover
    fun createTooltip(): HTMLElement {
        val tip = document.createElement("div") as HTMLElement
        tip.textContent = config.text
        tip.style.apply {
            position = "absolute"
            backgroundColor = "rgba(17, 24, 39, 0.95)"  // gray-900 with slight transparency
            color = "#e5e7eb"  // gray-200
            padding = "4px 8px"
            borderRadius = "4px"
            fontSize = "12px"
            fontFamily = "system-ui, sans-serif"
            whiteSpace = "nowrap"
            zIndex = "10000"
            pointerEvents = "none"
            opacity = "0"
            transition = "opacity 0.15s ease-in-out"
            border = "1px solid rgba(75, 85, 99, 0.5)"  // gray-600
            boxShadow = "0 2px 8px rgba(0, 0, 0, 0.3)"
        }
        document.body?.appendChild(tip as Node)
        return tip
    }

    fun positionTooltip(tip: HTMLElement) {
        val rect = htmlElement.getBoundingClientRect()
        val tipRect = tip.getBoundingClientRect()
        val scrollX = window.scrollX
        val scrollY = window.scrollY

        when (config.position) {
            "bottom" -> {
                tip.style.left = "${scrollX + rect.left + (rect.width - tipRect.width) / 2}px"
                tip.style.top = "${scrollY + rect.bottom + 6}px"
            }
            "left" -> {
                tip.style.left = "${scrollX + rect.left - tipRect.width - 6}px"
                tip.style.top = "${scrollY + rect.top + (rect.height - tipRect.height) / 2}px"
            }
            "right" -> {
                tip.style.left = "${scrollX + rect.right + 6}px"
                tip.style.top = "${scrollY + rect.top + (rect.height - tipRect.height) / 2}px"
            }
            else -> { // "top" is default
                tip.style.left = "${scrollX + rect.left + (rect.width - tipRect.width) / 2}px"
                tip.style.top = "${scrollY + rect.top - tipRect.height - 6}px"
            }
        }
    }

    htmlElement.onMouseEnter { _ ->
        if (tooltipEl == null) {
            tooltipEl = createTooltip()
        }
        tooltipEl?.let { tip ->
            tip.style.opacity = "1"
            // Position after making visible so we can measure
            positionTooltip(tip)
        }
    }

    htmlElement.onMouseLeave { _ ->
        tooltipEl?.style?.opacity = "0"
    }
}
