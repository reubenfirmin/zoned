package zoned.framework.libs

import js.array.Tuple
import js.array.Tuple1
import js.errors.toThrowable
import js.function.ConstructorFunction
import js.function.invoke
import js.import.importAsync
import js.promise.Promise as JsPromise
import kotlin.js.Promise
import kotlinx.js.JsPlainObject
import web.html.HTMLElement

/** Typed CodeMirror 6 interop. Configuration constructors enforce required fields at compile time. */
external interface CmExtension
external interface CmEffect
external interface CmTransaction
external interface CmTag
/** Native transaction input, which may contain a ChangeSet and multiple selections. */
external interface CmTransactionInput

external interface CmFacet<T> { fun of(value: T): CmExtension }
external interface CmCompartment {
    fun of(extension: CmExtension): CmExtension
    fun of(extensions: Array<CmExtension>): CmExtension
    fun reconfigure(extension: CmExtension): CmEffect
    fun reconfigure(extensions: Array<CmExtension>): CmEffect
}
external interface CmText {
    val length: Int
    val lines: Int
    fun line(number: Int): CmLine
    fun lineAt(position: Int): CmLine
    override fun toString(): String
}
external interface CmLine {
    val from: Int
    val to: Int
    val number: Int
    val text: String
    val length: Int
}
external interface CmSelectionRange {
    val anchor: Int
    val head: Int
    val from: Int
    val to: Int
    val empty: Boolean
}
external interface CmSelection {
    val main: CmSelectionRange
    val ranges: Array<CmSelectionRange>
}
@JsPlainObject
external interface CmSelectionSpec { val anchor: Int; val head: Int? }
@JsPlainObject
external interface CmChange { var from: Int; var to: Int; var insert: String }
@JsPlainObject
external interface CmTransactionSpec : CmTransactionInput {
    var changes: CmChange?
    var selection: CmSelectionSpec?
    var effects: CmEffect?
    var userEvent: String?
    var scrollIntoView: Boolean?
}
external interface CmState {
    val doc: CmText
    val selection: CmSelection
    fun replaceSelection(text: String): CmTransactionInput
}
@JsPlainObject
external interface CmStateConfig {
    var doc: String
    var selection: CmSelectionSpec?
    var extensions: Array<CmExtension>
}
@JsPlainObject
external interface CmViewConfig { val state: CmState; val parent: HTMLElement }
@JsPlainObject
external interface CmEditorAttributes { val `class`: String }
external interface CmView {
    val state: CmState
    val dom: HTMLElement
    val contentDOM: HTMLElement
    val scrollDOM: HTMLElement
    fun dispatch(vararg specs: CmTransactionInput)
    fun dispatch(transaction: CmTransaction)
    fun setState(state: CmState)
    fun focus()
    fun requestMeasure()
    fun destroy()
}
@JsPlainObject
external interface CmKeyBinding {
    var key: String?
    var run: ((CmView) -> Boolean)?
    var shift: ((CmView) -> Boolean)?
    var preventDefault: Boolean?
}
external interface CmCompletionContext {
    val state: CmState
    val pos: Int
    val explicit: Boolean
}
@JsPlainObject
external interface CmCompletionInfo { val label: String; val detail: String? }
@JsPlainObject
external interface CmCompletion : CmCompletionInfo { val apply: String? }
@JsPlainObject
external interface CmCompletionResult { val from: Int; val to: Int?; val options: Array<CmCompletion> }
@JsPlainObject
external interface CmCompletionConfig {
    var `override`: Array<(CmCompletionContext) -> CmCompletionResult?>
}
@JsPlainObject
external interface CmMarkdownConfig {
    var addKeymap: Boolean
    var completeHTMLTags: Boolean
    var pasteURLAsLink: Boolean
}
external interface CmHighlighter
@JsPlainObject
external interface CmTagClass { val tag: CmTag; val `class`: String }

