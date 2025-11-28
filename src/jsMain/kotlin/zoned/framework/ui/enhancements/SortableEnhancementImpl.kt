package zoned.framework.ui.enhancements

import js.objects.jso
import kotlinx.html.TagConsumer
import kotlinx.html.div
import web.dom.Node
import web.events.EventType
import web.events.addEventListener
import web.html.HTMLElement
import web.uievents.DragEvent
import zoned.framework.dom.Ref
import zoned.framework.dom.insertChildren
import zoned.framework.dom.onMount
import zoned.framework.dom.ref
import zoned.framework.libs.HTMXHelper
import zoned.framework.libs.Sortable
import zoned.framework.libs.SortableGroupOptions
import zoned.framework.libs.SortableOptions

/**
 * Client-side implementation of the Sortable enhancement.
 * Wraps content to make it sortable or a drop target.
 *
 * Uses TagConsumer pattern: captures server-rendered children and rebuilds with DSL.
 */
@EnhancementImpl(SortableEnhancement::class)
fun TagConsumer<HTMLElement>.initSortableEnhancement(config: SortableConfig, children: List<Node>) {
    val containerRef = Ref<HTMLElement>()

    div {
        // Set container ID FIRST - must be before ref() so ref uses this ID for behavior lookup
        config.containerId?.let { attributes["id"] = it }
        ref(containerRef)

        // Re-insert server-rendered children (the sortable items)
        insertChildren(children)

        onMount {
            val container = containerRef.element

            // If this is a drop target (put = true), add dragenter/dragleave handlers for visual feedback
            if (config.put) {
                var dragCounter = 0  // Track nested dragenter/dragleave events

                container.addEventListener(EventType<DragEvent>("dragenter"), { event: DragEvent ->
                    event.stopPropagation()
                    dragCounter++
                    if (dragCounter == 1) {
                        container.classList.add(config.dropTargetClass)
                    }
                })

                container.addEventListener(EventType<DragEvent>("dragleave"), { event: DragEvent ->
                    event.stopPropagation()
                    dragCounter--
                    if (dragCounter == 0) {
                        container.classList.remove(config.dropTargetClass)
                    }
                })

                container.addEventListener(EventType<DragEvent>("drop"), { event: DragEvent ->
                    event.stopPropagation()
                    dragCounter = 0
                    container.classList.remove(config.dropTargetClass)
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
                // Filter elements that should not trigger drag (links, buttons, inputs, etc.)
                filter = config.filter
                preventOnFilter = false  // Allow clicks to fire on filtered elements

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

            Sortable.create(container, options)
        }
    }
}
