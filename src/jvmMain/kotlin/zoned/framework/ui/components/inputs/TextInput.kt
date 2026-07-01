package zoned.framework.ui.components.inputs

import kotlinx.html.*
import zoned.framework.ui.components.buttons.ButtonAction
import zoned.framework.ui.components.buttons.ButtonStyle
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.components.buttons.ibutton
import zoned.framework.ui.components.html
import zoned.framework.ui.libs.onKeyPress

class TextInput(
    private val clzzes: String?,
    private val formName: String,
    private val placeHolder: String?,
    private val action: ButtonAction?,
    private val buttonLabel: String?,
    val value: String?,
    private val autofocus: Boolean,
    private val inset: (() -> Unit)?, consumer: TagConsumer<*>): DIV(mapOf("class" to ""), consumer) {

    private val defaultClasses = "block w-full p-4 text-sm text-gray-900 border border-gray-300 rounded-lg bg-gray-50 " +
            "focus:ring-primary-500 focus:border-primary-500 dark:bg-gray-700 dark:border-gray-600 " +
            "dark:placeholder-gray-400 dark:text-white dark:focus:ring-primary-500 dark:focus:border-primary-500"

    fun FlowContent.render() {
        val inset = this@TextInput.inset != null

        if (inset) {
            div("absolute inset-y-0 start-0 flex items-center ps-3 pointer-events-none") {
                this@TextInput.inset.invoke()
            }
        }

        val style = this@TextInput.clzzes ?: this@TextInput.defaultClasses

        input(classes = style + if (inset) { " ps-10" } else {""}) {
            type = InputType.text
            id = "tagInput"
            with (this@TextInput.action) {
                if (this != null && this is HTMXAction) {
                    val action = this
                    onKeyPress(action, "Enter")
                }
            }
            if (this@TextInput.autofocus) {
                autoFocus = true
            }

            name = this@TextInput.formName
            value = this@TextInput.value ?: ""
            placeholder = this@TextInput.placeHolder ?: ""
        }
        if (this@TextInput.buttonLabel != null && this@TextInput.action != null) {
            ibutton(ButtonStyle.PRIMARY, this@TextInput.buttonLabel, this@TextInput.action)
        }
    }
}

fun html.input(classes: String? = null, formName: String, placeHolder: String? = null, action: ButtonAction? = null,
               buttonLabel: String? = null, value: String? = null, autofocus: Boolean = false, inset: (() -> Unit)? = null) =
    TextInput(classes, formName, placeHolder, action, buttonLabel, value, autofocus, inset, consumer).visit {
        render()
    }
