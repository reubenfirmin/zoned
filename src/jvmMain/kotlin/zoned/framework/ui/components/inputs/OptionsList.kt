package zoned.framework.ui.components.inputs

import kotlinx.html.*
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.libs.onChange

// TODO refactor params
class OptionsList<T>(
    val classes: String,
    val label: String?,
    val options: List<T>,
    val formName: String,
    val optionLabel: (T) -> String,
    val optionValue: (T) -> String,
    val selected: (Int, T) -> Boolean,
    val showIfEmpty: Boolean,
    val includeBlank: Boolean,
    val onChangeAction: HTMXAction?,
    consumer: TagConsumer<*>): DIV(mapOf("class" to classes), consumer) {

    fun render() {
        div {
            with(this@OptionsList) {
                val optionId = formName
                if (showIfEmpty || options.isNotEmpty()) {
                    if (label != null) {
                        label("block mb-2 text-sm font-medium text-gray-900 dark:text-white") {
                            htmlFor = optionId
                            +this@OptionsList.label!!
                        }
                    }
                    select(
                        "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg " +
                                "focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 " +
                                "dark:border-gray-600 dark:placeholder-gray-400 dark:text-white " +
                                "dark:focus:ring-blue-500 dark:focus:border-blue-500"
                    ) {
                         with (this@select) {
                            if (this@OptionsList.onChangeAction != null) {
                                onChange(this@OptionsList.onChangeAction)
                            }
                        }

                        id = optionId
                        name = this@OptionsList.formName
                        if (this@OptionsList.includeBlank) {
                            option {
                                value = ""
                                +""
                            }
                        }
                        this@OptionsList.options.forEachIndexed { idx, option ->
                            option {
                                value = this@OptionsList.optionValue(option)
                                selected = this@OptionsList.selected(idx, option)
                                +this@OptionsList.optionLabel(option)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun <T> FlowContent.optionsList(
    classes: String,
    label: String?,
    options: List<T>,
    formName: String,
    optionLabel: (T) -> String,
    optionValue: (T) -> String,
    selected: (Int, T) -> Boolean = { _,_ -> false},
    showIfEmpty: Boolean = false,
    includeBlank: Boolean = false,
    onChangeAction: HTMXAction? = null) =

    OptionsList(classes, label, options, formName, optionLabel, optionValue, selected, showIfEmpty,
        includeBlank, onChangeAction, consumer).visit {
        render()
    }