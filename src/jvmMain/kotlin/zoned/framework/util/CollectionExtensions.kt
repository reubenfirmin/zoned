package zoned.framework.util

fun <T> Collection<T>.indexOfOrNull(t: T): Int? {
    val idx = indexOf(t)
    if (idx == -1) {
        return null
    } else {
        return idx
    }
}

inline operator fun <reified T> Array<T>.plus(other: Array<T>): Array<T> {
    return this.toMutableList().apply { addAll(other) }.toTypedArray()
}