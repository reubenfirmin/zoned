package zoned.framework.ui.libs.components.htmx

import kotlinx.html.*

class HTMXParamLink(styleClasses: String = "",
                    val path: String,
                    val elementTarget: String,
                    val params: Map<String, String>,
                    val attrs: Map<String, String>,
                    consumer: TagConsumer<*>): DIV(mapOf("class" to styleClasses), consumer) {

    fun render(block: HTMXParamLink.() -> Unit) {
        params.entries.forEach {
            input {
                type = InputType.hidden
                name = it.key
                value = it.value
            }
        }
        a {
            attributes["hx-post"] = this@HTMXParamLink.path
            attributes["hx-trigger"] = "click"
            attributes["hx-target"] = this@HTMXParamLink.elementTarget
            attributes["hx-swap"] = "innerHTML"
            this@HTMXParamLink.attrs.forEach {
                attributes[it.key] = it.value
            }
            this@HTMXParamLink.block()
        }
    }
}

fun FlowContent.htmxParamLink(path: String,
                              target: String,
                              options: WithOptions,
                              block: HTMXParamLink.() -> Unit) {
    form {
        htmxParamLink(path, target, options, block)
    }
}

fun FORM.htmxParamLink(path: String,
                       target: String,
                       options: WithOptions,
                       block: HTMXParamLink.() -> Unit) {
    HTMXParamLink(options.classes, path, target, options.params, options.attrs, consumer).visit {
        render(block)
    }
}
