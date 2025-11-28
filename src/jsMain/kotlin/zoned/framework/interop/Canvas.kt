package zoned.framework.interop

import web.html.HTMLCanvasElement

/**
 * Extension for HTMLCanvasElement.toDataURL with quality parameter.
 * kotlin-browser only has the single-argument overload typed.
 */
fun HTMLCanvasElement.toDataURL(type: String, quality: Double): String {
    return asDynamic().toDataURL(type, quality) as String
}
