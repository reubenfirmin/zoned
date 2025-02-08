package zoned.framework.ui.components.dropdown

import kotlinx.html.*
import zoned.framework.ui.components.buttons.ButtonType
import zoned.framework.ui.components.buttons.ButtonType.DROPDOWN
import zoned.framework.ui.components.buttons.WithFlowbiteAttributes
import zoned.framework.ui.components.buttons.ibutton
import zoned.framework.ui.components.icons.dropdownIcon

class CheckboxDropdown(val id: String,
                       val label: String,
                       val options: Options,
                       val action: zoned.framework.ui.components.buttons.ButtonAction,
                       consumer: TagConsumer<*>): SPAN(mapOf(), consumer) {

    fun render() {
        ibutton(
            DROPDOWN, label, WithFlowbiteAttributes(mapOf("data-dropdown-toggle" to id)),
            id = "${id}Button",
            postlabel = {
                dropdownIcon("-mr-1 ml-1.5 w-5 h-5")
            })

        div("z-10 hidden w-48 p-3 bg-white rounded-lg shadow dark:bg-gray-700") {
            id = this@CheckboxDropdown.id
            h6("mb-3 text-sm font-medium text-gray-900 dark:text-white") {
                +this@CheckboxDropdown.options.label
            }
            form {
                ul("space-y-2 text-sm") {
                    attributes["aria-labelledby"] = "${this@CheckboxDropdown.id}Button"
                    this@CheckboxDropdown.options.options.forEach { option ->
                        li("flex items-center") {
                            input(classes = "w-4 h-4 bg-gray-100 border-gray-300 rounded text-primary-600 " +
                                    "focus:ring-primary-500 dark:focus:ring-primary-600 dark:ring-offset-gray-700 " +
                                    "focus:ring-2 dark:bg-gray-600 dark:border-gray-500") {
                                id = option.id
                                name = option.id
                                type = InputType.checkBox
                                checked = option.selected
                                value = ""
                            }
                            label("ml-2 text-sm font-medium text-gray-900 dark:text-gray-100") {
                                htmlFor = option.id
                                +option.label
                            }
                        }
                    }
                }
                div("mt-4 mr-2") {
                    ibutton(ButtonType.PRIMARY, "Apply", this@CheckboxDropdown.action)
                }
            }
        }
    }
}

fun FlowContent.checkboxDropdown(id: String, label: String, options: Options, action: zoned.framework.ui.components.buttons.ButtonAction) {
    CheckboxDropdown(id, label, options, action, consumer).visit {
        render()
    }
}

data class Options(val label: String, val options: List<Option>)

data class Option(val id: String, val label: String, val selected: Boolean)