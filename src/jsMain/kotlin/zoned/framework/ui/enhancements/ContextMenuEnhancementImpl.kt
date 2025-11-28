package zoned.framework.ui.enhancements

import kotlinx.css.*
import kotlinx.html.TagConsumer
import kotlinx.html.div
import web.dom.Element
import web.dom.Node
import web.dom.document
import web.html.HTMLElement
import web.events.EventType
import web.events.addEventListener
import web.events.removeEventListener
import web.uievents.KeyboardEvent
import web.uievents.MouseEvent
import zoned.framework.dom.Ref
import zoned.framework.dom.insertChildren
import zoned.framework.dom.onContextMenu
import zoned.framework.dom.onDestroy
import zoned.framework.dom.onMount
import zoned.framework.dom.ref
import zoned.framework.interop.addToBody
import zoned.framework.interop.css
import zoned.framework.interop.encodeURIComponent
import zoned.framework.libs.HTMXHelper

private var menuIdCounter = 0

/**
 * Client-side implementation of the ContextMenu enhancement.
 *
 * Works as a wrapper - listens for right-click on any descendant and finds
 * the nearest element with the configured data attributes.
 *
 * Uses TagConsumer pattern: captures server-rendered children and rebuilds with DSL.
 *
 * Usage (server-side DSL):
 * ```kotlin
 * contextMenu({ menuUrl = "...", dataAttributes = listOf("propertyId") }) {
 *     table {
 *         tbody {
 *             items.forEach { item ->
 *                 tr {
 *                     attributes["data-propertyId"] = item.id
 *                     // row content
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 */
@EnhancementImpl(ContextMenuEnhancement::class)
fun TagConsumer<HTMLElement>.initContextMenuEnhancement(config: ContextMenuConfig, children: List<Node>) {
    val containerRef = Ref<HTMLElement>()

    // Use server's menu target if provided, otherwise create our own
    val useServerMenu = config.menuTarget.isNotBlank()
    val menuTarget = if (useServerMenu) config.menuTarget else "#context-menu-${menuIdCounter++}"
    val menuRef = Ref<HTMLElement>()

    // Store handler references for cleanup on destroy
    var clickHandler: ((MouseEvent) -> Unit)? = null
    var keyHandler: ((KeyboardEvent) -> Unit)? = null

    div {
        ref(containerRef)

        // Re-insert server-rendered children (the content that triggers context menu)
        insertChildren(children)

        onContextMenu { event ->
            event.preventDefault()
            val container = containerRef.element

            // Find menu element (either server-provided or our own)
            val menu = if (useServerMenu) {
                document.querySelector(config.menuTarget) as? HTMLElement
            } else {
                menuRef.getOrNull()
            } ?: return@onContextMenu

            // Find the actual target element with data attributes
            val targetElement = findElementWithDataAttributes(event.target as? Element, config.dataAttributes)

            // Look for parent with data-selected-ids (from SelectableTable enhancement)
            var selectedIds: String? = null
            var parent: Element? = container.parentElement
            while (parent != null) {
                val ids = parent.getAttribute("data-selected-ids")
                if (ids != null && ids.isNotEmpty()) {
                    selectedIds = ids
                    break
                }
                parent = parent.parentElement
            }

            // Build URL with data attributes as query params
            val url = buildString {
                append(config.menuUrl)
                val params = mutableListOf<String>()

                // Add data attributes from the target element
                if (targetElement != null) {
                    config.dataAttributes.forEach { attr ->
                        targetElement.getAttribute("data-$attr")?.let { value ->
                            params.add("${encodeURIComponent(attr)}=${encodeURIComponent(value)}")
                        }
                    }
                }

                // Add selectedIds if found in parent
                if (selectedIds != null) {
                    params.add("selectedIds=${encodeURIComponent(selectedIds)}")
                }

                if (params.isNotEmpty()) {
                    append(if ("?" in config.menuUrl) "&" else "?")
                    append(params.joinToString("&"))
                }
            }

            // Show menu at click position
            showMenu(menu, event.clientX, event.clientY)

            // Fetch menu content via HTMX (use server's target or our own)
            HTMXHelper.get(url, menuTarget)
        }

        onMount {
            // Create handlers and store references for cleanup
            clickHandler = { _: MouseEvent ->
                val menu = if (useServerMenu) {
                    document.querySelector(config.menuTarget) as? HTMLElement
                } else {
                    menuRef.getOrNull()
                }
                menu?.let { hideMenu(it) }
            }
            keyHandler = { event: KeyboardEvent ->
                if (event.key == "Escape") {
                    val menu = if (useServerMenu) {
                        document.querySelector(config.menuTarget) as? HTMLElement
                    } else {
                        menuRef.getOrNull()
                    }
                    menu?.let { hideMenu(it) }
                }
            }
            // Add to document with stored references (so we can remove them later)
            document.addEventListener(EventType("click"), clickHandler!!)
            document.addEventListener(EventType("keydown"), keyHandler!!)
        }

        onDestroy {
            // Only remove menu element if we created it (not server's)
            if (!useServerMenu) {
                menuRef.getOrNull()?.remove()
            }
            // Remove document handlers to prevent accumulation
            clickHandler?.let { document.removeEventListener(EventType("click"), it) }
            keyHandler?.let { document.removeEventListener(EventType("keydown"), it) }
        }
    }

    // Only create our own menu container if server didn't provide one
    if (!useServerMenu) {
        addToBody {
            div {
                // Set ID FIRST - must be before ref() so ref uses this ID for behavior lookup
                attributes["id"] = menuTarget.removePrefix("#")
                attributes["class"] = "hidden"
                ref(menuRef)
                css {
                    position = Position.fixed
                    zIndex = 9999
                    backgroundColor = Color("#1f2937")
                    border = Border(1.px, BorderStyle.solid, Color("#374151"))
                    borderRadius = 4.px
                    minWidth = 160.px
                }
            }
        }
    }
}

/**
 * Walk up from element to find the nearest ancestor (or self) that has
 * at least one of the specified data attributes.
 */
private fun findElementWithDataAttributes(start: Element?, dataAttributes: List<String>): Element? {
    var current = start
    while (current != null) {
        val hasAttr = dataAttributes.any { attr ->
            current?.getAttribute("data-$attr") != null
        }
        if (hasAttr) return current
        current = current.parentElement
    }
    return null
}

private fun showMenu(menu: HTMLElement, x: Int, y: Int) {
    menu.css {
        left = x.px
        top = y.px
    }
    menu.classList.remove("hidden")
}

private fun hideMenu(menu: HTMLElement) {
    menu.classList.add("hidden")
}
