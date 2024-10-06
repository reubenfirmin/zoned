package zoned.framework.ui.libs.components.htmx

import zoned.framework.ui.tags.svg
import kotlinx.html.*

/**
 * Creates a form with an input which is POSTed to the path (the input name ends up as a form param)
 */
class HTMXSearchInput(private val styleClasses: String,
                      private val inputName: String,
                      private val inputPlaceholder: String,
                      private val params: Map<String, String>,
                      val path: String,
                      private val elementTarget: String,
                      consumer: TagConsumer<*>): DIV(mapOf("class" to "relative w-full"), consumer) {

    val searchDelay = 200 // ms. could be parameterized

    fun render(block: HTMXSearchInput.() -> Unit) {
        div("absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none") {
            // magnifying glass
            svg("w-5 h-5 text-gray-500 dark:text-gray-400") {
                attributes["aria-hidden"] = "true"
                fill = "currentColor"
                viewBox = "0 0 20 20"
                path {
                    attributes["fill-rule"] = "evenodd"
                    d =
                        "M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 " +
                                "01-1.414 1.414l-4.816-4.816A6 6 0 012 8z"
                    attributes["clip-rule"] = "evenodd"
                }
            }
        }
        // screenreader
        label("sr-only") {
            htmlFor = "search"
            +"""Search"""
        }
        form {
            this@HTMXSearchInput.params.entries.forEach {
                input {
                    type = InputType.hidden
                    name = it.key
                    value = it.value
                }
            }
            input {
                type = InputType.text
                this@HTMXSearchInput.let {
                    name = it.inputName
                    classes = it.styleClasses.split(" ").toSet()
                    attributes["hx-post"] = it.path
                    attributes["hx-trigger"] = "keyup changed delay:${it.searchDelay}ms"
                    attributes["hx-target"] = it.elementTarget
                    placeholder = it.inputPlaceholder
                    it.block()
                }
            }
        }
    }
}

fun FlowContent.htmxSearchInput(name: String, placeholder: String, path: String, target: String,
                                withOptions: WithOptions, block: HTMXSearchInput.() -> Unit) {

    HTMXSearchInput(withOptions.classes, name, placeholder, withOptions.params, path, target, consumer).visit {
        render(block)
    }
}