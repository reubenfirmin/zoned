package zoned.framework.interop

import kotlinx.css.CssBuilder
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.classes
import kotlinx.html.style
import web.cssom.ClassName
import web.dom.ElementId
import web.dom.document
import web.html.HTMLElement
import web.html.HTMLStyleElement
import web.html.HtmlSource
import web.html.asStringOrNull

/**
 * Worried about performance of inlining css? Don't be. https://danielnagy.me/posts/Post_tsr8q6sx37pl
 * (kotlin-css does not support classname based styles; kotlin-styled-next is required for that.)
 */
fun CommonAttributeGroupFacade.css(block: CssBuilder.() -> Unit) {
    style = CssBuilder().apply(block).toString().removeSuffix("\n")
}

/**
 * Sets inline styles on an HTMLElement.
 *
 * WARNING: This REPLACES the entire style attribute. If you need to modify
 * individual properties without losing existing styles, use element.style.propertyName directly.
 *
 * Example - this will lose position/z-index set earlier:
 * ```
 * element.css { position = Position.absolute; zIndex = 100 }
 * element.css { opacity = 1 }  // Now only has opacity!
 * ```
 *
 * Instead, for incremental updates use:
 * ```
 * element.style.opacity = "1"
 * ```
 */
fun HTMLElement.css(block: CssBuilder.() -> Unit) {
    val style = CssBuilder().apply(block)
    this.setAttribute("style", style.toString().removeSuffix("\n"))
}

fun HTMLElement.classes(classes: String) {
    this.className = ClassName(classes.trim().split(Regex("\\s+")).distinct().joinToString(" "))
}

/**
 * Typed stylesheet injection — the rule-based companion to [css].
 *
 * [css] only writes an element's inline `style`, so it cannot express selectors, pseudo-classes
 * (`:hover`), or descendant selectors. [styleSheet] emits real CSS *rules* whose declarations are
 * built from typed kotlin-css [CssBuilder] blocks. The selector is a string (CSS selectors are
 * inherently strings); everything inside the braces stays typed.
 *
 * Rules go into a single `<style id="...">` in `<head>`; calling again with the same [id] REPLACES
 * its contents, so it is safe to call on every render or to refresh after a theme change.
 *
 * ```
 * styleSheet("tdz-task") {
 *     rule(".tdz-task:hover") { backgroundColor = theme.taskHover }
 *     rule(".ace_editor .ace_heading") { color = theme.heading; fontWeight = FontWeight.bold }
 *     raw("@font-face { font-family: 'IBM VGA 8x16'; src: url('/vga.woff') format('woff'); }")
 * }
 * ```
 */
fun styleSheet(id: String, block: StyleSheetScope.() -> Unit) {
    val css = StyleSheetScope().apply(block).render()
    val elementId = ElementId(id)
    val style = (document.getElementById(elementId) as? HTMLStyleElement)
        ?: (document.createElement("style") as HTMLStyleElement).also {
            it.id = elementId
            document.head.appendChild(it)
        }
    style.textContent = css
}

/** Receiver for [styleSheet]: collects typed selector rules, plus a [raw] escape for at-rules. */
class StyleSheetScope internal constructor() {
    private val sb = StringBuilder()

    /** A CSS rule for [selector] whose declarations are typed kotlin-css. */
    fun rule(selector: String, block: CssBuilder.() -> Unit) {
        val body = CssBuilder().apply(block).toString().removeSuffix("\n").trim()
        if (body.isNotEmpty()) sb.append(selector).append(" { ").append(body).append(" }\n")
    }

    /** Escape hatch for at-rules / properties kotlin-css can't model (@font-face, background-clip: text, …). */
    fun raw(css: String) {
        sb.append(css).append('\n')
    }

    internal fun render(): String = sb.toString()
}

var counter = 0

/**
 * Creates a class and attaches it to the head. Sets the current element's classname either to something specific if supplied,
 * or a generated value.
 * According to the video above, this is not as performant as inlining styles, or tailwind.
 */
fun CommonAttributeGroupFacade.cssClass(className: String = "clzz${counter++}", block: CssBuilder.() -> Unit) {
    val rawCss = CssBuilder().apply(block).toString()
    val formattedCss = rawCss.split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(";\n        ")
    val style = """
    .$className {
        $formattedCss;
    }
    """.trimIndent()

    val head = document.head
    val styleTag = head.getElementsByTagName("style").firstOrNull() as? HTMLStyleElement
        ?: document.createElement("style").also { head.appendChild(it) } as HTMLStyleElement

    val currentHtml = styleTag.innerHTML.asStringOrNull() ?: ""
    styleTag.innerHTML = HtmlSource("$currentHtml\n$style")

    this.classes = setOf(className) + this.classes
}

/**
 * Creates a class with hover state support.
 * Attaches both base styles and hover styles to the head.
 *
 * Usage:
 * ```kotlin
 * button {
 *     cssClassWithHover(
 *         base = { backgroundColor = Color("#374151") },
 *         hover = { backgroundColor = Color("#4b5563") }
 *     )
 * }
 * ```
 */
fun CommonAttributeGroupFacade.cssClassWithHover(
    className: String = "clzz${counter++}",
    base: CssBuilder.() -> Unit,
    hover: CssBuilder.() -> Unit
) {
    val baseCss = CssBuilder().apply(base).toString()
    val hoverCss = CssBuilder().apply(hover).toString()

    fun formatCss(raw: String) = raw.split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(";\n        ")

    val style = """
    .$className {
        ${formatCss(baseCss)};
    }
    .$className:hover {
        ${formatCss(hoverCss)};
    }
    """.trimIndent()

    val head = document.head
    val styleTag = head.getElementsByTagName("style").firstOrNull() as? HTMLStyleElement
        ?: document.createElement("style").also { head.appendChild(it) } as HTMLStyleElement

    val currentHtml = styleTag.innerHTML.asStringOrNull() ?: ""
    styleTag.innerHTML = HtmlSource("$currentHtml\n$style")

    this.classes = setOf(className) + this.classes
}

