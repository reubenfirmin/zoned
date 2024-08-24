package zoned.framework.ui.components.buttons

import io.javalin.http.Context
import kotlinx.html.*
import kotlinx.html.ButtonType.*
import zoned.framework.api.Response
import zoned.framework.api.Parameterizer
import zoned.framework.ui.components.buttons.ButtonClasses.dropdownComponentClasses
import zoned.framework.ui.components.buttons.ButtonClasses.highlight1Classes
import zoned.framework.ui.components.buttons.ButtonClasses.highlight2Classes
import zoned.framework.ui.components.buttons.ButtonClasses.iconButtonClasses
import zoned.framework.ui.components.buttons.ButtonClasses.primaryClasses
import zoned.framework.ui.components.buttons.ButtonClasses.secondaryClasses
import zoned.framework.ui.libs.HTMX
import zoned.framework.ui.libs.onClick
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1

/**
 * This component has two "blocks" on either side of the label, to allow adding icons on either side.
 */
class RcpButton(type: ButtonType, val label: String?, val action: ButtonAction, val enabled: Boolean, val elementId: String?,
                consumer: TagConsumer<*>):
    BUTTON(mapOf("class" to type.classes), consumer) {

    fun render(prelabel: RcpButton.() -> Unit, postlabel: RcpButton.() -> Unit) {

        fun buttonUi() {
            prelabel()
            if (label != null) {
                +label
            }
            postlabel()
        }

        type = button

        if (elementId != null) {
            id = elementId
        }

        disabled = !enabled
        var behavior: ButtonAction? = action
        while (behavior != null) {
            attachBehavior(behavior)
            behavior = behavior.nextAction
        }
        buttonUi()
    }

    private fun attachBehavior(behavior: ButtonAction) {
        when (behavior) {
            is HTMXAction -> {
                onClick(behavior)
            }

            is WithFlowbiteAttributes -> {
                behavior.attrs.forEach {
                    attributes[it.key] = it.value
                }
            }

            is WithSubmitAction -> {
                type = submit
            }

            is WithNoAction -> {
            }
        }
    }
}

object ButtonClasses {
    const val primaryClasses = "text-white bg-blue-700 hover:bg-blue-800 focus:ring-4 focus:ring-blue-300 " +
            "font-medium rounded-lg text-sm px-5 py-2.5 dark:bg-blue-600 " +
            "dark:hover:bg-blue-700 focus:outline-none dark:focus:ring-blue-800"
    const val secondaryClasses = "py-2.5 px-5 text-sm font-medium text-gray-900 focus:outline-none " +
            "bg-white rounded-lg border border-gray-200 hover:bg-gray-100 hover:text-blue-700 " +
            "focus:z-10 focus:ring-4 focus:ring-gray-200 dark:focus:ring-gray-700 " +
            "dark:bg-gray-800 dark:text-gray-400 dark:border-gray-600 dark:hover:text-white " +
            "dark:hover:bg-gray-700"
    const val highlight1Classes = "text-white bg-gradient-to-r from-blue-500 via-blue-600 to-blue-700 " +
            "hover:bg-gradient-to-br focus:ring-4 focus:outline-none focus:ring-blue-300 " +
            "dark:focus:ring-blue-800 font-medium rounded-lg text-sm px-5 py-2.5 text-center"
    const val highlight2Classes = "text-white bg-gradient-to-r from-green-400 via-green-500 to-green-600 " +
            "hover:bg-gradient-to-br focus:ring-4 focus:outline-none focus:ring-green-300 " +
            "dark:focus:ring-green-800 font-medium rounded-lg text-sm px-5 py-2.5 text-center"
    const val iconButtonClasses = "flex items-center justify-center text-white hover:bg-primary-800 " +
            "focus:ring-4 focus:ring-primary-300 font-medium rounded-lg text-sm px-4 py-2 " +
            "dark:hover:bg-primary-700 focus:outline-none dark:focus:ring-primary-800"
    const val dropdownComponentClasses = "w-full md:w-auto flex items-center justify-center py-2 px-4 text-sm " +
            "font-medium text-gray-900 focus:outline-none bg-white rounded-lg border border-gray-200 " +
            "hover:bg-gray-100 hover:text-primary-700 focus:z-10 focus:ring-4 focus:ring-gray-200 " +
            "dark:focus:ring-gray-700 dark:bg-gray-800 dark:text-gray-400 dark:border-gray-600 dark:hover:text-white " +
            "dark:hover:bg-gray-700"
}

fun FlowContent.ibutton(
    buttonType: ButtonType,
    label: String? = null,
    action: ButtonAction,
    enabled: Boolean = true,
    id: String? = null,
    prelabel: RcpButton.() -> Unit = {},
    postlabel: RcpButton.() -> Unit = {}) {

    RcpButton(buttonType, label, action, enabled, id, consumer).visit {
        render(prelabel, postlabel)
    }
}

abstract class ButtonAction {
    var nextAction: ButtonAction? = null

    // TODO this is only supported properly by button
    fun after(next: ButtonAction): ButtonAction {
        next.nextAction = this
        return next
    }
}

data class WithFlowbiteAttributes(val attrs: Map<String, String>): ButtonAction()

// TODO these aren't all ButtonActions. Make into a sealed class maybe?
/**
 * If parameterizer supplied, button must take id
 */
data class HTMXAction(val handler: KFunction<Response>,
                      val parameterizer: Parameterizer? = null,
                      val includeSelector: String? = null,
                      val swap: HTMX.Swap? = null,
                      val swapDelay: Int? = null,
                      val target: String? = null): ButtonAction()

data object WithNoAction: ButtonAction()

data object WithSubmitAction: ButtonAction()

enum class ButtonType(val classes: String) {
    /** A cta, form submit, etc. There should generally only be one of these on a page */
    PRIMARY(primaryClasses),
    /** A secondary, form cancel, etc. There can be many of these */
    SECONDARY(secondaryClasses),
    /** A special button */
    HIGHLIGHT1(highlight1Classes),
    /** A second type of special button */
    HIGHLIGHT2(highlight2Classes),
    /** A button with an icon (must be inserted in block) and a hidden border */
    ICON_BUTTON(iconButtonClasses),
    /** A button which is intended to launch a dropdown */
    DROPDOWN(dropdownComponentClasses)
}