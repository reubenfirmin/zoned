package zoned.framework.libs

import kotlinx.css.Color
import kotlinx.css.LinearDimension
import kotlinx.css.px
import web.html.HTMLElement

/** Zero-based row and column, independent of CodeMirror's character offsets. */
data class EditorPosition(val row: Int, val column: Int)
data class EditorCompletion(val label: String, val insertText: String, val detail: String = "")
data class EditorCompletionContext(val line: String, val cursor: EditorPosition, val explicit: Boolean)
data class EditorCompletions(val fromColumn: Int, val options: List<EditorCompletion>)

data class CodeMirrorTheme(
    val foreground: Color = Color("#222"),
    val background: Color = Color.white,
    val popupBackground: Color = background,
    val popupForeground: Color = foreground,
    val muted: Color = Color("#777"),
    val lineNumberColor: Color = muted,
    val accent: Color = Color("#246"),
    val heading: Color = Color("#246"),
    val link: Color = Color("#246"),
    val selection: Color = Color("#bdf8"),
    val activeLine: Color = Color("#8881"),
    val fontFamily: String = "monospace",
    val fontSize: LinearDimension = 16.px,
    val dark: Boolean = false,
)

data class CodeMirrorOptions(
    val text: String = "",
    val tabSize: Int = 2,
    val lineWrapping: Boolean = true,
    val plain: Boolean = false,
    val markdown: Boolean = true,
    /** Applications with their own Markdown dialect can install their own Enter/Backspace rules. */
    val markdownKeymap: Boolean = true,
    val theme: CodeMirrorTheme = CodeMirrorTheme(),
    val extensions: List<CmExtension> = emptyList(),
)

/**
 * Reusable editor lifecycle and operations. The application supplies its commands, completion
 * source and theme; no domain syntax or persistence lives here. [view] permits typed native
 * transactions and [CodeMirrorOptions.extensions] accepts additional CodeMirror extensions.
 */
