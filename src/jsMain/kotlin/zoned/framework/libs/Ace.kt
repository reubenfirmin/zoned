package zoned.framework.libs

import js.objects.unsafeJso
import kotlin.js.Promise
import web.keyboard.KeyboardEvent

/** The Ace module's surface (the static `ace` object the UMD bundle exports). */
external interface AceModule {
    fun edit(id: String): AceEditor
}

private var aceModule: AceModule? = null

/**
 * Load Ace ON DEMAND — a dynamic `import()`, so webpack splits Ace out of the main bundle and
 * nobody pays its parse cost until an editor actually opens. Also loads the webpack resolver that
 * registers bundled URLs for Ace's runtime-fetched themes/modes/workers (without it Ace fetches
 * them from the server root and 404s). Cached: later calls resolve immediately.
 */
fun loadAce(): Promise<AceModule> {
    aceModule?.let { return Promise.resolve(it) }
    // The resolver import is side-effect-only and must run after ace itself; chain in raw JS so
    // the nested promise flattens naturally, then type the result at the boundary.
    val loading = js(
        "import('ace-builds/src-noconflict/ace')" +
            ".then(function(m){ return import('ace-builds/webpack-resolver').then(function(){ return m; }); })"
    ).unsafeCast<Promise<dynamic>>()
    return loading.then<AceModule> { m ->
        val ace = unwrapModule(m, probe = "edit").unsafeCast<AceModule>()
        aceModule = ace
        ace
    }
}

/**
 * A dynamic `import()` of a UMD/CJS module surfaces its exports either directly on the namespace
 * or under `default`, depending on bundler interop — [probe] picks whichever side carries the API.
 */
internal fun unwrapModule(m: dynamic, probe: String): dynamic =
    if (m[probe] != undefined) m else m.default

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
