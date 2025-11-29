package zoned.framework.libs

import web.keyboard.KeyboardEvent

@JsModule("ace-builds/src-noconflict/ace")
@JsNonModule
@JsName("ace")
external object AceWrapper {

    fun edit(id: String): AceEditor

    interface AceEditor {

        // NOTE this is only valid for "change"
        fun on(event: String, cb: (delta: AceDelta) -> Unit)

        fun destroy()

        fun focus()

        fun getValue(): String

        fun setValue(value: String)

        fun setTheme(theme: String)

        fun clearSelection()

        fun setFontSize(number: Int)

        fun getSession(): EditSession

        fun getCursorPosition(): AceCursorPosition

        fun moveCursorTo(row: Int, col: Int)

        fun resize()

        fun insert(s: String)

        fun moveCursorUp()

        fun moveCursorDown()

        val keyBinding: KeyBinding

        val commands: AceCommands

        val renderer: AceVirtualRenderer
    }

    interface EditSession {

        fun setMode(mode: String)

        fun setUseWrapMode(wrap: Boolean)

        fun setTabSize(tabsize: Int)

        fun setUseSoftTabs(spaces: Boolean)

        fun getRowWrapIndent(row: Int): Int

        fun indentRows(startRow: Int, endRow: Int, indentWith: String)

        fun replace(range: AceRange, text: String)

        fun getLine(row: Int): String
    }

    interface KeyBinding {
        fun addKeyboardHandler(handler: (String, String, String, Int, KeyboardEvent) -> CommandObject?)
    }

    interface AceVirtualRenderer {
        fun textToScreenCoordinates(row: Int, column: Int): AceCoordinates
    }

    interface AceCoordinates {
        val pageX: Int
        val pageY: Int
    }

    interface AceCursorPosition {
        var row: Int
        var column: Int
    }

    interface AceCommands {
        fun addCommand(command: AceCommand)
    }

    interface AceCommand {
        val name: String
        val bindKey: AceCommandKey
        val exec: (editor: AceEditor) -> Boolean
    }

    interface AceCommandKey {
        val win: String
        val mac: String
    }

    interface AceDelta {
        val start: AceCursorPosition
        val end: AceCursorPosition
        val lines: Array<String>
        val action: String
    }

    interface AceRange {
        var start: AceCursorPosition
        var end: AceCursorPosition
    }

    interface CommandObject {
        val command: (() -> Unit)?
    }
}

class AceHelper {

    companion object {
        fun range(startRow: Int, startCol: Int, endRow: Int, endCol: Int): AceWrapper.AceRange {
            val startPos = js("{}").unsafeCast<AceWrapper.AceCursorPosition>().apply {
                row = startRow
                column = startCol
            }

            val endPos = js("{}").unsafeCast<AceWrapper.AceCursorPosition>().apply {
                row = endRow
                column = endCol
            }

            return js("{}").unsafeCast<AceWrapper.AceRange>().apply {
                start = startPos
                end = endPos
            }
        }
    }
}
