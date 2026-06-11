package zoned.framework.ui.enhancements

import kotlinx.browser.window
import kotlinx.css.*
import kotlinx.css.properties.BoxShadow
import kotlinx.css.properties.Timing
import kotlinx.css.properties.Transition
import kotlinx.css.properties.ms
import kotlinx.html.TagConsumer
import kotlinx.html.div
import web.dom.Node
import web.dom.document
import web.html.HTMLElement
import zoned.framework.dom.Ref
import zoned.framework.dom.insertChildren
import zoned.framework.dom.onMouseEnter
import zoned.framework.dom.onMouseLeave
import web.mouse.MouseEvent
import zoned.framework.dom.ref
import zoned.framework.interop.css

/**
 * Client-side implementation of the Tooltip enhancement.
 *
 * Wraps trigger content and shows tooltip on hover.
 * Tooltip text comes from server-rendered config.
 *
 * Usage (server-side DSL):
 * ```kotlin
 * tooltip({ text = "Helpful information" }) {
 *     span { +"Hover me" }
 * }
 * ```
 */
@EnhancementImpl(TooltipEnhancement::class)
fun TagConsumer<HTMLElement>.initTooltipEnhancement(config: TooltipConfig, children: List<Node>) {
    val triggerRef = Ref<HTMLElement>()

    // Wrapper div - the hover trigger area
    div {
        ref(triggerRef)
        css {
            display = Display.inlineBlock
        }

        // Re-insert server-rendered trigger content
        insertChildren(children)

        // Show tooltip on hover
        onMouseEnter { _: MouseEvent ->
            TooltipManager.show(triggerRef.element, config.text, config.position)
        }

        onMouseLeave { _: MouseEvent ->
            TooltipManager.hide()
        }
    }
}

/**
 * Singleton manager for tooltip display.
 * Uses a single tooltip element appended to body for proper z-index/overflow handling.
 * Content is always from server-rendered config - frontend just displays it.
 */
private object TooltipManager {
    private var tooltipElement: HTMLElement? = null

    private fun getOrCreateTooltip(): HTMLElement {
        // A full-page render can clear <body>, detaching the cached element — recreate if so.
        tooltipElement?.takeIf { it.isConnected }?.let { return it }

        // Create tooltip element directly (not via Ref which has async binding)
        val tip = document.createElement("div") as HTMLElement
        tip.css {
            position = Position.absolute
            backgroundColor = Color("rgba(17, 24, 39, 0.95)")
            color = Color("#e5e7eb")
            padding = Padding(4.px, 8.px)
            borderRadius = 4.px
            fontSize = 12.px
            fontFamily = "system-ui, sans-serif"
            whiteSpace = WhiteSpace.nowrap
            zIndex = 10000
            pointerEvents = PointerEvents.none
            opacity = 0
            transition += Transition("opacity", 150.ms, Timing.easeInOut)
            border = Border(1.px, BorderStyle.solid, Color("rgba(75, 85, 99, 0.5)"))
            boxShadow += BoxShadow(Color("rgba(0, 0, 0, 0.3)"), 0.px, 2.px, 8.px)
        }
        document.body?.appendChild(tip)
        tooltipElement = tip
        return tip
    }

    fun show(trigger: HTMLElement, text: String, position: String) {
        val tip = getOrCreateTooltip()
        tip.textContent = text
        tip.style.opacity = "1"

        // Position after making visible so we can measure. The trigger can be swapped out of the
        // DOM between mouseenter and this frame — a detached rect is all zeros, which would pin
        // the tooltip to the viewport corner.
        window.requestAnimationFrame {
            if (trigger.isConnected) positionTooltip(tip, trigger, position) else hide()
        }
    }

    fun hide() {
        tooltipElement?.style?.opacity = "0"
    }

    private fun positionTooltip(tip: HTMLElement, trigger: HTMLElement, position: String) {
        val rect = trigger.getBoundingClientRect()
        val tipRect = tip.getBoundingClientRect()
        val scrollX = window.scrollX
        val scrollY = window.scrollY

        val (x, y) = when (position) {
            "bottom" -> Pair(
                scrollX + rect.left + (rect.width - tipRect.width) / 2,
                scrollY + rect.bottom + 6
            )
            "left" -> Pair(
                scrollX + rect.left - tipRect.width - 6,
                scrollY + rect.top + (rect.height - tipRect.height) / 2
            )
            "right" -> Pair(
                scrollX + rect.right + 6,
                scrollY + rect.top + (rect.height - tipRect.height) / 2
            )
            else -> Pair( // "top" is default
                scrollX + rect.left + (rect.width - tipRect.width) / 2,
                scrollY + rect.top - tipRect.height - 6
            )
        }

        tip.style.left = "${x}px"
        tip.style.top = "${y}px"
    }
}
