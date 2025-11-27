package zoned.framework.ui.enhancements

import kotlinx.css.CssBuilder
import web.dom.Document
import web.dom.Element
import web.html.HTMLAnchorElement
import web.html.HTMLElement
import web.html.HTMLImageElement
import web.html.HTMLInputElement
import web.html.HTMLFormElement
import zoned.framework.interop.css

/**
 * Factory for creating DOM elements with a DSL syntax.
 *
 * Usage:
 * ```kotlin
 * element.create.div {
 *     className = "toolbar"
 *     css { display = Display.flex }
 *
 *     child(element.create.button {
 *         textContent = "Click me"
 *         onClick { println("clicked!") }
 *     })
 * }
 * ```
 */
class ElementFactory(private val document: Document) {

    fun div(block: ElementBuilder.() -> Unit = {}): Element = createElement("div", block)
    fun span(block: ElementBuilder.() -> Unit = {}): Element = createElement("span", block)
    fun button(block: ElementBuilder.() -> Unit = {}): Element = createElement("button", block)
    fun input(block: ElementBuilder.() -> Unit = {}): Element = createElement("input", block)
    fun textarea(block: ElementBuilder.() -> Unit = {}): Element = createElement("textarea", block)
    fun a(block: ElementBuilder.() -> Unit = {}): Element = createElement("a", block)
    fun ul(block: ElementBuilder.() -> Unit = {}): Element = createElement("ul", block)
    fun li(block: ElementBuilder.() -> Unit = {}): Element = createElement("li", block)
    fun p(block: ElementBuilder.() -> Unit = {}): Element = createElement("p", block)
    fun h1(block: ElementBuilder.() -> Unit = {}): Element = createElement("h1", block)
    fun h2(block: ElementBuilder.() -> Unit = {}): Element = createElement("h2", block)
    fun h3(block: ElementBuilder.() -> Unit = {}): Element = createElement("h3", block)
    fun label(block: ElementBuilder.() -> Unit = {}): Element = createElement("label", block)
    fun img(block: ElementBuilder.() -> Unit = {}): Element = createElement("img", block)

    /**
     * Create any element by tag name.
     */
    fun element(tagName: String, block: ElementBuilder.() -> Unit = {}): Element =
        createElement(tagName, block)

    private fun createElement(tagName: String, block: ElementBuilder.() -> Unit): Element {
        val element = document.createElement(tagName) as HTMLElement
        ElementBuilder(element).apply(block)
        return element
    }
}

/**
 * Builder for configuring an element during creation.
 */
class ElementBuilder(private val element: HTMLElement) {

    /** Access to the underlying HTMLElement for escape-hatch operations */
    val raw: HTMLElement get() = element

    // ========== Properties ==========

    var id: String
        get() = element.id
        set(value) { element.id = value }

    var className: String
        get() = element.className
        set(value) { element.className = value }

    var textContent: String
        get() = element.textContent ?: ""
        set(value) { element.textContent = value }

    var innerHTML: String
        get() = element.innerHTML
        set(value) { element.innerHTML = value }

    // ========== Input-specific ==========

    private val asInput: HTMLInputElement? get() = element as? HTMLInputElement

    var type: String
        get() = element.getAttribute("type") ?: ""
        set(value) { element.setAttribute("type", value) }

    var value: String
        get() = asInput?.value ?: ""
        set(value) { asInput?.let { it.value = value } }

    var name: String
        get() = element.getAttribute("name") ?: ""
        set(value) { element.setAttribute("name", value) }

    var placeholder: String
        get() = asInput?.placeholder ?: ""
        set(value) { asInput?.let { it.placeholder = value } }

    var disabled: Boolean
        get() = asInput?.disabled ?: false
        set(value) { asInput?.let { it.disabled = value } }

    var contentEditable: String
        get() = element.contentEditable
        set(value) { element.contentEditable = value }

    // ========== Link-specific ==========

    private val asAnchor: HTMLAnchorElement? get() = element as? HTMLAnchorElement

    var href: String
        get() = asAnchor?.href ?: ""
        set(value) { asAnchor?.href = value }

    var target: String
        get() = element.getAttribute("target") ?: ""
        set(value) { element.setAttribute("target", value) }

    // ========== Image-specific ==========

    private val asImage: HTMLImageElement? get() = element as? HTMLImageElement

    var src: String
        get() = asImage?.src ?: ""
        set(value) { asImage?.src = value }

    var alt: String
        get() = asImage?.alt ?: ""
        set(value) { asImage?.let { it.alt = value } }

    // ========== Styling ==========

    /**
     * Apply type-safe CSS styles using kotlin-css CssBuilder.
     */
    fun css(block: CssBuilder.() -> Unit) = element.css(block)

    // ========== Event Handlers ==========

    fun onClick(handler: () -> Unit) {
        element.asDynamic().onclick = { handler() }
    }

    fun onClick(handler: (dynamic) -> Unit) {
        element.asDynamic().onclick = handler
    }

    fun onInput(handler: () -> Unit) {
        element.asDynamic().oninput = { handler() }
    }

    fun onInput(handler: (dynamic) -> Unit) {
        element.asDynamic().oninput = handler
    }

    fun onMouseOver(handler: () -> Unit) {
        element.asDynamic().onmouseover = { handler() }
    }

    fun onMouseOut(handler: () -> Unit) {
        element.asDynamic().onmouseout = { handler() }
    }

    fun onKeyDown(handler: (dynamic) -> Unit) {
        element.asDynamic().onkeydown = handler
    }

    fun onFocus(handler: () -> Unit) {
        element.asDynamic().onfocus = { handler() }
    }

    fun onBlur(handler: () -> Unit) {
        element.asDynamic().onblur = { handler() }
    }

    // ========== Attributes ==========

    fun setAttribute(name: String, value: String) {
        element.setAttribute(name, value)
    }

    fun data(name: String, value: String) {
        element.setAttribute("data-$name", value)
    }

    // ========== Children ==========

    /**
     * Append a child element.
     */
    fun child(childElement: Element) {
        element.appendChild(childElement)
    }

    /**
     * Append multiple child elements.
     */
    fun children(vararg elements: Element) {
        elements.forEach { element.appendChild(it) }
    }

    /**
     * Add raw text content. Use textContent property for simple cases.
     */
    operator fun String.unaryPlus() {
        element.appendChild(element.ownerDocument!!.createTextNode(this))
    }
}
