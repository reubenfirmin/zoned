package zoned.framework.interop

import kotlinx.css.*
import kotlinx.css.properties.*
import web.dom.ElementId
import web.dom.document
import zoned.framework.hasDom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CssMotionTest {

    private fun sheetText(id: String): String =
        (document.getElementById(ElementId(id))?.textContent ?: "")

    @Test
    fun keyframesEmitAtRuleAndReturnTypedHandle() {
        if (!hasDom) return
        styleSheet("kf-test-1") {
            val spin = keyframes("kf-spin") {
                from { opacity = 0.0 }
                to { put("transform", "rotate(360deg)") }
            }
            rule(".kf-x") {
                animation(spin, 1.s, Timing.linear, iterationCount = IterationCount.infinite)
            }
        }
        try {
            val text = sheetText("kf-test-1")
            assertTrue("@keyframes kf-spin" in text, "at-rule emitted: $text")
            assertTrue("opacity: 0" in text, "from-frame body emitted: $text")
            assertTrue("transform: rotate(360deg)" in text, "to-frame body emitted: $text")
            assertTrue("animation: kf-spin 1s linear" in text, "animation references the handle by name: $text")
            assertTrue("infinite" in text, "iteration count emitted: $text")
        } finally {
            document.getElementById(ElementId("kf-test-1"))?.remove()
        }
    }

    @Test
    fun multiStopFramesAndMultipleAnimationsCompose() {
        if (!hasDom) return
        styleSheet("kf-test-2") {
            val pulse = keyframes("kf-pulse") {
                at(0.pct, 100.pct) { opacity = 0.3 }
                at(50.pct) { opacity = 1.0 }
            }
            val enter = keyframes("kf-enter") {
                from { opacity = 0.0 }
                to { opacity = 1.0 }
            }
            rule(".kf-y") {
                animation(enter, 1.s)
                animation(pulse, 2.s, iterationCount = IterationCount.infinite)
            }
        }
        try {
            val text = sheetText("kf-test-2")
            assertTrue("0%, 100% {" in text, "grouped stops emitted: $text")
            assertTrue("50% {" in text, "single percent stop emitted: $text")
            val animation = text.substringAfter(".kf-y").substringAfter("animation:").substringBefore(";")
            assertTrue("kf-enter" in animation && "kf-pulse" in animation && "," in animation,
                "two animation() calls compose into one comma-separated shorthand: $animation")
        } finally {
            document.getElementById(ElementId("kf-test-2"))?.remove()
        }
    }
}

class GradientsTest {

    @Test
    fun conicGradientRenders() {
        val g = conicGradient(from = 90.deg) {
            stop(Color.transparent, 0.deg)
            stop(Color("#60a5fa"), 60.deg)
            stop(Color.transparent, 140.deg)
        }
        assertEquals("conic-gradient(from 90deg, transparent 0deg, #60a5fa 60deg, transparent 140deg)", g.toString())
    }

    @Test
    fun conicGradientWithoutFromOmitsThePrefix() {
        val g = conicGradient { stop(Color.red); stop(Color.blue) }
        assertEquals("conic-gradient(red, blue)", g.toString())
    }

    @Test
    fun radialGradientRenders() {
        val g = radialGradient(at = "center") {
            stop(Color.white, 0.pct)
            stop(Color.black, 50.pct)
        }
        assertEquals("radial-gradient(circle at center, white 0%, black 50%)", g.toString())
    }

    @Test
    fun linearGradientRenders() {
        val g = linearGradient(135.deg) {
            stop(Color("#111"), 0.pct)
            stop(Color("#222"), LinearDimension("100%"))
        }
        assertEquals("linear-gradient(135deg, #111 0%, #222 100%)", g.toString())
    }
}
