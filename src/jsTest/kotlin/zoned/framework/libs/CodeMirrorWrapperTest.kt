package zoned.framework.libs

import kotlinx.css.*
import web.dom.document
import web.dom.getComputedStyle
import web.events.EventType
import web.keyboard.KeyboardEvent
import web.keyboard.KeyboardEventInit
import zoned.framework.hasDom
import zoned.framework.interop.css
import kotlin.js.Promise
import kotlin.test.*

class CodeMirrorWrapperTest {
    private fun CodeMirrorWrapper.pressKey(key: String, ctrl: Boolean = false, alt: Boolean = false) {
        view.contentDOM.dispatchEvent(KeyboardEvent(EventType("keydown"), KeyboardEventInit(
            key = key, ctrlKey = ctrl, altKey = alt, bubbles = true, cancelable = true,
        )))
    }

    private fun withEditor(options: CodeMirrorOptions = CodeMirrorOptions(), test: (CodeMirrorWrapper) -> Unit): Promise<Unit> {
        if (!hasDom) return Promise.resolve(Unit)
        return loadCodeMirror().then { modules ->
            val host = document.createElement("div")
            host.css { width = 300.px; height = 200.px }
            document.body.appendChild(host)
            val editor = CodeMirrorWrapper(modules, host, options)
            try { test(editor) } finally { editor.destroy(); host.remove() }
        }
    }

    @Test
    fun concurrentLoadsShareOnePromise(): Promise<Unit> {
        val first = loadCodeMirror()
        assertSame(first, loadCodeMirror())
        return first.then { assertSame(first, loadCodeMirror()) }
    }

    @Test
    fun replacingSelectionPreservesNativeChangesAndUndo(): Promise<Unit> = withEditor { ed ->
        ed.setValue("one\ntwo\nthree")
        ed.select(EditorPosition(0, 1), EditorPosition(2, 2))
        ed.insert("X\nY")
        assertEquals("oX\nYree", ed.getValue())
        assertEquals(EditorPosition(1, 1), ed.getCursorPosition())
        assertTrue(ed.undo())
        assertEquals("one\ntwo\nthree", ed.getValue())
    }

    @Test
    fun selectNextOccurrenceEditsBothSelectionsAndUndoRestoresThem(): Promise<Unit> = withEditor { ed ->
        ed.setValue("word word")
        ed.select(EditorPosition(0, 0), EditorPosition(0, 4))
        ed.focus()
        ed.pressKey("d", ctrl = true)
        assertEquals(listOf(0 to 4, 5 to 9), ed.view.state.selection.ranges.map { it.from to it.to })
        ed.insert("changed")
        assertEquals("changed changed", ed.getValue())
        assertTrue(ed.undo())
        assertEquals("word word", ed.getValue())
        assertEquals(listOf(0 to 4, 5 to 9), ed.view.state.selection.ranges.map { it.from to it.to })
    }

    @Test
    fun addingCursorAboveEditsBothLines(): Promise<Unit> = withEditor { ed ->
        ed.setValue("one\ntwo")
        ed.focus()
        ed.pressKey("ArrowUp", ctrl = true, alt = true)
        assertEquals(listOf(3, 7), ed.view.state.selection.ranges.map { it.head })
        assertTrue(ed.hasSelection(), "multiple cursors must bypass single-cursor application commands")
        ed.type("!")
        assertEquals("one!\ntwo!", ed.getValue())
    }

    @Test
    fun equalContentPreservesUndoAndChangedContentResetsIt(): Promise<Unit> = withEditor { ed ->
        ed.insert("one\ntwo")
        ed.setValue("one\ntwo")
        assertTrue(ed.undo(), "reattaching unchanged content keeps undo history")
        assertEquals("", ed.getValue())
        assertTrue(ed.redo())
        assertEquals("one\ntwo", ed.getValue())
        ed.setValue("external update")
        assertFalse(ed.undo(), "external replacement starts a new history")
        assertEquals(EditorPosition(0, 15), ed.getCursorPosition())
    }

    @Test
    fun typedThemeUpdatesWithoutLosingHistoryAndStylesAreRemovedOnDestroy(): Promise<Unit> = withEditor { ed ->
        ed.insert("# Heading")
        val sheets = document.head.getElementsByTagName("style").length
        val dom = ed.view.dom
        ed.setTheme(CodeMirrorTheme(
            foreground = Color("#123456"), background = Color("#fedcba"),
            heading = Color("#abcdef"), fontSize = 19.px, fontFamily = "serif",
        ))
        assertEquals("rgb(18, 52, 86)", getComputedStyle(dom).color)
        assertEquals("rgb(254, 220, 186)", getComputedStyle(dom).backgroundColor)
        assertEquals("19px", getComputedStyle(dom).fontSize)
        assertEquals("serif", getComputedStyle(ed.view.scrollDOM).fontFamily)
        val heading = dom.querySelector(".tok-heading")!!
        assertEquals("rgb(171, 205, 239)", getComputedStyle(heading).color)
        assertEquals(sheets, document.head.getElementsByTagName("style").length)
        assertTrue(ed.undo())
        ed.destroy()
        ed.destroy()
        assertFalse(dom.isConnected)
        assertEquals(sheets - 1, document.head.getElementsByTagName("style").length)
    }

    @Test
    fun proseModeWrapsAndHidesGutter(): Promise<Unit> = withEditor(CodeMirrorOptions(plain = true)) { ed ->
        assertNull(ed.view.dom.querySelector(".cm-gutters"))
        assertEquals("break-spaces", getComputedStyle(ed.view.contentDOM).whiteSpace)
        assertEquals(200, ed.view.dom.clientHeight)
    }

    @Test
    fun lineNumberAndBackgroundColorsSurviveFocusAndThemeChanges(): Promise<Unit> = withEditor { ed ->
        val blue = Color("#243b55")
        val theme = CodeMirrorTheme(background = blue, foreground = Color.white, lineNumberColor = Color.white, dark = true)
        ed.setTheme(theme)
        ed.setValue("first\nsecond")
        ed.focus()
        ed.insert("!")
        val dom = ed.view.dom
        val numbers = dom.querySelector(".cm-lineNumbers")!!
        val gutter = dom.querySelector(".cm-gutters")!!
        assertEquals("rgb(36, 59, 85)", getComputedStyle(dom).backgroundColor)
        assertEquals(getComputedStyle(dom).backgroundColor, getComputedStyle(gutter).backgroundColor)
        assertEquals("rgb(255, 255, 255)", getComputedStyle(numbers).color)
        ed.setTheme(theme.copy(background = Color.white, foreground = Color.black, lineNumberColor = Color.black, dark = false))
        assertEquals("rgb(255, 255, 255)", getComputedStyle(dom).backgroundColor)
        assertEquals(getComputedStyle(dom).backgroundColor, getComputedStyle(gutter).backgroundColor)
        assertEquals("rgb(0, 0, 0)", getComputedStyle(numbers).color)
    }
}
