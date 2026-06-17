package zoned.gradle

/**
 * Project-level configuration for the zoned gradle plugin, exposed as the `zoned { }` block.
 */
abstract class ZonedExtension {

    internal val enumMappings = mutableListOf<Pair<String, String>>()
    internal val forcedTypes = mutableListOf<Triple<String, String, String>>()

    /**
     * Map a column to an arbitrary Kotlin [userType] via a jOOQ [converter] (FQN) for SQLite
     * generation, e.g. `forcedType("currencies", "kotlin.collections.List<rcp.model.enums.Currency>",
     * "rcp.model.CurrencyListConverter")`. [column] is matched by unqualified name.
     */
    fun forcedType(column: String, userType: String, converter: String) {
        forcedTypes += Triple(column, userType, converter)
    }

    /**
     * Map a column to a Kotlin enum for SQLite model generation (SQLite has no enum types, so the
     * column is stored as TEXT). [column] is the unqualified column name; [enumClass] is the fully
     * qualified enum class name (build scripts can't reference the app's classes directly), e.g.:
     *
     * ```
     * zoned {
     *     enum("currency", "rcp.model.enums.Currency")
     *     enum("role", "rcp.model.enums.UserRole")
     * }
     * ```
     *
     * jOOQ generates an `EnumConverter` so the generated field is the enum, persisted as its name.
     */
    fun enum(column: String, enumClass: String) {
        enumMappings += column to enumClass
    }
}
