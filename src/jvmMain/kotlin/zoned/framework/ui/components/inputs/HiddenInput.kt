package zoned.framework.ui.components.inputs

import kotlinx.html.*
import zoned.framework.db.FormObject
import zoned.framework.ui.components.html
import kotlin.reflect.KProperty1

class HiddenInput(private val propName: String,
                  private val propVal: String,
                  private val elId: String?, consumer: TagConsumer<*>): INPUT(mapOf(), consumer) {

    fun render() {
        type = InputType.hidden
        name = propName
        if (elId != null) {
            id = this@HiddenInput.elId
        }
        value = propVal
    }
}

fun html.hiddenInput(name: String, value: String?, id: String? = null) {
    if (value != null) {
        HiddenInput(name, value, id, consumer).visit {
            render()
        }
    }
}

fun <T:FormObject> html.hiddenInput(prop: KProperty1<T, *>, instance: T) {
    hiddenInput(prop.name, prop.get(instance)?.toString() ?: "")
}