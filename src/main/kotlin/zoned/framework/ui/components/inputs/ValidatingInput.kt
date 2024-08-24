package zoned.framework.ui.components.inputs

import kotlinx.html.*
import zoned.framework.api.BaseRoute
import zoned.framework.db.FormObject
import zoned.framework.form.FormName
import zoned.framework.form.FormValidation
import zoned.framework.form.Validation
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.components.validation.errorClass
import zoned.framework.ui.components.validation.errorMessage
import zoned.framework.ui.layouts.ResponseContext
import zoned.framework.ui.libs.HTMX.htmxOnEvent
import zoned.framework.ui.libs.onKeypress
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

class ValidatingInput(
    classes: String,
    private val label: String?,
    private val placeholder: String?,
    private val formName: String,
    private val defaultValue: String?,
    private val validation: Validation?,
    private val insetLeft: String?,
    private val numeric: Boolean,
    private val autofocus: Boolean,
    private val keypressHandler: BaseRoute?,
    private val keypressAction: HTMXAction?,
    consumer: TagConsumer<*>): DIV(mapOf("class" to errorClass(classes, validation)), consumer) {

    fun FlowContent.render() {
        val inputId = this@ValidatingInput.formName

        if (this@ValidatingInput.label != null) {
            label("block mb-2 text-sm font-medium text-gray-900 dark:text-white") {
                htmlFor = inputId
                +this@ValidatingInput.label
            }
        }

        val hasInsetLeft = this@ValidatingInput.insetLeft != null

        div(if (hasInsetLeft) { "relative " } else {""}) {
            if (hasInsetLeft) {
                div("pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3") {
                    span("text-gray-500 sm:text-sm") {
                        +this@ValidatingInput.insetLeft!!
                    }
                }
            }
            input(classes = "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg " +
                    "focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 " +
                    "dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 " +
                    "dark:focus:border-blue-500" + if (hasInsetLeft) { " pl-7 block" } else { "" }) {

                with (this@ValidatingInput.keypressHandler) {
                    if (this != null) {
                        htmxOnEvent("keyup", url(), method = method)
                    }
                }
                if (this@ValidatingInput.autofocus) {
                    autoFocus = true
                }

                if (this@ValidatingInput.keypressAction != null) {
                    this@input.onKeypress(this@ValidatingInput.keypressAction)
                }

                type = if (this@ValidatingInput.numeric) { InputType.number } else { InputType.text }
                id = inputId
                name = this@ValidatingInput.formName
                placeholder = this@ValidatingInput.placeholder ?: ""
                value = this@ValidatingInput.defaultValue ?: ""
                required = true
            }
        }
        errorMessage(this@ValidatingInput.validation)
    }
}

// deprecated
fun FlowContent.vinput(classes: String,
                       label: String?,
                       placeholder: String,
                       formName: String,
                       defaultValue: String?,
                       validation: Validation?,
                       insetLeft: String? = null,
                       numeric: Boolean = false) =
    ValidatingInput(classes, label, placeholder, formName, defaultValue, validation, insetLeft, numeric, false,
        null, null, consumer).visit {
            render()
    }

inline fun <reified T: FormObject> FlowContent.vinput(
                    labelAndPlaceholder: LabelAndPlaceHolder,
                    prop: KProperty1<T, *>,
                    resp: ResponseContext<T>,
                    insetLeft: String? = null,
                    numeric: Boolean = false,
                    keypressHandler: BaseRoute? = null) {

    vinput(null, labelAndPlaceholder, prop, resp, insetLeft, numeric, false, keypressHandler)
 }

inline fun <reified T: FormObject> FlowContent.vinput2(
    labelAndPlaceholder: LabelAndPlaceHolder,
    prop: KProperty1<T, *>,
    validation: FormValidation<T>?,
    entity: T,
    classes: String = "",
    insetLeft: String? = null,
    numeric: Boolean = false,
    keypressAction: HTMXAction? = null) {

    val formNameOverride = prop.findAnnotation<FormName>()
    val formName = formNameOverride?.name ?: prop.name

    ValidatingInput(classes, labelAndPlaceholder.label, labelAndPlaceholder.placeholder, formName, prop.get(entity)?.toString(),
        validation?.validation(prop), insetLeft, numeric, false, null, keypressAction, consumer)
        .visit {
            render()
        }
}

inline fun <reified T: FormObject> FlowContent.vinput(
                    classes: String?,
                    labelAndPlaceholder: LabelAndPlaceHolder,
                    prop: KProperty1<T, *>,
                    resp: ResponseContext<T>,
                    insetLeft: String? = null,
                    numeric: Boolean = false,
                    autofocus: Boolean = false,
                    keypressHandler: BaseRoute? = null) {
    val formNameOverride = prop.findAnnotation<FormName>()
    val formName = formNameOverride?.name ?: prop.name

    ValidatingInput(classes  ?: "", labelAndPlaceholder.label, labelAndPlaceholder.placeholder, formName, resp.defaultValue(prop),
        resp.validation(T::class)?.validation(prop), insetLeft, numeric, autofocus, keypressHandler, null, consumer)
        .visit {
            render()
        }
}

data class LabelAndPlaceHolder(val label: String?, val placeholder: String?) {

    companion object {
        fun placeholder(str: String) = LabelAndPlaceHolder(null, str)

        fun inputLabel(str: String) = LabelAndPlaceHolder(str, null)

        fun labelPlaceholder(label: String, placeholder: String) = LabelAndPlaceHolder(label, placeholder)

        fun noLabelPlaceholder() = LabelAndPlaceHolder(null, null)
    }
}