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
    // Defaults match Sortable's own defaults (no behaviour change for existing callers).
    // forceFallback + fallbackOnBody use Sortable's managed mirror (lifted to <body>) instead
    // of the native HTML5 drag image — smoother across nested scroll/grid containers.
    // emptyInsertThreshold is the px distance for inserting into a (sparse/empty) container.
    val forceFallback: Boolean = false,
    val fallbackOnBody: Boolean = false,
    val fallbackTolerance: Int = 0,
    val emptyInsertThreshold: Int = 5,
    // MultiDrag: when true, the plugin is mounted and elements that are selected (via
    // [sortableSelect]) drag together. [multiDragKey] gates click-to-select — set it to a
    // modifier (e.g. "CTRL") so plain clicks on items keep their normal behaviour and
    // selection is driven only programmatically from [onChoose].
    val multiDrag: Boolean = false,
    val selectedClass: String = "sortable-selected",
    val multiDragKey: String? = null,
    // Fired when an item is grabbed (before the drag starts); the chosen element is passed so
    // callers can select related items to drag them as a group.
    val onChoose: ((Element) -> Unit)? = null,
    // Fired once the drag actually starts (mirror + placeholder exist); the dragged element is
    // passed so callers can decorate the drag visuals.
    val onStart: ((Element) -> Unit)? = null,
    // Per-relocation veto: Sortable calls this every time it wants to move its placeholder within
    // (or into) this container; return false to cancel — the placeholder stays put and the layout
    // does not reflow. Lets callers freeze sorting while a drop is routed elsewhere (e.g. an
    // overlay/portal target is active) without disabling the drag itself.
    val onMove: ((MoveEvent) -> Boolean)? = null,
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

/** Mounted lazily so the MultiDrag plugin is registered exactly once across all sortables. */
private var multiDragMounted = false

private fun ensureMultiDragMounted() {
    if (!multiDragMounted) {
        Sortable.mount(MultiDrag())
        multiDragMounted = true
    }
}

/** Add [el] to the MultiDrag selection so it drags together with other selected elements. */
fun sortableSelect(el: Element) = Sortable.utils.select(el)

/** Remove [el] from the MultiDrag selection. */
fun sortableDeselect(el: Element) = Sortable.utils.deselect(el)

fun makeSortable(config: SortableConfig) {
    val element = document.getElementById(config.elementId) ?: run {
        console.warn("Could not find element: ${config.elementId}")
        return
    }

    if (config.multiDrag) ensureMultiDragMounted()

    val options: SortableOptions = unsafeJso {
        group = config.group
        animation = config.animation
        config.draggable?.let { draggable = it }
        ghostClass = config.ghostClass
        chosenClass = config.chosenClass
        dragClass = config.dragClass
        forceFallback = config.forceFallback
        fallbackOnBody = config.fallbackOnBody
        fallbackTolerance = config.fallbackTolerance
        emptyInsertThreshold = config.emptyInsertThreshold
        if (config.multiDrag) {
            multiDrag = true
            selectedClass = config.selectedClass
            config.multiDragKey?.let { multiDragKey = it }
            // Keep our programmatic selection intact when a drag starts on an unselected item.
            avoidImplicitDeselect = true
        }
        config.onChoose?.let { cb -> onChoose = { event -> cb(event.item) } }
        config.onStart?.let { cb -> onStart = { event -> cb(event.item) } }
        config.onMove?.let { cb -> onMove = { event -> cb(event) } }
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
