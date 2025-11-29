package zoned.framework.libs

import js.objects.unsafeJso
import web.dom.Element
import web.dom.ElementId
import web.dom.document

data class SortableConfig(
    val elementId: ElementId,
    val group: String = "default",
    val animation: Int = 150,
    val draggable: String? = null,
    val ghostClass: String = "sortable-ghost",
    val chosenClass: String = "sortable-chosen",
    val dragClass: String = "sortable-drag",
    val onEnd: (SortableDropEvent) -> Unit
)

data class SortableDropEvent(
    val itemId: ElementId,
    val fromId: ElementId,
    val toId: ElementId,
    val oldIndex: Int,
    val newIndex: Int,
    val item: Element,
    val from: Element,
    val to: Element
)

fun makeSortable(config: SortableConfig) {
    val element = document.getElementById(config.elementId) ?: run {
        console.warn("Could not find element: ${config.elementId}")
        return
    }

    val options: SortableOptions = unsafeJso {
        group = config.group
        animation = config.animation
        config.draggable?.let { draggable = it }
        ghostClass = config.ghostClass
        chosenClass = config.chosenClass
        dragClass = config.dragClass
        onEnd = { event ->
            config.onEnd(SortableDropEvent(
                itemId = event.item.id,
                fromId = event.from.id,
                toId = event.to.id,
                oldIndex = event.oldIndex,
                newIndex = event.newIndex,
                item = event.item,
                from = event.from,
                to = event.to
            ))
        }
    }

    Sortable.create(element, options)
}
