@file:JsModule("html-to-image")
@file:JsNonModule
package zoned.framework.libs

import org.w3c.dom.Element
import kotlin.js.Promise

external fun toPng(element: Element, options: HtmlToPngOptions): Promise<String>

external interface HtmlToPngOptions {
    val width: Int
    val height: Int
}