class CodeMirrorWrapper(
    val modules: CodeMirrorModules,
    parent: HTMLElement,
    private val options: CodeMirrorOptions = CodeMirrorOptions(),
) {
    private val commandCompartment = modules.createCompartment()
    private val completionCompartment = modules.createCompartment()
    private val themeCompartment = modules.createCompartment()
    private val bindings = mutableListOf<CmKeyBinding>()
    private var completionSource: ((EditorCompletionContext) -> EditorCompletions?)? = null
    private var theme = options.theme
    private var destroyed = false
    private val styles = CodeMirrorStyles(theme)
    val view: CmView = modules.createView(CmViewConfig(state = createState(options.text), parent = parent))

    private fun createState(text: String): CmState = modules.state.EditorState.create(CmStateConfig(
        doc = text,
        selection = CmSelectionSpec(anchor = text.length),
        extensions = buildList {
            // CodeMirror rewrites the root class on focus and reconfiguration; let it own ours too.
            add(modules.view.EditorView.editorAttributes.of(CmEditorAttributes(`class` = styles.className.toString())))
            add(modules.state.EditorState.tabSize.of(options.tabSize))
            add(modules.state.EditorState.allowMultipleSelections.of(true))
            add(modules.language.indentUnit.of(" ".repeat(options.tabSize)))
            add(modules.commands.history())
            add(modules.view.drawSelection())
            add(modules.view.dropCursor())
            add(modules.language.bracketMatching())
            add(modules.autocomplete.closeBrackets())
            add(commandCompartment.of(commandExtension()))
            add(completionCompartment.of(completionExtension()))
            add(themeCompartment.of(modules.view.EditorView.darkTheme.of(theme.dark)))
            add(styles.highlighting(modules))
            add(modules.view.keymap.of(
                modules.autocomplete.closeBracketsKeymap + modules.commands.defaultKeymap +
                    modules.commands.historyKeymap + modules.search.searchKeymap + modules.commands.indentWithTab
            ))
            if (options.lineWrapping) add(modules.view.EditorView.lineWrapping)
            if (!options.plain) {
                add(modules.view.lineNumbers())
                add(modules.view.highlightActiveLine())
                add(modules.view.highlightActiveLineGutter())
            }
            if (options.markdown) add(modules.markdown.markdown(CmMarkdownConfig(
                addKeymap = options.markdownKeymap,
                completeHTMLTags = false,
                pasteURLAsLink = false,
            )))
            addAll(options.extensions)
        }.toTypedArray(),
    ))

    fun getValue(): String = view.state.doc.toString()

    /** Replace externally supplied content, resetting undo history; equal content is a no-op. */
    fun setValue(text: String) {
        if (text != getValue()) view.setState(createState(text))
    }

    fun getCursorPosition(): EditorPosition {
        val head = view.state.selection.main.head
        val line = view.state.doc.lineAt(head)
        return EditorPosition(line.number - 1, head - line.from)
    }

    fun getLine(row: Int): String = view.state.doc.line(row + 1).text
    fun hasSelection(): Boolean = !view.state.selection.main.empty || view.state.selection.ranges.size > 1

    private fun offset(position: EditorPosition): Int {
        val line = view.state.doc.line((position.row + 1).coerceIn(1, view.state.doc.lines))
        return line.from + position.column.coerceIn(0, line.length)
    }

    fun moveCursorTo(row: Int, column: Int) {
        view.dispatch(CmTransactionSpec(
            selection = CmSelectionSpec(anchor = offset(EditorPosition(row, column))),
            scrollIntoView = true,
        ))
    }

    fun select(from: EditorPosition, to: EditorPosition) {
        view.dispatch(CmTransactionSpec(selection = CmSelectionSpec(anchor = offset(from), head = offset(to))))
    }

    /** Replace a range and optionally place the cursor in the resulting document, in one undo step. */
    fun replace(from: EditorPosition, to: EditorPosition, text: String, cursor: EditorPosition? = null) {
        val start = offset(from)
        val end = offset(to)
        val selection = cursor?.let {
            // Row/column cursor override is for edits within one line (indent/outdent).
            require(from.row == to.row && '\n' !in text && it.row == from.row)
            CmSelectionSpec(anchor = view.state.doc.line(it.row + 1).from + it.column)
        }
        view.dispatch(CmTransactionSpec(
            changes = CmChange(from = start, to = end, insert = text),
            selection = selection,
            userEvent = "input",
            scrollIntoView = true,
        ))
    }

    fun insert(text: String) {
        view.dispatch(
            view.state.replaceSelection(text),
            CmTransactionSpec(userEvent = "input.type", scrollIntoView = true),
        )
    }

    /** Programmatic typing with the same bracket-pair and live-completion behavior as text input. */
    fun type(text: String) {
        val bracket = if (text.length == 1) modules.autocomplete.insertBracket(view.state, text) else null
        if (bracket != null) view.dispatch(bracket) else insert(text)
    }

    fun addKeyBinding(key: String, command: (CodeMirrorWrapper) -> Boolean) {
        bindings.add(CmKeyBinding(key = key, run = { command(this@CodeMirrorWrapper) }))
        view.dispatch(CmTransactionSpec(effects = commandCompartment.reconfigure(commandExtension())))
    }

    private fun commandExtension(): CmExtension = modules.view.keymap.of(bindings.toTypedArray())

    fun setCompletionSource(source: (EditorCompletionContext) -> EditorCompletions?) {
        completionSource = source
        view.dispatch(CmTransactionSpec(effects = completionCompartment.reconfigure(completionExtension())))
    }

    private fun completionExtension(): CmExtension = modules.autocomplete.autocompletion(CmCompletionConfig(
        `override` = arrayOf({ context: CmCompletionContext ->
            val line = context.state.doc.lineAt(context.pos)
            val result = completionSource?.invoke(EditorCompletionContext(
                line.text, EditorPosition(line.number - 1, context.pos - line.from), context.explicit,
            ))
            result?.let {
                CmCompletionResult(
                    from = line.from + it.fromColumn,
                    options = it.options.map { item ->
                        CmCompletion(label = item.label, apply = item.insertText, detail = item.detail)
                    }.toTypedArray(),
                )
            }
        }),
    ))

    fun startCompletion(): Boolean = modules.autocomplete.startCompletion(view)
    fun acceptCompletion(): Boolean = modules.autocomplete.acceptCompletion(view)
    fun completionActive(): Boolean = modules.autocomplete.completionStatus(view.state) != null
    fun completionLabels(): List<String> = modules.autocomplete.currentCompletions(view.state).map { it.label }
    fun undo(): Boolean = modules.commands.undo(view)
    fun redo(): Boolean = modules.commands.redo(view)
    fun focus() = view.focus()
    fun resize() = view.requestMeasure()

    fun setTheme(theme: CodeMirrorTheme) {
        this.theme = theme
        styles.update(theme)
        view.dispatch(CmTransactionSpec(effects = themeCompartment.reconfigure(modules.view.EditorView.darkTheme.of(theme.dark))))
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        view.destroy()
        view.dom.remove()
        styles.destroy()
    }
}