external interface CmStateFactory {
    fun create(config: CmStateConfig): CmState
    val tabSize: CmFacet<Int>
    val allowMultipleSelections: CmFacet<Boolean>
}
external interface CmViewFactory : ConstructorFunction<Tuple1<CmViewConfig>, CmView> {
    val lineWrapping: CmExtension
    val darkTheme: CmFacet<Boolean>
    val editorAttributes: CmFacet<CmEditorAttributes>
}
external interface CmStateModule {
    val EditorState: CmStateFactory
    val Compartment: ConstructorFunction<Tuple, CmCompartment>
}
external interface CmViewModule {
    val EditorView: CmViewFactory
    val keymap: CmFacet<Array<CmKeyBinding>>
    fun lineNumbers(): CmExtension
    fun highlightActiveLine(): CmExtension
    fun highlightActiveLineGutter(): CmExtension
    fun drawSelection(): CmExtension
    fun dropCursor(): CmExtension
}
external interface CmCommandsModule {
    val defaultKeymap: Array<CmKeyBinding>
    val historyKeymap: Array<CmKeyBinding>
    val indentWithTab: CmKeyBinding
    fun history(): CmExtension
    fun undo(view: CmView): Boolean
    fun redo(view: CmView): Boolean
}
external interface CmLanguageModule {
    val indentUnit: CmFacet<String>
    fun syntaxHighlighting(highlighter: CmHighlighter): CmExtension
    fun bracketMatching(): CmExtension
}
external interface CmMarkdownModule { fun markdown(config: CmMarkdownConfig): CmExtension }
external interface CmAutocompleteModule {
    fun autocompletion(config: CmCompletionConfig): CmExtension
    fun closeBrackets(): CmExtension
    val closeBracketsKeymap: Array<CmKeyBinding>
    fun startCompletion(view: CmView): Boolean
    fun closeCompletion(view: CmView): Boolean
    fun acceptCompletion(view: CmView): Boolean
    fun completionStatus(state: CmState): String?
    fun currentCompletions(state: CmState): Array<CmCompletionInfo>
    fun insertBracket(state: CmState, bracket: String): CmTransaction?
}
external interface CmSearchModule { val searchKeymap: Array<CmKeyBinding> }
external interface CmHighlightTags {
    val heading: CmTag
    val list: CmTag
    val strong: CmTag
    val emphasis: CmTag
    val link: CmTag
    val url: CmTag
    val quote: CmTag
    val monospace: CmTag
}
external interface CmHighlightModule {
    val tags: CmHighlightTags
    fun tagHighlighter(tags: Array<CmTagClass>): CmHighlighter
}

/** Advanced consumers can build native extensions using these typed, lazily loaded modules. */
@JsPlainObject
external interface CodeMirrorModules {
    val state: CmStateModule
    val view: CmViewModule
    val commands: CmCommandsModule
    val language: CmLanguageModule
    val markdown: CmMarkdownModule
    val autocomplete: CmAutocompleteModule
    val search: CmSearchModule
    val highlight: CmHighlightModule
}

fun CodeMirrorModules.createView(config: CmViewConfig): CmView = view.EditorView(config)
fun CodeMirrorModules.createCompartment(): CmCompartment = state.Compartment()

private var codeMirrorLoading: Promise<CodeMirrorModules>? = null

/**
 * One shared import, including concurrent callers. All imports start together and remain
 * code-split; module exports and constructor arguments are typed. Rejected loads can be retried.
 */
fun loadCodeMirror(): Promise<CodeMirrorModules> {
    codeMirrorLoading?.let { return it }
    val state = importAsync<CmStateModule>("@codemirror/state")
    val view = importAsync<CmViewModule>("@codemirror/view")
    val commands = importAsync<CmCommandsModule>("@codemirror/commands")
    val language = importAsync<CmLanguageModule>("@codemirror/language")
    val markdown = importAsync<CmMarkdownModule>("@codemirror/lang-markdown")
    val autocomplete = importAsync<CmAutocompleteModule>("@codemirror/autocomplete")
    val search = importAsync<CmSearchModule>("@codemirror/search")
    val highlight = importAsync<CmHighlightModule>("@lezer/highlight")
    // Observe every rejection immediately. Then combine the typed exports without array casts.
    val modules = JsPromise.all(arrayOf(state, view, commands, language, markdown, autocomplete, search, highlight)).flatThen {
        state.flatThen { s ->
            view.flatThen { v ->
                commands.flatThen { c ->
                    language.flatThen { l ->
                        markdown.flatThen { m ->
                            autocomplete.flatThen { a ->
                                search.flatThen { f ->
                                    highlight.then { h -> CodeMirrorModules(s, v, c, l, m, a, f, h) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // Preserve the framework's kotlin.js.Promise API at this boundary.
    val loading = Promise<CodeMirrorModules> { resolve, reject ->
        modules.then({ resolve(it) }, { reject(it.toThrowable()) })
    }
    codeMirrorLoading = loading.catch { error ->
        codeMirrorLoading = null
        throw error
    }
    return codeMirrorLoading!!
}
