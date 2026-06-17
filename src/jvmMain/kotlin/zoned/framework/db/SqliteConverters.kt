package zoned.framework.db

import org.jooq.Converter
import java.time.OffsetDateTime

/**
 * SQLite has no native timestamptz type — jOOQ maps the declared `timestamptz` columns to its
 * generic OTHER (java.lang.Object) type, reading the stored ISO text back as a String. This
 * converter keeps the rich Kotlin OffsetDateTime while persisting ISO text. Wired in via a forced
 * type for SQLite generation (see JooqGenerator). uuid -> UUID, date -> LocalDate, numeric ->
 * BigDecimal and boolean -> Boolean are all handled natively by jOOQ and need no converter.
 */
class OffsetDateTimeConverter : Converter<Any, OffsetDateTime> {
    override fun from(databaseObject: Any?): OffsetDateTime? = (databaseObject as? String)?.let(OffsetDateTime::parse)
    override fun to(userObject: OffsetDateTime?): Any? = userObject?.toString()
    override fun fromType(): Class<Any> = Any::class.java
    override fun toType(): Class<OffsetDateTime> = OffsetDateTime::class.java
}
