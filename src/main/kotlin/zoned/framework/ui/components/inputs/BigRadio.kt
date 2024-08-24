package zoned.framework.ui.components.inputs

import kotlinx.html.*
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.components.icons.arrowRightIcon
import zoned.framework.ui.libs.onClick

/**
 * If parameterizer is supplied, will send the option id as a url parameter to the selected handler
 */
class BigRadio(val heading: String,
               private val groupName: String,
               private val selectedId: String,
               private val options: List<RadioOption>,
               consumer: TagConsumer<*>): DIV(initialAttributes = mapOf(), consumer = consumer) {

    fun render() {
        h3("mb-5 text-lg font-medium text-gray-900 dark:text-white") {
            +this@BigRadio.heading
        }
        ul("grid w-full gap-6 md:grid-cols-2") {
            this@BigRadio.options.forEach { option ->
                li {
                    if (option.action != null) {
                        onClick(option.action)
                    }

                    input(classes = "hidden peer") {
                        type = InputType.radio
                        id = option.id
                        name = this@BigRadio.groupName
                        value = option.id
                        if (id == this@BigRadio.selectedId) {
                            checked = true
                        }
                    }
                    label("inline-flex items-center justify-between w-full p-5 text-gray-500 bg-white border " +
                            "border-gray-200 rounded-lg cursor-pointer dark:hover:text-gray-300 dark:border-gray-700 " +
                            "dark:peer-checked:text-blue-500 peer-checked:border-blue-600 peer-checked:text-blue-600 " +
                            "hover:text-gray-600 hover:bg-gray-100 dark:text-gray-400 dark:bg-gray-800 " +
                            "dark:hover:bg-gray-700") {

                        htmlFor = option.id
                        div("block") {
                            div("w-full text-lg font-semibold") {
                                +option.shortDesc
                            }
                            div("w-full") {
                                +option.longDesc
                            }
                        }
                        arrowRightIcon("w-5 h-5 ms-3 rtl:rotate-180")
                    }
                }

            }
        }
    }
}

data class RadioOption(val id: String, val shortDesc: String, val longDesc: String, val action: HTMXAction? = null)

fun FlowContent.bigRadio(heading: String, groupName: String, selectedId: String, options: List<RadioOption>) =
    BigRadio(heading, groupName, selectedId, options, consumer).visit {
        render()
    }