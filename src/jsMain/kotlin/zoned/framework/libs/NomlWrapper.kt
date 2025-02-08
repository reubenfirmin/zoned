package zoned.framework.libs

import kotlinx.browser.document
import web.html.HTMLCanvasElement

class NomnomlWrapper {
    companion object {
        fun renderToCanvas(canvasId: String, source: String) {
            val canvas = document.getElementById(canvasId) as? HTMLCanvasElement
            if (canvas != null) {
                draw(canvas, source)
            }
        }
    }
}