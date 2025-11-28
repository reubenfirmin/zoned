package zoned.framework.interop

import web.dom.Document
import web.html.HTMLElement
import web.html.HTMLFormElement

/**
 * External declaration for document.execCommand.
 * While deprecated, execCommand is still the standard way to implement
 * rich text editing with contenteditable elements.
 */
external interface DocumentExecCommand {
    fun execCommand(command: String, showUI: Boolean = definedExternally, value: String = definedExternally): Boolean
}

/**
 * Execute a formatting command on the document.
 * Wraps the deprecated but widely-used execCommand API.
 *
 * @param command The command to execute (e.g., "bold", "italic", "insertUnorderedList")
 * @param value Optional value for commands that require it
 * @return true if the command was successful
 */
fun Document.execCommand(command: String, value: String = ""): Boolean {
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    return (this as DocumentExecCommand).execCommand(command, false, value)
}

/**
 * Find the closest ancestor form element.
 * Type-safe wrapper for element.closest("form").
 */
fun HTMLElement.closestForm(): HTMLFormElement? {
    return closest("form") as? HTMLFormElement
}
