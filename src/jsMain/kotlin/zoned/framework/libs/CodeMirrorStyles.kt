package zoned.framework.libs

import kotlinx.css.*
import kotlinx.css.properties.TextDecoration
import kotlinx.css.properties.TextDecorationLine
import web.cssom.ClassName
import web.dom.document
import web.html.HTMLStyleElement
import zoned.framework.interop.StyleSheetScope

private var editorStyleId = 0

/** One owned stylesheet per editor; updates preserve the document, selection and undo history. */
internal class CodeMirrorStyles(theme: CodeMirrorTheme) {
    val className = ClassName("zoned-cm-${editorStyleId++}")
    private val element = (document.createElement("style") as HTMLStyleElement).also {
        document.head.appendChild(it)
    }

    init { update(theme) }

    fun update(t: CodeMirrorTheme) {
        val root = ".$className.cm-editor"
        element.textContent = StyleSheetScope().apply {
            rule(root) {
                color = t.foreground
                backgroundColor = t.background
                height = 100.pct
                fontSize = t.fontSize
            }
            rule("$root.cm-focused") { outlineStyle = OutlineStyle.none }
            rule("$root .cm-scroller") { overflow = Overflow.auto; fontFamily = t.fontFamily }
            rule("$root .cm-gutters") {
                backgroundColor = t.background
                color = t.muted
                borderStyle = BorderStyle.none
            }
            rule("$root .cm-lineNumbers") { color = t.lineNumberColor }
            rule("$root .cm-activeLine, $root .cm-activeLineGutter") { backgroundColor = t.activeLine }
            rule("$root .cm-selectionBackground, $root.cm-focused .cm-selectionBackground, $root ::selection") {
                backgroundColor = t.selection
            }
            rule("$root .cm-cursor, $root .cm-dropCursor") { borderLeftColor = t.foreground }
            rule("$root .cm-tooltip, $root .cm-panels") {
                color = t.popupForeground
                backgroundColor = t.popupBackground
            }
            rule("$root .cm-tooltip-autocomplete ul li[aria-selected]") {
                color = t.popupForeground
                backgroundColor = t.selection
            }
            rule("$root .tok-heading") { color = t.heading; fontWeight = FontWeight.w700 }
            rule("$root .tok-list") { color = t.accent }
            rule("$root .tok-strong") { color = t.accent; fontWeight = FontWeight.w700 }
            rule("$root .tok-emphasis") { fontStyle = FontStyle.italic }
            rule("$root .tok-link") {
                color = t.link
                textDecoration = TextDecoration(setOf(TextDecorationLine.underline))
            }
            rule("$root .tok-url") { color = t.link }
            rule("$root .tok-quote") { color = t.muted; fontStyle = FontStyle.italic }
            rule("$root .tok-monospace") { fontFamily = "monospace" }
        }.render()
    }

    fun highlighting(modules: CodeMirrorModules): CmExtension {
        val tags = modules.highlight.tags
        return modules.language.syntaxHighlighting(modules.highlight.tagHighlighter(arrayOf(
            CmTagClass(tags.heading, "tok-heading"),
            CmTagClass(tags.list, "tok-list"),
            CmTagClass(tags.strong, "tok-strong"),
            CmTagClass(tags.emphasis, "tok-emphasis"),
            CmTagClass(tags.link, "tok-link"),
            CmTagClass(tags.url, "tok-url"),
            CmTagClass(tags.quote, "tok-quote"),
            CmTagClass(tags.monospace, "tok-monospace"),
        )))
    }

    fun destroy() = element.remove()
}
