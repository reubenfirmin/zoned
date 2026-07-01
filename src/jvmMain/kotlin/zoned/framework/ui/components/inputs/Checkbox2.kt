package org.example.zoned.framework.ui.components.inputs

import kotlinx.html.*
import kotlinx.html.DIV
import kotlinx.html.TagConsumer
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.components.html
import zoned.framework.ui.libs.onClick
import java.util.*

class Checkbox2(val label: String, val formName: String, val action: HTMXAction?, consumer: TagConsumer<*>):
    DIV(mapOf("class" to "flex items-center mb-4 gap-4"), consumer) {

    fun render() {
        val uniq = UUID.randomUUID().toString()
        input(classes = "w-4 h-4 text-primary-600 bg-gray-100 border-gray-300 rounded focus:ring-primary-500 " +
                "dark:focus:ring-primary-600 dark:ring-offset-gray-800 focus:ring-2 dark:bg-gray-700 dark:border-gray-600") {
            if (this@Checkbox2.action != null) {
                onClick(this@Checkbox2.action)
            }

            id = uniq
            name = this@Checkbox2.formName
            type = InputType.checkBox
            value = ""
        }
        label("text-sm font-medium text-gray-900 dark:text-gray-300") {
            htmlFor = uniq
            +this@Checkbox2.label
        }
    }
}

fun html.checkbox(label: String, formName: String, action: HTMXAction?) {
    Checkbox2(label, formName, action, consumer).visit {
        render()
    }
}