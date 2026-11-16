package zoned.framework.gl

import org.w3c.dom.HTMLElement
import js.objects.unsafeJso

external interface RendererOptions {
    var antialias: Boolean
    var alpha: Boolean
}

/** Typed constructor for a transparent, antialiased renderer — hides the JS options bag. */
fun webGLRenderer(antialias: Boolean = true, alpha: Boolean = true): WebGLRenderer {
    val params = unsafeJso<RendererOptions> {
        this.antialias = antialias
        this.alpha = alpha
    }
    return WebGLRenderer(params)
}

/** Set a uniform 2D scale on an element (used to size billboard labels by depth). */
fun HTMLElement.setScale(scale: Double) {
    style.setProperty("transform", "scale($scale)")
}

/** Set element opacity from a number. */
fun HTMLElement.setOpacity(value: Double) {
    style.setProperty("opacity", value.toString())
}
