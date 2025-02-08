package zoned.framework.ui.components

import kotlinx.html.*

class Carousel(private val images: List<String>, consumer: TagConsumer<*>): DIV(mapOf("class" to "p-2 relative w-full"), consumer) {

    fun render(block: Carousel.() -> Unit) {
        id = "default-carousel"
        attributes["data-carousel"] = "static" // or "slide" for animated

        // TODO make an actual carousel in future
        images.first().let { image ->
            img() {
                src = image
                alt = "..."
            }
        }

        block()
    }
}

fun FlowContent.carousel(images: List<String>, block: Carousel.() -> Unit = {}) {
    Carousel(images, consumer).visit {
        render(block)
    }
}