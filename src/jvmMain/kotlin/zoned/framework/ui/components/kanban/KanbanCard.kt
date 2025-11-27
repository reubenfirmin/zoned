package zoned.framework.ui.components.kanban

import zoned.framework.ui.components.badge
import zoned.framework.ui.components.buttons.ButtonStyle.ICON_BUTTON
import zoned.framework.ui.components.buttons.ibutton
import zoned.framework.ui.components.icons.clockIcon
import zoned.framework.ui.components.icons.expandIcon
import kotlinx.html.DIV
import kotlinx.html.TagConsumer
import kotlinx.html.*

class KanbanCard(val title: String, val elementId: String, val navigationAction: zoned.framework.ui.components.buttons.ButtonAction, consumer: TagConsumer<*>):

    DIV(mapOf("class" to "flex flex-col p-5 max-w-md bg-white rounded-lg shadow transform cursor-move " +
            "dark:bg-gray-800"), consumer) {

    fun render(cardStatus: KanbanCard.() -> Unit, block: KanbanCard.() -> Unit) {
        id = elementId
        div("flex justify-between items-center pb-4") {
            div("text-base font-semibold text-gray-900 dark:text-white") {
                +this@KanbanCard.title
            }
            ibutton(ICON_BUTTON, null, this@KanbanCard.navigationAction, prelabel = {
                expandIcon("w-5 h-5")
            })
        }
        div("flex flex-col") {
            div("pb-4 text-sm font-normal text-gray-700 dark:text-gray-400") {
                this@KanbanCard.block()
            }

            div("flex justify-between") {
                div {
                    // e.g. user icons here
                    this@KanbanCard.cardStatus()
                }
                // TODO
                badge {
                    clockIcon("mr-1 w-4 h-4")
                    +"""5 days left"""
                }
            }
        }
    }
}

fun Column.card(title: String,
                id: String,
                navigationAction: zoned.framework.ui.components.buttons.ButtonAction,
                cardStatus: KanbanCard.() -> Unit = {},
                block: KanbanCard.() -> Unit) {
    KanbanCard(title, id, navigationAction, consumer).visit {
        render(cardStatus, block)
    }
}