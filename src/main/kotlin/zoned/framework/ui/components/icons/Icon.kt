package zoned.framework.ui.components.icons

import zoned.framework.ui.tags.SvgTag
import kotlinx.html.TagConsumer

/**
 * @param filled - adds fill = currentColor (vs none), stroke = currentColor
 * @param attributeSet - filled for fill-rule/clip-rule = evenodd, outlined for stroke-linecap/linejoin = round
 */
abstract class Icon(classes: String,
                    val filled: Boolean,
                    private val attributeSet: Map<String, String>,
                    private val viewBoxDim: Pair<Int, Int> = 20 to 20,
                    private val color: String? = null,
                    consumer: TagConsumer<*>):
    SvgTag(classes, consumer) {

    abstract fun paths(): List<String>

    fun render() {
        attributes["aria-hidden"] = "true"
        viewbox = "0 0 ${viewBoxDim.first} ${viewBoxDim.second}"
        if (color != null) {
            attributes["style"] = "color: #$color"
        }
        fill = if (filled) {
            "currentColor"
        } else {
            "none"
        }
        paths().forEach { pth ->
            path {
                if (!this@Icon.filled) {
                    attributes["stroke"] = "currentColor"
                }
                this@Icon.attributeSet.forEach {
                    attributes[it.key] = it.value
                }
                d = pth
            }
        }
    }
}

object IconAttributes {
    // TODO i don't think the names of these are right...

    fun filled() = mapOf(
        "fill-rule" to "evenodd",
        "clip-rule" to "evenodd"
    )

    fun outlined() = mapOf(
        "stroke-linecap" to "round",
        "stroke-linejoin" to "round",
        "stroke-width" to "2"
    )
}