package zoned.framework.libs

import org.w3c.dom.Element

@JsModule("dragula")
@JsNonModule
external fun dragula(options: DrakeOptions): Drake

external class Drake {
    fun on(event: String, cb: (element: Element) -> Unit)

    fun on(event: String, cb: (element: Element, source: Element) -> Unit)

    fun on(event: String, cb: (element: Element, container: Element, source: Element) -> Unit)

    fun on(event: String, cb: (element: Element, target: Element, source: Element, sibling: Element?) -> Unit)

    val containers: JavascriptArray
}

interface DrakeOptions {

    val moves: (Element, Element, Any, Element) -> Boolean

    val revertOnSpill: Boolean

    val removeOnSpill: Boolean

    val copy: Boolean
}

external class JavascriptArray {

    fun push(el: Element)

    fun splice(start: Int)

    fun splice(start: Int, numElements: Int)

    fun findIndex(test: (value: Element, index: Int, array: JavascriptArray) -> Boolean): Int?

    val length: Int

    fun at(index: Int): Element

    fun includes(el: Element): Boolean
}