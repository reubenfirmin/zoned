package zoned.framework.ui.components.inputs

import kotlinx.html.*
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.layouts.ResponseContext
import zoned.framework.ui.libs.onClick
import kotlin.reflect.KProperty1

class Checkbox(
    private val selected: Boolean,
    private val formName: String,
    private val label: String,
    private val action: HTMXAction? = null,
    consumer: TagConsumer<*>): LABEL(mapOf("class" to "relative inline-flex items-center cursor-pointer"), consumer) {

    fun render() {
        input(classes = "sr-only peer") {
            if (this@Checkbox.action != null) {
                onClick(this@Checkbox.action)
            }

            type = InputType.checkBox
            name = this@Checkbox.formName
            value = "true"
            checked = this@Checkbox.selected
        }
        div("w-11 h-6 bg-gray-200 rounded-full peer peer-focus:ring-4 peer-focus:ring-blue-300 " +
                "dark:peer-focus:ring-blue-800 dark:bg-gray-700 peer-checked:after:translate-x-full " +
                "rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white " +
                "after:content-[''] after:absolute after:top-0.5 after:start-[2px] after:bg-white " +
                "after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 " +
                "after:transition-all dark:border-gray-600 peer-checked:bg-blue-600") {
        }
        span("ms-3 text-sm font-medium text-gray-900 dark:text-gray-300") {
            +this@Checkbox.label
        }
    }
}

fun <T> FlowContent.toggle(property: KProperty1<T, Boolean?>, label: String, context: ResponseContext<T>) =
    with (context.entity) {
        val selected = if (this != null) {
            property.get(this) ?: false
        } else {
            false
        }
        Checkbox(selected, property.name, label, null, consumer).visit { render() }
    }

fun <T> FlowContent.toggle(property: KProperty1<T, Boolean?>, label: String, context: T, action: HTMXAction) =
    with (context) {
        val selected = if (this != null) {
            property.get(this) ?: false
        } else {
            false
        }
        Checkbox(selected, property.name, label, action, consumer).visit { render() }
    }