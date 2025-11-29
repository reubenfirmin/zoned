package zoned.framework.interop

import kotlinx.css.CssBuilder
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.classes
import kotlinx.html.style
import web.cssom.ClassName
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

