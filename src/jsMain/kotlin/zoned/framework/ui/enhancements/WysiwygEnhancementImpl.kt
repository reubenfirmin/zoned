package zoned.framework.ui.enhancements

import kotlinx.css.*
import kotlinx.css.properties.TextDecoration
import kotlinx.css.properties.TextDecorationLine
import kotlinx.html.*
import web.dom.Node
import web.dom.document
import web.html.HTMLElement
import web.html.HTMLInputElement
import web.html.HTMLTextAreaElement
import zoned.framework.dom.*
import zoned.framework.dom.setInnerHtml
import web.uievents.MouseEvent
import web.uievents.InputEvent
import web.uievents.KeyboardEvent
import zoned.framework.interop.closestForm
import zoned.framework.interop.css
import zoned.framework.interop.cssClassWithHover
import zoned.framework.interop.execCommand

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
 *
 * Wraps a textarea element and enhances it to a rich text editor.
 * If JS doesn't run, the textarea still works as a fallback.
 *
 * Usage (server-side):
 * ```kotlin
 * wysiwyg({ toolbar = "standard" }) {
 *     textArea {
 *         name = "content"
 *         placeholder = "Add a note..."
 *         +existingContent
 *     }
 * }
 * ```
 */
@EnhancementImpl(WysiwygEnhancement::class)
fun TagConsumer<HTMLElement>.initWysiwygEnhancement(config: WysiwygConfig, children: List<Node>) {
    // Find the textarea from server-rendered content
    val textarea = children.asSequence()
        .filterIsInstance<HTMLElement>()
        .mapNotNull { el ->
            if (el.tagName.equals("TEXTAREA", ignoreCase = true)) el as? HTMLTextAreaElement
            else el.querySelector("textarea") as? HTMLTextAreaElement
        }
        .firstOrNull()

    // Extract textarea properties for the editor
    val inputName = textarea?.name ?: "content"
    val initialContent = textarea?.value ?: ""
    val placeholder = textarea?.placeholder ?: ""

    // Element references
    val editorRef = Ref<HTMLElement>()
    val hiddenInputRef = Ref<HTMLInputElement>()
    val toolbarRef = Ref<HTMLElement>()

    // Toolbar button definitions
    val buttons = when (config.toolbar) {
        "full" -> listOf(
            ToolbarButton("bold", "B", "Bold", fontWeight = FontWeight.bold),
            ToolbarButton("italic", "I", "Italic", fontStyle = FontStyle.italic),
            ToolbarButton("underline", "U", "Underline", textDecoration = TextDecoration(setOf(TextDecorationLine.underline))),
            ToolbarButton("strikeThrough", "S", "Strikethrough", textDecoration = TextDecoration(setOf(TextDecorationLine.lineThrough))),
            ToolbarButton.separator(),
            ToolbarButton("insertUnorderedList", "•", "Bullet List"),
            ToolbarButton("insertOrderedList", "1.", "Numbered List"),
            ToolbarButton.separator(),
            ToolbarButton("removeFormat", "✕", "Clear Formatting")
        )
        "standard" -> listOf(
            ToolbarButton("bold", "B", "Bold", fontWeight = FontWeight.bold),
            ToolbarButton("italic", "I", "Italic", fontStyle = FontStyle.italic),
            ToolbarButton("underline", "U", "Underline", textDecoration = TextDecoration(setOf(TextDecorationLine.underline))),
            ToolbarButton.separator(),
            ToolbarButton("insertUnorderedList", "•", "Bullet List"),
            ToolbarButton("insertOrderedList", "1.", "Numbered List")
        )
        else -> listOf(
            ToolbarButton("bold", "B", "Bold", fontWeight = FontWeight.bold),
            ToolbarButton("italic", "I", "Italic", fontStyle = FontStyle.italic),
            ToolbarButton("insertUnorderedList", "•", "Bullet List")
        )
    }

    // Build DOM structure
    div {
        css {
            display = Display.flex
            flexDirection = FlexDirection.column
        }

        // Toolbar
        div("wysiwyg-toolbar") {
            ref(toolbarRef)
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

            buttons.forEach { btn ->
                if (btn.isSeparator) {
                    div {
                        css {
                            width = 1.px
                            height = 24.px
                            backgroundColor = Color("#4b5563")
                            margin = Margin(0.px, 4.px)
                        }
                    }
                } else {
                    button(type = ButtonType.button) {
                        attributes["data-command"] = btn.command
                        +btn.label
                        title = btn.title

                        // Base + hover styles via CSS class
                        cssClassWithHover(
                            base = {
                                padding = Padding(4.px, 8.px)
                                minWidth = 28.px
                                backgroundColor = Color("#374151")
                                border = Border(1.px, BorderStyle.solid, Color("#4b5563"))
                                borderRadius = 4.px
                                color = Color("#e5e7eb")
                                cursor = Cursor.pointer
                                fontSize = 14.px
                                // Apply formatting style to button label
                                btn.fontWeight?.let { fontWeight = it }
                                btn.fontStyle?.let { fontStyle = it }
                                btn.textDecoration?.let { textDecoration = it }
                            },
                            hover = {
                                backgroundColor = Color("#4b5563")
                            }
                        )

                        onClick { e: MouseEvent ->
                            e.preventDefault()
                            editorRef.element.focus()
                            val command = (e.currentTarget as? HTMLElement)?.getAttribute("data-command") ?: return@onClick
                            document.execCommand(command)
                        }
                    }
                }
            }
        }

        // Hidden input for form submission (replaces the textarea)
        input(type = InputType.hidden, name = inputName) {
            ref(hiddenInputRef)
            value = initialContent
        }

        // Editor area - replaces the textarea with contenteditable
        div("wysiwyg-editor") {
            ref(editorRef)
            contentEditable = true
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

            // Set initial content from textarea
            if (initialContent.isNotBlank()) {
                unsafe { +initialContent }
            }

            if (placeholder.isNotBlank()) {
                attributes["data-placeholder"] = placeholder
            }

            // Sync content to hidden input on changes
            onInput { _: InputEvent ->
                hiddenInputRef.element.value = sanitizeHtml(editorRef.element.innerHTML.toString())
            }

            // Handle Ctrl+Enter / Alt+Enter for form submit
            onKeyDown { e: KeyboardEvent ->
                if ((e.ctrlKey || e.altKey) && e.key == "Enter") {
                    e.preventDefault()
                    hiddenInputRef.element.value = sanitizeHtml(editorRef.element.innerHTML.toString())
                    editorRef.element.closestForm()?.requestSubmit()
                    setInnerHtml(editorRef.element, "")
                    hiddenInputRef.element.value = ""
                }
            }
        }
    }

    console.log("WYSIWYG editor initialized")
}

/**
 * Data class for toolbar button configuration.
 * Uses typed CSS properties instead of string styles.
 */
private data class ToolbarButton(
    val command: String,
    val label: String,
    val title: String,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val textDecoration: TextDecoration? = null,
    val isSeparator: Boolean = false
) {
    companion object {
        fun separator() = ToolbarButton("separator", "", "", isSeparator = true)
    }
}
