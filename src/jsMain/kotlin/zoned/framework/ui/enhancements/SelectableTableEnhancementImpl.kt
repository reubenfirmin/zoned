package zoned.framework.ui.enhancements

import js.objects.jso
import kotlinx.html.TagConsumer
import kotlinx.html.div
import web.dom.Element
import web.dom.Node
import web.dom.document
import web.events.EventType
import web.events.addEventListener
import web.html.HTMLElement
import web.html.HTMLInputElement
import web.uievents.MouseEvent
import zoned.framework.dom.Ref
import zoned.framework.dom.insertChildren
import zoned.framework.dom.onMount
import zoned.framework.dom.ref
import zoned.framework.libs.HTMXHelper
import zoned.framework.libs.Sortable
import zoned.framework.libs.SortableGroupOptions
import zoned.framework.libs.SortableOptions

/**
 * Client-side implementation of the SelectableTable enhancement.
 *
 * Wraps a server-rendered table to provide row selection, multi-select, and drag-drop support.
 * Uses TagConsumer pattern: captures server-rendered children and rebuilds with DSL.
 */
@EnhancementImpl(SelectableTableEnhancement::class)
fun TagConsumer<HTMLElement>.initSelectableTableEnhancement(config: SelectableTableConfig, children: List<Node>) {
    val containerRef = Ref<HTMLElement>()

    div {
        ref(containerRef)

        // Re-insert server-rendered table
        insertChildren(children)

        onMount {
            val container = containerRef.element
            val selectedIds = mutableSetOf<String>()
            var lastClickedIndex: Int? = null

            // Get all rows
            fun getRows(): List<HTMLElement> {
                val nodeList = container.querySelectorAll(config.rowSelector)
                val result = mutableListOf<HTMLElement>()
                for (i in 0 until nodeList.length) {
                    val node = nodeList.item(i) as? HTMLElement ?: continue
                    if (node.getAttribute(config.itemIdAttribute) != null) {
                        result.add(node)
                    }
                }
                return result
            }

            // Get item ID from row
            fun getItemId(row: HTMLElement): String? = row.getAttribute(config.itemIdAttribute)

            // Update row visual state
            fun updateRowVisual(row: HTMLElement, selected: Boolean) {
                val checkbox = row.querySelector(config.checkboxSelector) as? HTMLInputElement
                checkbox?.checked = selected
                if (selected) {
                    row.classList.add(config.selectedClass)
                } else {
                    row.classList.remove(config.selectedClass)
                }
            }

            // Update select-all checkbox state
            fun updateSelectAllCheckbox() {
                val selectAll = container.querySelector(config.selectAllSelector) as? HTMLInputElement ?: return
                val rows = getRows()
                val allSelected = rows.isNotEmpty() && rows.all { row ->
                    getItemId(row)?.let { it in selectedIds } ?: false
                }
                val someSelected = selectedIds.isNotEmpty()
                selectAll.checked = allSelected
                selectAll.indeterminate = someSelected && !allSelected
            }

            // Update floating action bar
            fun updateActionBar() {
                val actionBar = config.actionBarSelector?.let { document.querySelector(it) as? HTMLElement } ?: return
                val countSpan = actionBar.querySelector(".selection-count")

                if (selectedIds.isEmpty()) {
                    actionBar.classList.add("hidden")
                } else {
                    actionBar.classList.remove("hidden")
                    countSpan?.textContent = "${selectedIds.size} selected"
                }
            }

            // Update the container's data-selected-ids attribute for context menu integration
            fun updateSelectedIdsAttribute() {
                container.setAttribute("data-selected-ids", selectedIds.joinToString(","))
            }

            // Hide any open context menu (it has stale selection info)
            fun hideContextMenu() {
                // Look for context menu by common selector pattern
                val contextMenu = document.querySelector("[id$='-context-menu']") as? HTMLElement
                contextMenu?.classList?.add("hidden")
            }

            // Toggle selection for a row
            fun toggleSelection(row: HTMLElement, forceState: Boolean? = null) {
                val itemId = getItemId(row) ?: return
                val newState = forceState ?: (itemId !in selectedIds)

                if (newState) {
                    selectedIds.add(itemId)
                } else {
                    selectedIds.remove(itemId)
                }
                updateRowVisual(row, newState)
                updateSelectAllCheckbox()
                updateActionBar()
                updateSelectedIdsAttribute()
                hideContextMenu() // Close context menu since selection changed
            }

            // Handle shift-click range selection
            fun handleRangeSelection(clickedIndex: Int) {
                val lastIndex = lastClickedIndex ?: clickedIndex
                val rows = getRows()
                val start = minOf(lastIndex, clickedIndex)
                val end = maxOf(lastIndex, clickedIndex)

                for (i in start..end) {
                    if (i < rows.size) {
                        toggleSelection(rows[i], true)
                    }
                }
            }

            // Setup checkbox click handlers
            fun setupCheckboxHandlers() {
                val rows = getRows()
                rows.forEachIndexed { index, row ->
                    val checkbox = row.querySelector(config.checkboxSelector) as? HTMLInputElement ?: return@forEachIndexed

                    checkbox.addEventListener(EventType<MouseEvent>("click"), { event: MouseEvent ->
                        event.stopPropagation()

                        if (config.shiftSelect && event.shiftKey && lastClickedIndex != null) {
                            event.preventDefault()
                            handleRangeSelection(index)
                        } else {
                            toggleSelection(row)
                        }
                        lastClickedIndex = index
                    })

                    // Also add click handler on the checkbox cell (the td that contains the checkbox)
                    val checkboxCell = checkbox.parentElement as? HTMLElement
                    checkboxCell?.addEventListener(EventType<MouseEvent>("click"), { event: MouseEvent ->
                        // Only handle if click was on the cell, not the checkbox itself
                        val target = event.target as? Element
                        if (target == checkboxCell) {
                            event.stopPropagation()
                            if (config.shiftSelect && event.shiftKey && lastClickedIndex != null) {
                                handleRangeSelection(index)
                            } else {
                                toggleSelection(row)
                            }
                            lastClickedIndex = index
                        }
                    })
                }
            }

            // Setup select-all checkbox
            fun setupSelectAllHandler() {
                val selectAll = container.querySelector(config.selectAllSelector) as? HTMLInputElement ?: return

                selectAll.addEventListener(EventType<MouseEvent>("click"), { _: MouseEvent ->
                    val rows = getRows()
                    val shouldSelectAll = selectAll.checked

                    rows.forEach { row ->
                        toggleSelection(row, shouldSelectAll)
                    }
                })
            }

            // Setup bulk action handlers
            fun setupActionBarHandlers() {
                val actionBar = config.actionBarSelector?.let { document.querySelector(it) as? HTMLElement } ?: return

                // Get menu element lazily (it might not exist at initialization)
                fun getMoveMenu(): HTMLElement? = actionBar.querySelector(".bulk-move-menu") as? HTMLElement

                // Hide menu when clicking outside
                fun hideMoveMenu() {
                    getMoveMenu()?.classList?.add("hidden")
                }

                // Show menu
                fun showMoveMenu() {
                    getMoveMenu()?.classList?.remove("hidden")
                }

                // Archive button
                val archiveBtn = actionBar.querySelector(".bulk-archive")
                archiveBtn?.addEventListener(EventType<MouseEvent>("click"), { _: MouseEvent ->
                    config.archiveUrl?.let { url ->
                        // Get folderId from action bar's data attribute
                        val folderId = actionBar.getAttribute("data-folder-id") ?: ""
                        HTMXHelper.post(
                            url = url,
                            target = config.htmxTarget ?: "body",
                            swap = "innerHTML",
                            values = mapOf("propertyIds" to selectedIds.joinToString(","), "folderId" to folderId)
                        )
                        selectedIds.clear()
                        updateActionBar()
                        updateSelectedIdsAttribute()
                    }
                })

                // Move menu button - fetch menu with propertyIds using HTMXHelper directly
                val moveBtn = actionBar.querySelector(".bulk-move") as? HTMLElement
                moveBtn?.addEventListener(EventType<MouseEvent>("click"), { event: MouseEvent ->
                    // Always prevent default to stop declarative HTMX from firing
                    event.preventDefault()
                    event.stopPropagation()

                    val folderId = actionBar.getAttribute("data-folder-id") ?: ""
                    val moveMenu = getMoveMenu()

                    // Check if menu is already visible
                    val isVisible = moveMenu?.classList?.contains("hidden") != true

                    if (isVisible) {
                        hideMoveMenu()
                    } else {
                        // Build URL with query params and fetch menu via HTMXHelper
                        config.moveMenuUrl?.let { baseUrl ->
                            val separator = if (baseUrl.contains("?")) "&" else "?"
                            val url = "$baseUrl${separator}propertyIds=${selectedIds.joinToString(",")}&folderId=$folderId"
                            val target = moveMenu?.id?.let { "#$it" } ?: return@addEventListener
                            HTMXHelper.get(url, target)
                            showMoveMenu()
                        }
                    }
                })

                // Hide menu when clicking outside
                document.addEventListener(EventType<MouseEvent>("click"), { event: MouseEvent ->
                    val target = event.target as? Element
                    // If click is not inside the action bar, hide menu
                    if (target != null && !actionBar.contains(target)) {
                        hideMoveMenu()
                    }
                })

                // Listen for clicks inside the action bar's move menu (delegated)
                actionBar.addEventListener(EventType<MouseEvent>("click"), { event: MouseEvent ->
                    val target = event.target as? Element
                    val moveMenu = getMoveMenu()
                    // If click is inside the move menu (on a button), it will trigger an HTMX request
                    // Clear selection after move is triggered
                    if (moveMenu != null && moveMenu.contains(target) && target?.tagName?.lowercase() == "button") {
                        selectedIds.clear()
                        updateActionBar()
                        hideMoveMenu()
                    }
                })
            }

            // Setup sortable drag-drop with multi-select support
            fun setupSortable() {
                if (config.onDropUrl == null && !config.dragPull) return

                // Find the actual sortable container (tbody for tables, or container itself)
                val sortableContainer = container.querySelector("tbody") as? HTMLElement ?: container
                // Adjust draggable selector - if we found tbody, rows are direct children
                val draggableSelector = if (sortableContainer != container) "tr" else config.rowSelector

                val options: SortableOptions = jso {
                    group = jso<SortableGroupOptions> {
                        name = config.dragGroup
                        pull = config.dragPull
                        put = config.dragPut
                    }
                    sort = false
                    animation = config.dragAnimation
                    draggable = draggableSelector
                    ghostClass = config.ghostClass
                    chosenClass = config.chosenClass
                    // Only allow drag from handle element if specified
                    config.dragHandle?.let { handle = it }
                    // Filter elements that should not trigger drag (links, buttons, inputs, etc.)
                    filter = config.dragFilter
                    preventOnFilter = false  // Allow clicks to fire on filtered elements

                    onStart = { event ->
                        // If dragging an unselected item, select only that item
                        val draggedId = event.item.getAttribute(config.itemIdAttribute)
                        if (draggedId != null && draggedId !in selectedIds) {
                            // Clear previous selection and select only this one
                            val rows = getRows()
                            rows.forEach { row -> updateRowVisual(row, false) }
                            selectedIds.clear()
                            selectedIds.add(draggedId)
                            updateRowVisual(event.item as HTMLElement, true)
                            updateSelectAllCheckbox()
                            updateActionBar()
                            updateSelectedIdsAttribute()
                        }
                    }

                    onEnd = { event ->
                        config.onDropUrl?.let { url ->
                            // Send all selected item IDs
                            val itemIds = if (selectedIds.isNotEmpty()) {
                                selectedIds.joinToString(",")
                            } else {
                                event.item.getAttribute(config.itemIdAttribute) ?: ""
                            }

                            // Extract folder IDs from element IDs (e.g., "property-rows-abc123" -> "abc123", "folder-xyz456" -> "xyz456")
                            fun extractFolderId(elementId: String): String {
                                return when {
                                    elementId.startsWith("property-rows-") -> elementId.removePrefix("property-rows-")
                                    elementId.startsWith("folder-") -> elementId.removePrefix("folder-")
                                    else -> elementId
                                }
                            }

                            HTMXHelper.post(
                                url = url,
                                target = config.htmxTarget ?: "body",
                                swap = "innerHTML",
                                values = mapOf(
                                    "propertyIds" to itemIds,
                                    "sourceFolderId" to extractFolderId(event.from.id),
                                    "targetFolderId" to extractFolderId(event.to.id)
                                )
                            )
                        }
                    }
                }

                Sortable.create(sortableContainer, options)
            }

            // Initialize everything
            setupCheckboxHandlers()
            setupSelectAllHandler()
            setupActionBarHandlers()
            setupSortable()

            // Initialize visual state
            updateSelectAllCheckbox()
            updateActionBar()
        }
    }
}
