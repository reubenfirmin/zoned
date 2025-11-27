package zoned.framework.ui.enhancements

import js.objects.jso
import web.dom.Element
import web.events.Event
import web.events.EventType
import web.events.addEventListener
import web.html.HTMLElement
import web.uievents.DragEvent
import zoned.framework.libs.HTMXHelper
import zoned.framework.libs.Sortable
import zoned.framework.libs.SortableGroupOptions
import zoned.framework.libs.SortableOptions

/**
 * Client-side implementation of the Sortable enhancement.
 * This function is called by the auto-generated registry.
 */
fun makeSortableEnhancement(element: Element, config: SortableConfig) {
    val htmlElement = element as HTMLElement

    // If this is a drop target (put = true), add dragenter/dragleave handlers for visual feedback
    if (config.put) {
        var dragCounter = 0  // Track nested dragenter/dragleave events

        htmlElement.addEventListener(EventType<DragEvent>("dragenter"), { event: DragEvent ->
            event.stopPropagation()
            dragCounter++
            if (dragCounter == 1) {
                htmlElement.classList.add(config.dropTargetClass)
            }
        })

        htmlElement.addEventListener(EventType<DragEvent>("dragleave"), { event: DragEvent ->
            event.stopPropagation()
            dragCounter--
            if (dragCounter == 0) {
                htmlElement.classList.remove(config.dropTargetClass)
            }
        })

        htmlElement.addEventListener(EventType<DragEvent>("drop"), { event: DragEvent ->
            event.stopPropagation()
            dragCounter = 0
            htmlElement.classList.remove(config.dropTargetClass)
        })
    }

    val options: SortableOptions = jso {
        group = jso<SortableGroupOptions> {
            name = config.group
            pull = config.pull
            put = config.put
        }
        sort = config.sort
        animation = config.animation
        config.draggable?.let { draggable = it }
        ghostClass = config.ghostClass
        chosenClass = config.chosenClass
        dragClass = config.dragClass

        onEnd = { event ->
            // If onDropUrl is configured, POST via HTMX
            // Server response with aux targets will repaint any affected areas
            config.onDropUrl?.let { url ->
                HTMXHelper.post(
                    url = url,
                    target = config.htmxTarget ?: "body",
                    swap = config.htmxSwap ?: "innerHTML",
                    values = mapOf(
                        "itemId" to event.item.id,
                        "fromContainerId" to event.from.id,
                        "toContainerId" to event.to.id,
                        "oldIndex" to event.oldIndex.toString(),
                        "newIndex" to event.newIndex.toString()
                    )
                )
            }
        }
    }

    Sortable.create(element, options)
}
