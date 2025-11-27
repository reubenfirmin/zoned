package zoned.framework.ui.enhancements

import kotlinx.browser.document
import kotlinx.css.*
import web.html.HTMLFormElement
import web.html.HTMLInputElement

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
    try {
        // Clear and setup container
        element.clear()
        element.css {
            display = Display.flex
            flexDirection = FlexDirection.column
        }

        // Create toolbar
        val toolbar = element.create.div {
            className = "wysiwyg-toolbar"
            css {
                display = Display.flex
                gap = 4.px
                padding = Padding(8.px)
                backgroundColor = Color("#1f2937")
                border = Border(1.px, BorderStyle.solid, Color("#4b5563"))
                borderTopLeftRadius = 8.px
                borderTopRightRadius = 8.px
                borderBottomLeftRadius = 0.px
                borderBottomRightRadius = 0.px
                flexWrap = FlexWrap.wrap
            }
        }

        // Add toolbar buttons based on config
        val buttons = when (config.toolbar) {
            "full" -> arrayOf(
                arrayOf("bold", "B", "Bold", "font-weight: bold;"),
                arrayOf("italic", "I", "Italic", "font-style: italic;"),
                arrayOf("underline", "U", "Underline", "text-decoration: underline;"),
                arrayOf("strikeThrough", "S", "Strikethrough", "text-decoration: line-through;"),
                arrayOf("separator", "", "", ""),
                arrayOf("insertUnorderedList", "•", "Bullet List", ""),
                arrayOf("insertOrderedList", "1.", "Numbered List", ""),
                arrayOf("separator", "", "", ""),
                arrayOf("removeFormat", "✕", "Clear Formatting", "")
            )
            "standard" -> arrayOf(
                arrayOf("bold", "B", "Bold", "font-weight: bold;"),
                arrayOf("italic", "I", "Italic", "font-style: italic;"),
                arrayOf("underline", "U", "Underline", "text-decoration: underline;"),
                arrayOf("separator", "", "", ""),
                arrayOf("insertUnorderedList", "•", "Bullet List", ""),
                arrayOf("insertOrderedList", "1.", "Numbered List", "")
            )
            else -> arrayOf( // minimal
                arrayOf("bold", "B", "Bold", "font-weight: bold;"),
                arrayOf("italic", "I", "Italic", "font-style: italic;"),
                arrayOf("insertUnorderedList", "•", "Bullet List", "")
            )
        }

        buttons.forEach { btn ->
            val command = btn[0]
            val label = btn[1]
            val title = btn[2]
            val extraStyle = btn[3]

            if (command == "separator") {
                val sep = element.create.div {
                    css {
                        width = 1.px
                        height = 24.px
                        backgroundColor = Color("#4b5563")
                        margin = Margin(0.px, 4.px)
                    }
                }
                toolbar.appendChild(sep)
            } else {
                val button = element.create.button {
                    type = "button"
                    textContent = label
                    setAttribute("title", title)
                    data("command", command)
                    css {
                        padding = Padding(4.px, 8.px)
                        minWidth = 28.px
                        backgroundColor = Color("#374151")
                        border = Border(1.px, BorderStyle.solid, Color("#4b5563"))
                        borderRadius = 4.px
                        color = Color("#e5e7eb")
                        cursor = Cursor.pointer
                        fontSize = 14.px
                    }
                    // Apply extra style via raw attribute if provided
                    if (extraStyle.isNotBlank()) {
                        val currentStyle = raw.getAttribute("style") ?: ""
                        raw.setAttribute("style", "$currentStyle $extraStyle")
                    }
                    onMouseOver { raw.asDynamic().style.background = "#4b5563" }
                    onMouseOut { raw.asDynamic().style.background = "#374151" }
                }
                toolbar.appendChild(button)
            }
        }

        element.appendChild(toolbar)

        // Create hidden input to store HTML content
        val hiddenInput = element.create.input {
            type = "hidden"
            name = config.inputName
            value = config.initialContent
        }

        // Create editor area
        val editor = element.create.div {
            contentEditable = "true"
            className = "wysiwyg-editor"
            css {
                minHeight = config.minHeight.px
                padding = Padding(12.px)
                border = Border(1.px, BorderStyle.solid, Color("#4b5563"))
                borderTopWidth = 0.px
                borderTopLeftRadius = 0.px
                borderTopRightRadius = 0.px
                borderBottomLeftRadius = 8.px
                borderBottomRightRadius = 8.px
                backgroundColor = Color("#374151")
                color = Color("#e5e7eb")
                overflowY = Overflow.auto
                outline = Outline.none
            }
            if (config.initialContent.isNotBlank()) {
                innerHTML = config.initialContent
            }
            if (config.placeholder.isNotBlank()) {
                data("placeholder", config.placeholder)
            }
            // Update hidden input on content change
            onInput(fun() {
                (hiddenInput as? HTMLInputElement)?.value = sanitizeHtml(raw.innerHTML)
            })
            // Ctrl+Enter to submit form
            onKeyDown { e ->
                if (e.ctrlKey == true && e.key == "Enter") {
                    e.preventDefault()
                    (hiddenInput as? HTMLInputElement)?.value = sanitizeHtml(raw.innerHTML)
                    (raw.closest("form") as? HTMLFormElement)?.requestSubmit()
                    // Clear editor after submit
                    raw.innerHTML = ""
                    (hiddenInput as? HTMLInputElement)?.value = ""
                }
            }
        }

        element.appendChild(editor)
        element.appendChild(hiddenInput)

        // Wire up toolbar buttons
        val toolbarButtons = toolbar.querySelectorAll("button[data-command]")
        for (i in 0 until toolbarButtons.length) {
            val btn = toolbarButtons.item(i)
            val command = btn?.asDynamic()?.getAttribute("data-command") as? String ?: continue

            btn.asDynamic().onclick = { e: dynamic ->
                e.preventDefault()
                editor.asDynamic().focus()
                document.asDynamic().execCommand(command, false, "")
            }
        }

        console.log("WYSIWYG editor initialized")
    } catch (e: Exception) {
        console.error("Failed to initialize WYSIWYG editor", e)
    }
}
