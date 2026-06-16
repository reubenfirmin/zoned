package zoned.framework.libs

import js.objects.unsafeJso
import kotlin.js.Promise
import kotlin.js.RegExp
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
    // WHY raw js(): Kotlin/JS has NO typed expression for a dynamic, code-split `import()` — it is
    // the conventional idiom (kotlinx + kotlin-wrappers themselves lazy-load this way). A static
    // @JsModule binding would bundle Ace into the main chunk, defeating the code-split; a typed JS
    // shim could hide it but would add a hand-written module + klib-resource packaging to this
    // PUBLISHED library. So the raw import lives here, confined, and the result is typed at the
    // boundary (below). The resolver import is side-effect-only and must run after ace itself;
    // chaining in raw JS lets the nested promise flatten naturally.
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

private var languageToolsLoading: Promise<Unit>? = null

/**
 * Load Ace's autocompletion extension (`language_tools`) ON DEMAND, once — code-split like [loadAce]
 * itself. Importing the extension registers the autocompletion editor options (e.g.
 * `enableLiveAutocompletion`) and completer machinery on the already-loaded Ace, so it must run
 * AFTER [loadAce]. Cached: later calls resolve immediately.
 */
fun loadAceLanguageTools(): Promise<Unit> {
    languageToolsLoading?.let { return it }
    // Raw js() for the same reason as [loadAce]: it's the only way to express a code-split
    // `import()` in Kotlin/JS. Confined here; the public API ([loadAceLanguageTools]) stays typed.
    val loading = js("import('ace-builds/src-noconflict/ext-language_tools')")
        .unsafeCast<Promise<dynamic>>()
        .then<Unit> { }
    languageToolsLoading = loading
    return loading
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
    /** Run a registered editor command by name (e.g. "startAutocomplete"). */
    fun execCommand(command: String)
    val keyBinding: AceKeyBinding
    val commands: AceCommands
    val renderer: AceVirtualRenderer
    /** Apply a batch of editor options (e.g. the autocompletion toggles). */
    fun setOptions(options: AceEditorOptions)
    /** The active completion providers. Assigning replaces Ace's defaults. Requires the
     *  `language_tools` extension to be loaded ([loadAceLanguageTools]). */
    var completers: Array<AceCompleter>
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
    /** Run a command through the exec pipeline (fires afterExec — drives live autocompletion). */
    fun exec(command: String, editor: AceEditor, args: String)
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

/** The subset of editor options we toggle: autocompletion, plus the chrome that distinguishes a
 *  code editor from a plain prose editor (gutter/line-numbers, the print margin, the active-line
 *  highlight). All optional — unset keys keep Ace's defaults.
 *
 *  NOTE: deliberately NO `fontFamily` — Ace's renderer measures a single glyph width and assumes a
 *  monospace face, so a proportional font misplaces the cursor, selection, and the autocomplete
 *  popup. Plain mode drops the chrome but keeps a monospace face. */
external interface AceEditorOptions {
    var enableBasicAutocompletion: Boolean
    var enableLiveAutocompletion: Boolean
    var showGutter: Boolean
    var showPrintMargin: Boolean
    var highlightActiveLine: Boolean
}

/** One suggestion in the autocomplete popup. */
external interface AceCompletion {
    var caption: String   // shown in the list
    var value: String     // inserted on accept
    var meta: String      // greyed type hint on the right
}

/** Ace hands completions back asynchronously: `callback(error, completions)`. */
typealias AceCompletionCallback = (error: Any?, completions: Array<AceCompletion>) -> Unit

/**
 * A completion provider. [identifierRegexps] defines which characters count as part of the prefix
 * (control how far back Ace reads the typed token); [getCompletions] is asked for suggestions and
 * replies through its callback.
 */
external interface AceCompleter {
    var identifierRegexps: Array<RegExp>
    /** Characters that pop the menu open immediately on type (e.g. `[`), even with an empty prefix. */
    var triggerCharacters: Array<String>
    var getCompletions: (
        editor: AceEditor,
        session: AceEditSession,
        pos: AceCursorPosition,
        prefix: String,
        callback: AceCompletionCallback,
    ) -> Unit
}

// Typed builders for the plain-JS config objects Ace expects (a command, a completer, a range…).
//
// WHY unsafeJso: constructing an `external interface` instance requires kotlin-wrappers' object
// builder. It used to be called `jso`; kotlin-wrappers RENAMED it to `unsafeJso` (current: 2026.6.2)
// precisely to stop implying the construction is type-checked — building an external interface never
// is. There is NO `jso` to switch to; `unsafeJso` IS the sanctioned, only API. Everything around it
// is fully typed: each builder takes typed params and returns a typed `external interface`, so call
// sites never touch `dynamic`. The single `unsafeJso { }` per builder is the minimal, contained
// construction primitive — the boundary, not a shortcut.

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

fun aceEditorOptions(enableBasicAutocompletion: Boolean, enableLiveAutocompletion: Boolean): AceEditorOptions = unsafeJso {
    this.enableBasicAutocompletion = enableBasicAutocompletion
    this.enableLiveAutocompletion = enableLiveAutocompletion
}

/** Editor chrome: make a code editor read as a plain editor by hiding the gutter (line numbers), the
 *  print margin, and the active-line highlight. The face stays monospace (see [AceEditorOptions]). */
fun acePlainEditorOptions(): AceEditorOptions = unsafeJso {
    this.showGutter = false
    this.showPrintMargin = false
    this.highlightActiveLine = false
}

fun aceCompletion(caption: String, value: String, meta: String): AceCompletion = unsafeJso {
    this.caption = caption
    this.value = value
    this.meta = meta
}

fun aceCompleter(
    triggerCharacters: Array<String>,
    identifierRegexps: Array<RegExp>,
    getCompletions: (AceEditor, AceEditSession, AceCursorPosition, String, AceCompletionCallback) -> Unit,
): AceCompleter = unsafeJso {
    this.triggerCharacters = triggerCharacters
    this.identifierRegexps = identifierRegexps
    this.getCompletions = getCompletions
}
