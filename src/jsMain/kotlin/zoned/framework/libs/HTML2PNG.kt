@file:JsModule("html-to-image")
@file:JsNonModule
package zoned.framework.libs

import web.dom.Element
import kotlin.js.Promise

external fun toPng(element: Element, options: HtmlToPngOptions): Promise<String>

external interface HtmlToPngOptions {
    val width: Int
    val height: Int
}