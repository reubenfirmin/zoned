package zoned.framework.ui.enhancements

import kotlinx.browser.document
import kotlinx.html.*
import web.html.HTMLElement
import web.html.HTMLFormElement
import web.html.HTMLInputElement
import zoned.framework.ui.enhancements.onClick as htmlOnClick
import zoned.framework.ui.enhancements.onInput as htmlOnInput
import zoned.framework.ui.enhancements.onMouseEnter as htmlOnMouseEnter
import zoned.framework.ui.enhancements.onMouseLeave as htmlOnMouseLeave
import zoned.framework.ui.enhancements.onKeyDown as htmlOnKeyDown

/**
 * Sanitize HTML to be valid XHTML (self-closing tags)
 */
private fun sanitizeHtml(html: String): String {
    return html
        .replace(Regex("<br\\s*>"), "<br/>")
        .replace(Regex("<hr\\s*>"), "<hr/>")
        .replace(Regex("<img([^>]*)(?<!/)>"), "<img$1/>")
}

/**
 * Client-side implementation of the WYSIWYG enhancement.
 * Uses contenteditable with basic formatting toolbar.
 */
fun initWysiwygEnhancement(element: EnhancementElement, config: WysiwygConfig) {
    val editorId = "wysiwyg-editor-${config.inputName}"
    val hiddenInputId = "wysiwyg-hidden-${config.inputName}"
    val toolbarId = "wysiwyg-toolbar-${config.inputName}"

    try {
        val container = element.raw
        container.innerHTML = ""
        container.style.display = "flex"
        container.style.flexDirection = "column"

        val buttons = when (config.toolbar) {
            "full" -> listOf(
                listOf("bold", "B", "Bold", "font-weight: bold;"),
                listOf("italic", "I", "Italic", "font-style: italic;"),
                listOf("underline", "U", "Underline", "text-decoration: underline;"),
                listOf("strikeThrough", "S", "Strikethrough", "text-decoration: line-through;"),
                listOf("separator", "", "", ""),
                listOf("insertUnorderedList", "•", "Bullet List", ""),
                listOf("insertOrderedList", "1.", "Numbered List", ""),
                listOf("separator", "", "", ""),
                listOf("removeFormat", "✕", "Clear Formatting", "")
            )
            "standard" -> listOf(
                listOf("bold", "B", "Bold", "font-weight: bold;"),
                listOf("italic", "I", "Italic", "font-style: italic;"),
                listOf("underline", "U", "Underline", "text-decoration: underline;"),
                listOf("separator", "", "", ""),
                listOf("insertUnorderedList", "•", "Bullet List", ""),
                listOf("insertOrderedList", "1.", "Numbered List", "")
            )
            else -> listOf(
                listOf("bold", "B", "Bold", "font-weight: bold;"),
                listOf("italic", "I", "Italic", "font-style: italic;"),
                listOf("insertUnorderedList", "•", "Bullet List", "")
            )
        }

        // Build DOM structure first
        element.appendTo().apply {
            // Toolbar
            div("wysiwyg-toolbar") {
                id = toolbarId
                style = "display: flex; gap: 4px; padding: 8px; background-color: #1f2937; " +
                        "border: 1px solid #4b5563; border-radius: 8px 8px 0 0; flex-wrap: wrap;"

                buttons.forEachIndexed { index, btn ->
                    val command = btn[0]
                    val label = btn[1]
                    val btnTitle = btn[2]
                    val extraStyle = btn[3]

                    if (command == "separator") {
                        div {
                            style = "width: 1px; height: 24px; background-color: #4b5563; margin: 0 4px;"
                        }
                    } else {
                        button(type = ButtonType.button) {
                            id = "wysiwyg-btn-${config.inputName}-$index"
                            attributes["data-command"] = command
                            +label
                            title = btnTitle
                            val baseStyle = "padding: 4px 8px; min-width: 28px; background-color: #374151; " +
                                    "border: 1px solid #4b5563; border-radius: 4px; color: #e5e7eb; " +
                                    "cursor: pointer; font-size: 14px;"
                            style = if (extraStyle.isNotBlank()) "$baseStyle $extraStyle" else baseStyle
                        }
                    }
                }
            }

            // Hidden input
            input(type = InputType.hidden, name = config.inputName) {
                id = hiddenInputId
                value = config.initialContent
            }

            // Editor area
            div("wysiwyg-editor") {
                id = editorId
                contentEditable = true
                style = "min-height: ${config.minHeight}px; padding: 12px; " +
                        "border: 1px solid #4b5563; border-top-width: 0; " +
                        "border-radius: 0 0 8px 8px; background-color: #374151; " +
                        "color: #e5e7eb; overflow-y: auto; outline: none;"

                if (config.initialContent.isNotBlank()) {
                    unsafe { +config.initialContent }
                }
                if (config.placeholder.isNotBlank()) {
                    attributes["data-placeholder"] = config.placeholder
                }
            }
        }

        // Get element references after DOM is built
        val editor = document.getElementById(editorId) as? HTMLElement
        val hiddenInput = document.getElementById(hiddenInputId) as? HTMLInputElement
        val toolbar = document.getElementById(toolbarId) as? HTMLElement

        // Attach toolbar button handlers
        toolbar?.querySelectorAll("button[data-command]")?.let { nodeList ->
            for (i in 0 until nodeList.length) {
                val btn = nodeList.item(i) as? HTMLElement ?: continue
                val command = btn.getAttribute("data-command") ?: continue

                btn.htmlOnMouseEnter { _ ->
                    btn.style.backgroundColor = "#4b5563"
                }
                btn.htmlOnMouseLeave { _ ->
                    btn.style.backgroundColor = "#374151"
                }
                btn.htmlOnClick { e ->
                    e.preventDefault()
                    editor?.focus()
                    document.asDynamic().execCommand(command, false, "")
                }
            }
        }

        // Attach editor handlers
        editor?.htmlOnInput { _ ->
            hiddenInput?.value = sanitizeHtml(editor.innerHTML)
        }

        editor?.htmlOnKeyDown { e ->
            // Support both Ctrl+Enter and Alt+Enter for submit
            if ((e.ctrlKey || e.altKey) && e.key == "Enter") {
                e.preventDefault()
                hiddenInput?.value = sanitizeHtml(editor.innerHTML)
                (editor.closest("form") as? HTMLFormElement)?.requestSubmit()
                editor.innerHTML = ""
                hiddenInput?.value = ""
            }
        }

        console.log("WYSIWYG editor initialized")
    } catch (e: Exception) {
        console.error("Failed to initialize WYSIWYG editor", e)
    }
}
