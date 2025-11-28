package zoned.framework.ui.enhancements

import web.dom.Element
import web.dom.document
import web.html.HTMLElement
import zoned.framework.libs.HTMXHelper

/**
 * Client-side implementation of the ContextMenu enhancement.
 * This function is called by the auto-generated registry.
 */
fun makeContextMenuEnhancement(element: Element, config: ContextMenuConfig) {
    val htmlElement = element as HTMLElement

    htmlElement.onContextMenu { event ->
        event.preventDefault()

        // Look for parent with data-selected-ids (from SelectableTable enhancement)
        var selectedIds: String? = null
        var parent: Element? = htmlElement.parentElement
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

            // Add data attributes from the element
            config.dataAttributes.forEach { attr ->
                htmlElement.getAttribute("data-$attr")?.let { value ->
                    params.add("${encodeURIComponent(attr)}=${encodeURIComponent(value)}")
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

        // Clear old content before fetching new menu (prevents showing stale content)
        (document.querySelector(config.menuTarget) as? HTMLElement)?.innerHTML = ""

        // Fetch menu content via HTMX
        HTMXHelper.get(url, config.menuTarget)

        // Position the menu at click location
        positionContextMenu(config.menuTarget, event.clientX, event.clientY)
    }

    // Close menu on click anywhere
    document.onClick { _ ->
        hideContextMenu(config.menuTarget)
    }

    // Close menu on escape key
    document.onKeyDown { event ->
        if (event.key == "Escape") {
            hideContextMenu(config.menuTarget)
        }
    }
}

private fun positionContextMenu(selector: String, x: Int, y: Int) {
    val menu = document.querySelector(selector) as? HTMLElement ?: return
    menu.style.position = "fixed"
    menu.style.left = "${x}px"
    menu.style.top = "${y}px"
    menu.style.display = "block"
    menu.style.zIndex = "9999"
}

private fun hideContextMenu(selector: String) {
    val menu = document.querySelector(selector) as? HTMLElement ?: return
    menu.style.display = "none"
}

private fun encodeURIComponent(s: String): String = js("encodeURIComponent(s)") as String
