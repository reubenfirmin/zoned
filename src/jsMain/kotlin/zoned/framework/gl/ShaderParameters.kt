package zoned.framework.gl

import js.objects.Record
import js.objects.unsafeJso

/** Builds Three.js uniform records while preserving each value's Kotlin type. */
class ShaderParameters {
    val values: Record<String, Uniform<*>> = unsafeJso()

    fun <T> bind(name: String, value: T): Uniform<T> = Uniform(value).also { values[name] = it }
}
