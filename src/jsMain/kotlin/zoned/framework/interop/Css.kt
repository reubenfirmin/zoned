package zoned.framework.interop

import kotlinx.css.Color
import kotlinx.css.CssBuilder
import kotlinx.css.LinearDimension
import kotlinx.css.filter
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
 * Low-level primitive for emitting a single declaration kotlin-css does not model — [property] is the
 * kebab-case CSS name, [value] the verbatim value. This is the building block for the *typed*
 * extensions below (vendor prefixes, `!important`); prefer those, or a kotlin-css property, over
 * calling [raw] directly. Add a new typed extension here whenever a property is worth typing.
 */
fun CssBuilder.raw(property: String, value: String) {
    declarations[property] = value
}

// --- Typed extensions for CSS that kotlin-css doesn't model -------------------------------------
// kotlin-css has no vendor-prefixed properties and no `!important`. Rather than scatter raw strings
// at call sites, model them here as typed extensions so callers stay in the css {} / rule {} DSL.

/** `-webkit-text-stroke`: a hairline outline painted on glyph edges (monochrome display headings). */
fun CssBuilder.webkitTextStroke(width: LinearDimension, color: Color) =
    raw("-webkit-text-stroke", "$width $color")

/**
 * `background-clip: text` plus the `-webkit-` prefix Chromium still requires — clips the element's
 * background (e.g. a gradient) to its glyph shapes. Pair with a transparent [webkitTextFillColor].
 */
fun CssBuilder.backgroundClipText() {
    raw("-webkit-background-clip", "text")
    raw("background-clip", "text")
}

/**
 * `-webkit-text-fill-color`: the colour painted inside glyphs, overriding `color` (set transparent to
 * reveal a background clipped via [backgroundClipText]).
 */
fun CssBuilder.webkitTextFillColor(color: Color) = raw("-webkit-text-fill-color", "$color")

/** Set [property] to [value] with `!important`, for the rare rule that must beat a more specific one. */
fun CssBuilder.important(property: String, value: Any) = raw(property, "$value !important")

/** `transform-origin` — the pivot point for `transform` (kotlin-css models `transform` but not its
 *  origin). [value] is a CSS origin such as `"center center"`, `"top left"`, or `"50% 50%"`. */
fun CssBuilder.transformOrigin(value: String) = raw("transform-origin", value)

/**
 * `contain-intrinsic-size` — the placeholder size used while a `content-visibility: auto` subtree
 * is skipped (kotlin-css models content-visibility but not this). With [rememberRendered] the
 * browser substitutes the element's actual last-rendered size once it has been shown, so scrollbar
 * geometry stays stable; [estimate] only covers the first, never-yet-rendered pass.
 */
fun CssBuilder.containIntrinsicSize(estimate: LinearDimension, rememberRendered: Boolean = true) =
    raw("contain-intrinsic-size", if (rememberRendered) "auto $estimate" else "$estimate")

// --- filter ---------------------------------------------------------------------------------
// kotlin-css types its `filter` property only as a raw String. Model the filter FUNCTIONS so call
// sites keep typed dimensions/colors — filter(brightness(1.4), dropShadow(0.px, 0.px, 10.px, c)) —
// and let the helper assign kotlin-css's own property with the composed value.

/** One CSS filter function (see [blur]/[brightness]/[dropShadow]); composes via [filter]. */
class FilterFunction internal constructor(private val css: String) {
    override fun toString() = css
}

fun blur(radius: LinearDimension) = FilterFunction("blur($radius)")
fun brightness(factor: Number) = FilterFunction("brightness($factor)")
fun dropShadow(x: LinearDimension, y: LinearDimension, blur: LinearDimension, color: Color) =
    FilterFunction("drop-shadow($x $y $blur $color)")

/** `filter` from typed [FilterFunction]s, space-joined per the spec. */
fun CssBuilder.filter(vararg functions: FilterFunction) {
    filter = functions.joinToString(" ")
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
/*
 * WHICH STYLING PRIMITIVE TO USE — the framework's decision rule:
 *
 *  - `css {}` (inline style, on a tag or element): values that differ PER INSTANCE — geometry,
 *    per-item conditionals, theme tokens applied to one element.
 *  - `styleSheet(id) { rule()/raw() }`: rules SHARED across instances, or anything needing a
 *    selector — pseudo-classes (:hover, :empty), descendant selectors, @keyframes. Re-calling
 *    with the same id replaces the rules, so refresh freely on theme changes.
 *  - `element.style.*` mutation: DYNAMIC runtime geometry on a live element (drag positions,
 *    focus highlights) where rebuilding inline style wholesale would clobber sibling values.
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

private var cssClassCounter = 0

/**
 * Creates a class and attaches it to the head. Sets the current element's classname either to something specific if supplied,
 * or a generated value.
 *
 * NOT idempotent: every call appends to the head's first <style> tag, so calling this from a
 * re-rendering code path grows the stylesheet without bound. For reusable/refreshable rules use
 * [styleSheet] (same id replaces). This exists for one-shot static chrome.
 */
fun CommonAttributeGroupFacade.cssClass(className: String = "clzz${cssClassCounter++}", block: CssBuilder.() -> Unit) {
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
    className: String = "clzz${cssClassCounter++}",
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

