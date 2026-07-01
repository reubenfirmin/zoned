package zoned.framework.ui.components.inputs

import kotlinx.html.*
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.components.icons.searchIcon
import zoned.framework.ui.libs.onClick
import zoned.framework.ui.libs.onTypingPause

class SearchInput(val classes: String, val label: String, val value: String, val searchAction: HTMXAction?,
                  consumer: TagConsumer<*>): FORM(mapOf(), consumer) {

    fun render() {

        div(classes = this@SearchInput.classes) {
            label("mb-2 text-sm font-medium text-gray-900 sr-only dark:text-white") {
                htmlFor = "default-search"
                +"Search"
            }
            div("relative") {
                div("absolute inset-y-0 start-0 flex items-center ps-3 pointer-events-none") {
                    searchIcon("w-4 h-4 text-gray-500 dark:text-gray-400")
                }
                input(
                    classes = "block w-full p-4 ps-10 text-sm text-gray-900 border border-gray-300 rounded-lg bg-gray-50 " +
                            "focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:border-gray-600 " +
                            "dark:placeholder-gray-400 dark:text-white dark:focus:ring-primary-500 dark:focus:border-primary-500"
                ) {
                    if (this@SearchInput.searchAction != null) {
                        onTypingPause(this@SearchInput.searchAction)
                    }

                    type = InputType.search
                    id = "default-search"
                    name = "query"
                    value = this@SearchInput.value
                    placeholder = this@SearchInput.label
                    required = true
                }

                button(
                    classes = "text-white absolute end-2.5 bottom-2.5 bg-primary-700 hover:bg-primary-800 focus:ring-4 " +
                            "focus:outline-none focus:ring-primary-300 font-medium rounded-lg text-sm px-4 py-2 dark:bg-primary-600 " +
                            "dark:hover:bg-primary-700 dark:focus:ring-primary-800"
                ) {
                    if (this@SearchInput.searchAction != null) {
                        onClick(this@SearchInput.searchAction)
                    }

                    type = ButtonType.submit
                    +"Search"
                }
            }
        }
    }
}

fun FlowContent.searchInput(classes: String, label: String, value: String, searchAction: HTMXAction) =
    SearchInput(classes, label, value, searchAction, consumer).visit {
        render()
    }