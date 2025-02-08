@file:JsModule("nomnoml")
@file:JsNonModule
package zoned.framework.libs

import web.html.HTMLCanvasElement

external fun draw(canvas: HTMLCanvasElement, source: String)
