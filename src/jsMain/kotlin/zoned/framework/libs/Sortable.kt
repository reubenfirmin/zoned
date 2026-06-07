@file:JsModule("sortablejs")
@file:JsNonModule
package zoned.framework.libs

import web.dom.Element
import web.geometry.DOMRect


@JsName("default")
external object Sortable {
    fun create(el: Element, options: SortableOptions)
    /** Register a plugin (e.g. MultiDrag) on the shared Sortable singleton. */
    fun mount(vararg plugins: Any)
    val utils: SortableUtils
}

/** Sortable.utils helpers exposed by the library. */
external interface SortableUtils {
    /** Programmatically add [el] to the MultiDrag selection. */
    fun select(el: Element)
    /** Programmatically remove [el] from the MultiDrag selection. */
    fun deselect(el: Element)
}

/** The MultiDrag plugin (named export of sortablejs); pass an instance to [Sortable.mount]. */
external class MultiDrag(options: dynamic = definedExternally)

external interface DragEvent {
    val item: Element
    val to: Element
    val from: Element
    val newIndex: Int
    val oldIndex: Int
}

external interface MoveEvent {
    val dragged: Element
    val draggedRect: DOMRect
    val related: Element
    val relatedRect: DOMRect
    val willInsertAfter: Boolean
}

external interface SortableEvent {
    val item: Element
    val from: Element
    val to: Element
    val oldIndex: Int
    val newIndex: Int
    val oldDraggableIndex: Int
    val newDraggableIndex: Int
    val clone: Element
    val pullMode: String?
    /** All dragged elements when the MultiDrag plugin is active (else absent). */
    val items: Array<Element>
}

/**
 * SortableJS group options for controlling drag between lists
 */
external interface SortableGroupOptions {
    var name: String?
    var pull: Boolean?
    var put: Boolean?
}

external interface SortableOptions {
    var group: dynamic  // Can be String or SortableGroupOptions
    var sort: Boolean?
    var delay: Int?
    var delayOnTouchOnly: Boolean?
    var touchStartThreshold: Int?
    var disabled: Boolean?
    var store: Any?
    var animation: Int?
    var easing: String?
    var handle: String?
    var filter: String?
    var preventOnFilter: Boolean?
    var draggable: String?
    var dataIdAttr: String?
    var ghostClass: String?
    var chosenClass: String?
    var dragClass: String?
    var swapThreshold: Double?
    var invertSwap: Boolean?
    var invertedSwapThreshold: Double?
    var direction: String?
    var forceFallback: Boolean?
    var fallbackClass: String?
    var fallbackOnBody: Boolean?
    var fallbackTolerance: Int?
    var dragoverBubble: Boolean?
    var removeCloneOnHide: Boolean?
    var emptyInsertThreshold: Int?
    // MultiDrag plugin options (only honoured when the plugin is mounted).
    var multiDrag: Boolean?
    var selectedClass: String?
    var multiDragKey: String?
    var avoidImplicitDeselect: Boolean?
    var onChoose: ((event: SortableEvent) -> Unit)?
    var onUnchoose: ((event: SortableEvent) -> Unit)?
    var onStart: ((event: SortableEvent) -> Unit)?
    var onEnd: ((event: SortableEvent) -> Unit)?
    var onAdd: ((event: SortableEvent) -> Unit)?
    var onUpdate: ((event: SortableEvent) -> Unit)?
    var onSort: ((event: SortableEvent) -> Unit)?
    var onRemove: ((event: SortableEvent) -> Unit)?
    var onMove: ((event: MoveEvent) -> Boolean?)?
    var onClone: ((event: SortableEvent) -> Unit)?
    var onChange: ((event: SortableEvent) -> Unit)?
}