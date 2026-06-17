package zoned.framework.ui.libs.components.htmx

import zoned.framework.ui.tags.svg
import kotlinx.html.*
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.libs.onTypingPause

/**
 * Creates a form with an input which is POSTed via HTMX (the input name ends up as a form param)
 *
 * @deprecated Use SearchInput with HTMXAction for type-safe HTMX integration
 */
@Deprecated("Use SearchInput with HTMXAction", ReplaceWith("searchInput"))
@Suppress("DEPRECATION")
class HTMXSearchInput(private val styleClasses: String,
                      private val inputName: String,
                      private val inputPlaceholder: String,
                      private val params: Map<String, String>,
                      val searchAction: HTMXAction?,
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

                    // Use type-safe HTMX action if provided
                    if (it.searchAction != null) {
                        onTypingPause(it.searchAction, delayMs = it.searchDelay)
                    }

                    placeholder = it.inputPlaceholder
                    it.block()
                }
            }
        }
    }
}

/**
 * @deprecated Use searchInput with HTMXAction for type-safe HTMX integration
 */
@Deprecated("Use searchInput with HTMXAction", ReplaceWith("searchInput"))
@Suppress("DEPRECATION")
fun FlowContent.htmxSearchInput(name: String, placeholder: String, searchAction: HTMXAction,
                                withOptions: WithOptions, block: HTMXSearchInput.() -> Unit) {

    HTMXSearchInput(withOptions.classes, name, placeholder, withOptions.params, searchAction, consumer).visit {
        render(block)
    }
}