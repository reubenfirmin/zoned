package zoned.framework.ui.components.inputs

import kotlinx.html.*

class Toggle(val classes: String, val label: String, val formName: String, val formValue: String, val checked: Boolean, consumer: TagConsumer<*>):
    LABEL(mapOf("class" to "inline-flex items-center cursor-pointer"), consumer) {

    fun render() {

        input(classes = "sr-only peer") {
            type = InputType.checkBox
            name = this@Toggle.formName
            value = this@Toggle.formValue
            if (this@Toggle.checked) {
                checked = true
            }
        }

        div("relative w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 " +
                "dark:peer-focus:ring-blue-800 rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full " +
                "rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] " +
                "after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border " +
                "after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 " +
                "peer-checked:bg-blue-600") {
        }
        span("ms-3 text-sm font-medium text-gray-900 dark:text-gray-300") {
            +this@Toggle.label
        }
    }
}

fun FlowContent.toggle(classes: String, label: String, formName: String, formValue: String, checked: Boolean) =
    Toggle(classes, label, formName, formValue, checked, consumer).visit {
    render()
}