package zoned.framework.routing

class Params(private val map: Map<String, String> = emptyMap()) {
    operator fun get(key: String): String? = map[key]

    fun required(key: String): String =
        map[key] ?: throw IllegalArgumentException("Required parameter not found: $key")

    fun segments(key: String): List<String> {
        val value = map[key] ?: return emptyList()
        return value.split("/").filter { it.isNotEmpty() }
    }

    fun string(key: String): String = required(key)

    fun int(key: String): Int = required(key).toIntOrNull()
        ?: throw IllegalArgumentException("Parameter '$key' is not a valid integer")

    fun long(key: String): Long = required(key).toLongOrNull()
        ?: throw IllegalArgumentException("Parameter '$key' is not a valid long")

    fun boolean(key: String): Boolean = required(key).toBooleanStrictOrNull()
        ?: throw IllegalArgumentException("Parameter '$key' is not a valid boolean")

    fun double(key: String): Double = required(key).toDoubleOrNull()
        ?: throw IllegalArgumentException("Parameter '$key' is not a valid double")

    // Optional typed variants: absent OR malformed reads as null (use the throwing forms when the
    // route pattern guarantees presence and a bad value is a programming error).
    fun intOrNull(key: String): Int? = map[key]?.toIntOrNull()
    fun longOrNull(key: String): Long? = map[key]?.toLongOrNull()
    fun doubleOrNull(key: String): Double? = map[key]?.toDoubleOrNull()
    fun booleanOrNull(key: String): Boolean? = map[key]?.toBooleanStrictOrNull()
}