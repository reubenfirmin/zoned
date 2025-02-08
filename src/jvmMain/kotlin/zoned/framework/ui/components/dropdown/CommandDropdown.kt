package zoned.framework.ui.components.dropdown

import kotlinx.html.*
import zoned.framework.ui.components.buttons.ButtonType.DROPDOWN
import zoned.framework.ui.components.buttons.HTMXAction
import zoned.framework.ui.components.buttons.WithFlowbiteAttributes
import zoned.framework.ui.components.buttons.ibutton
import zoned.framework.ui.components.icons.dropdownIcon
import zoned.framework.ui.libs.onClick

/**
 * A dropdown with groups of commands separated by horizontal bars. If surrounded by a form it will of course pick
 * up all the params when it posts to the various targets.
 */
class CommandDropdown(val id: String,
                      val label: String,
                      val menu: Menu,
                      consumer: TagConsumer<*>):
    SPAN(mapOf(), consumer) {

    fun render(dropdownBlock: CommandDropdown.() -> Unit) {
        ibutton(
            DROPDOWN, label, WithFlowbiteAttributes(mapOf("data-dropdown-toggle" to id)),
            id = "${id}Button",
            prelabel = {
                this@CommandDropdown.dropdownBlock()
            },
            postlabel = {
                dropdownIcon("ml-2 w-5 h-5")
            })

        div("hidden z-10 w-44 bg-white rounded divide-y divide-gray-100 shadow dark:bg-gray-700 dark:divide-gray-600") {
            id = this@CommandDropdown.id
            this@CommandDropdown.menu.commands.forEach { group ->
                ul("py-1") {
                    attributes["aria-labelledby"] = "${this@CommandDropdown.id}Button"
                    group.forEach { command ->
                        li {
                            a(classes = "block py-2 px-4 text-sm text-gray-700 hover:bg-gray-100 " +
                                    "dark:hover:bg-gray-600 dark:text-gray-200 dark:hover:text-white") {
                                if (command.action != null) {
                                    onClick(command.action)
                                }
                                href = "#"

                                +command.label
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Block is for an optional icon before the label
 */
fun FlowContent.commandDropdown(id: String, label: String, menu: Menu, block: CommandDropdown.() -> Unit = {}) {
    CommandDropdown(id, label, menu, consumer).visit {
        render(block)
    }
}

data class Command(val label: String, val action: HTMXAction? = null)

data class Menu(val commands: List<List<Command>>)