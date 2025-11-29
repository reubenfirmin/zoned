package zoned.framework.libs

import js.objects.unsafeJso
import web.keyboard.KeyboardEvent

@JsModule("ace-builds/src-noconflict/ace")
@JsNonModule
@JsName("ace")
external object Ace {
    fun edit(id: String): AceEditor
}

external interface AceEditor {
    fun on(event: String, cb: (delta: AceDelta) -> Unit)
    fun destroy()
    fun focus()
    fun getValue(): String
    fun setValue(value: String)
    fun setTheme(theme: String)
    fun clearSelection()
    fun setFontSize(size: Int)
    fun getSession(): AceEditSession
    fun getCursorPosition(): AceCursorPosition
    fun moveCursorTo(row: Int, col: Int)
    fun resize()
    fun insert(s: String)
    fun moveCursorUp()
    fun moveCursorDown()
    val keyBinding: AceKeyBinding
    val commands: AceCommands
    val renderer: AceVirtualRenderer
}

external interface AceEditSession {
    fun setMode(mode: String)
    fun setUseWrapMode(wrap: Boolean)
    fun setTabSize(tabsize: Int)
    fun setUseSoftTabs(spaces: Boolean)
    fun getRowWrapIndent(row: Int): Int
    fun indentRows(startRow: Int, endRow: Int, indentWith: String)
    fun replace(range: AceRange, text: String)
    fun getLine(row: Int): String
}

external interface AceKeyBinding {
    fun addKeyboardHandler(handler: (String, String, String, Int, KeyboardEvent?) -> AceCommandObject?)
}

external interface AceVirtualRenderer {
    fun textToScreenCoordinates(row: Int, column: Int): AceCoordinates
}

external interface AceCoordinates {
    val pageX: Int
    val pageY: Int
}

external interface AceCursorPosition {
    var row: Int
    var column: Int
}

external interface AceCommands {
    fun addCommand(command: AceCommand)
}

external interface AceCommand {
    var name: String
    var bindKey: AceCommandKey
    var exec: (editor: AceEditor) -> Boolean
}

external interface AceCommandKey {
    var win: String
    var mac: String
}

external interface AceDelta {
    val start: AceCursorPosition
    val end: AceCursorPosition
    val lines: Array<String>
    val action: String
}

external interface AceRange {
    var start: AceCursorPosition
    var end: AceCursorPosition
}

external interface AceCommandObject {
    val command: (() -> Unit)?
}

// Helper functions for ergonomic object creation

fun aceRange(startRow: Int, startCol: Int, endRow: Int, endCol: Int): AceRange = unsafeJso {
    start = unsafeJso {
        row = startRow
        column = startCol
    }
    end = unsafeJso {
        row = endRow
        column = endCol
    }
}

fun aceCommand(name: String, winKey: String, macKey: String, exec: (AceEditor) -> Boolean): AceCommand = unsafeJso {
    this.name = name
    bindKey = unsafeJso {
        win = winKey
        mac = macKey
    }
    this.exec = exec
}
