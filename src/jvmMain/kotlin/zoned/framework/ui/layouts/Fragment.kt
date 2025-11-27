package zoned.framework.ui.layouts

import io.javalin.http.Context
import kotlinx.html.*
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.stream.appendHTML
import zoned.framework.api.AuxResponse
import zoned.framework.api.ElementType
import zoned.framework.api.Response
import zoned.framework.api.ResponseType
import zoned.framework.ui.components.buttons.WithFlowbiteAttributes
import zoned.framework.ui.components.buttons.WithSubmitAction
import zoned.framework.ui.components.html
import zoned.framework.ui.libs.Bundle.bundleInit
import zoned.framework.ui.libs.HTMX.htmxBoost
import zoned.framework.ui.libs.isDynamic
import zoned.framework.ui.libs.setHistory
import zoned.framework.util.Either

// XXX split this up / organize better
object Fragment {

    fun CommonAttributeGroupFacade.zone(target: HTMXTarget) {
        id = target.id
        target.markRendered()  // Track that this target has been rendered
    }

    /**
     * @param mutateHtml if provided, allows mutation of the generated html, but turns off automatic unwrap
     */
    fun Context.fragment(target: HTMXTarget,
                         mutateHtml: ((String) -> String)? = null,
                         vararg composed: AuxResponse,
                         block: html.() -> Unit): Response {

        return this.ifragment(target, mutateHtml, composed, block)
    }

    fun Context.fragment(target: HTMXTarget,
                         vararg composed: AuxResponse,
                         block: html.() -> Unit): Response {

        return this.ifragment(target, null, composed, block)
    }

    fun Context.fragment(mutateHtml: ((String) -> String)? = null,
                         vararg composed: AuxResponse,
                         block: html.() -> Unit): Response {

        return this.ifragment(target = null, mutateHtml, composed, block)
    }

    private fun Context.ifragment(target: HTMXTarget? = null,
                                  mutateHtml: ((String) -> String)?,
                                  composed: Array<out AuxResponse>,
                                  block: html.() -> Unit): Response {
        val html = buildString {
            with(appendHTML()) {
                // this wrapper div is necessary because of kotlinx.html's api; however, the unwrap argument below adds
                // an HX-Reselect header which strips it
                div(classes = "wrapper") {
                    block()
                }

                composed.forEach {
                    val tag = tag(it.elementType, this)
                    tag.visitAndFinalize(this) {
                        applyOob(it, tag as CommonAttributeGroupFacade, this@ifragment)
                    }
                }
            }
        }
        val fragment = if (mutateHtml != null) {
            mutateHtml(html)
        } else {
            html
        }
        return Response(Either.left(fragment), target, unwrap = mutateHtml == null)
    }

    fun Context.page(template: PageTemplate, title: String, blocks: List<html.() -> Unit>): Response {
        if (blocks.size != template.slots()) {
            throw Exception("Mismatched number of blocks and template slots")
        }
        val bodyTarget = HTMXTarget("body")

        setHistory()
        return Response(Either.left(writePage(template.htmlClasses()) {
            head {
                meta(name = "viewport", content = "width=device-width, initial-scale=1, maximum-scale=1")
                title(title)

                with (template) {
                    header()
                }
                // TODO how does this work with framework? hardcoded?
                link(href = "/static/output.css", rel = "stylesheet")
                bundleInit()
            }

            body(template.bodyClasses().joinToString(" ")) {
                zone(bodyTarget)
                htmxBoost()
                template.bodyContent(this@page, this, blocks)
            }
        }), if (isDynamic()) { bodyTarget } else { null }, unwrap = false)
    }

    fun Context.toJson(response: Any): Response {
        return Response(Either.right(response), type = ResponseType.JSON)
    }

    private fun html.applyOob(resp: AuxResponse, tag: CommonAttributeGroupFacade, ctx: Context) {
        with (tag) {
            id = resp.target.id
            classes = resp.classes.split(" ").toSet()
            attributes["hx-swap-oob"] = "true"
            resp.subrender(this@applyOob, ctx)
        }
    }

    private fun tag(type: ElementType, consumer: TagConsumer<*>): HtmlBlockTag {
        return when (type) {
            ElementType.SPAN -> SPAN(mapOf(), consumer)
            ElementType.DIV -> DIV(mapOf(), consumer)
            ElementType.P -> P(mapOf(), consumer)
            // TR has odd typing
//            ElementType.TR -> TR(mapOf(), consumer)
            ElementType.TD -> TD(mapOf(), consumer)
        }
    }

    fun withFlowbite(attrs: Map<String, String>) = WithFlowbiteAttributes(attrs)

    fun submit() = WithSubmitAction
}

inline fun writePage(htmlClasses: List<String> = emptyList(), crossinline block : HTML.() -> Unit): String {

    return createHTMLDocument().html {
        // TODO switchable language
        lang = "en"
        if (htmlClasses.isNotEmpty()) {
            classes = htmlClasses.toSet()
        }
        block()
    }.serialize()
}