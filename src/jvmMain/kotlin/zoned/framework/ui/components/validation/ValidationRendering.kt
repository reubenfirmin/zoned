package zoned.framework.ui.components.validation

import kotlinx.html.FlowContent
import kotlinx.html.div
import zoned.framework.form.Validation

fun errorClass(classes: String, validation: Validation?) =
    if (validation != null && !validation.valid) {
        "$classes border-rose-500 border-2 p-2 m-2"
    } else {
        classes
    }

fun FlowContent.errorMessage(validation: Validation?) {
    if (validation != null && !validation.valid) {
        div {
            +validation.message!!
        }
    }
}