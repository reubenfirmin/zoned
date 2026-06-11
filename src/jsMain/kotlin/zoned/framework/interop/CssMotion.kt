package zoned.framework.interop

import kotlinx.css.Color
import kotlinx.css.CssBuilder
import kotlinx.css.Image
import kotlinx.css.LinearDimension
import kotlinx.css.properties.Angle
import kotlinx.css.properties.FillMode
import kotlinx.css.properties.IterationCount
import kotlinx.css.properties.Time
import kotlinx.css.properties.Timing
import kotlinx.css.properties.s

/*
 * Typed @keyframes / animation / gradient builders — the pieces kotlin-css doesn't model, so
 * stylesheets never need raw CSS strings for motion or gradient work. Keyframes are declared
 * inside a [styleSheet] block and referenced through the returned [KeyframesRef]: the animation
 * name can't drift from its declaration.
 */

/** A typed handle to a declared `@keyframes` block; obtain via [StyleSheetScope.keyframes]. */
value class KeyframesRef internal constructor(val name: String)

/** kotlin-css renders a zero dimension unitless ("0"), which keyframe selectors reject — restore "%". */
private fun LinearDimension.renderStop(): String = toString().let { if (it == "0") "0%" else it }

class KeyframesScope internal constructor() {
    internal val sb = StringBuilder()

    fun from(block: CssBuilder.() -> Unit) = frame("from", block)
    fun to(block: CssBuilder.() -> Unit) = frame("to", block)

    /** A percentage stop — `at(50.pct) { ... }` — or a grouped one — `at(0.pct, 100.pct) { ... }`. */
    fun at(vararg stops: LinearDimension, block: CssBuilder.() -> Unit) =
        frame(stops.joinToString(", ") { it.renderStop() }, block)

    private fun frame(selector: String, block: CssBuilder.() -> Unit) {
        val body = CssBuilder().apply(block).toString().removeSuffix("\n").trim()
        sb.append("    ").append(selector).append(" { ").append(body).append(" }\n")
    }
}

/** Declare `@keyframes [name]` in this stylesheet and get a typed reference for [animation]. */
fun StyleSheetScope.keyframes(name: String, block: KeyframesScope.() -> Unit): KeyframesRef {
    val frames = KeyframesScope().apply(block)
    raw("@keyframes $name {\n${frames.sb}}")
    return KeyframesRef(name)
}

/**
 * One animation referencing a typed [keyframes] handle. Call repeatedly to compose a
 * comma-separated multi-animation shorthand.
 */
fun CssBuilder.animation(
    keyframes: KeyframesRef,
    duration: Time,
    timing: Timing = Timing.ease,
    delay: Time = 0.s,
    iterationCount: IterationCount = IterationCount("1"),
    fillMode: FillMode = FillMode.none,
) {
    val entry = "${keyframes.name} $duration $timing $delay $iterationCount $fillMode"
    val existing = declarations["animation"]
    declarations["animation"] = if (existing != null) "$existing, $entry" else entry
}

// ---- Gradients (usable in css{} and styleSheet rules alike; they render to an [Image]) ----

class GradientStopScope internal constructor() {
    internal val entries = mutableListOf<String>()
    fun stop(color: Color) { entries += color.toString() }
    fun stop(color: Color, at: LinearDimension) { entries += "$color ${at.renderStop()}" }
    fun stop(color: Color, at: Angle) { entries += "$color $at" }
}

fun conicGradient(from: Angle? = null, block: GradientStopScope.() -> Unit): Image {
    val stops = GradientStopScope().apply(block)
    val prefix = from?.let { "from $it, " } ?: ""
    return Image("conic-gradient($prefix${stops.entries.joinToString(", ")})")
}

fun radialGradient(shape: String = "circle", at: String? = null, block: GradientStopScope.() -> Unit): Image {
    val stops = GradientStopScope().apply(block)
    val position = at?.let { " at $it" } ?: ""
    return Image("radial-gradient($shape$position, ${stops.entries.joinToString(", ")})")
}

fun linearGradient(direction: Angle, block: GradientStopScope.() -> Unit): Image {
    val stops = GradientStopScope().apply(block)
    return Image("linear-gradient($direction, ${stops.entries.joinToString(", ")})")
}
