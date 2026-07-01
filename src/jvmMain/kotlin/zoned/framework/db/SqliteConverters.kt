package zoned.framework.db

import org.jooq.Converter
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * SQLite has no native timestamptz type — jOOQ maps the declared `timestamptz` columns to its
 * generic OTHER (java.lang.Object) type, reading the stored ISO text back as a String. This
 * converter keeps the rich Kotlin OffsetDateTime while persisting ISO text. Wired in via a forced
 * type for SQLite generation (see JooqGenerator). uuid -> UUID, date -> LocalDate, numeric ->
 * BigDecimal and boolean -> Boolean are all handled natively by jOOQ and need no converter.
 *
 * Reading is deliberately lenient. We always WRITE strict ISO-8601 (see [to]), but values can
 * arrive in other shapes — SQLite's `datetime()`/`CURRENT_TIMESTAMP` and jOOQ's own default
 * OffsetDateTime/Timestamp bindings use a space instead of 'T' and may omit the zone offset.
 * A strict `OffsetDateTime.parse` would throw on those (and take the whole request/app down).
 * So we fast-path ISO, then fall back: normalize the space separator, and assume UTC when no
 * offset is present.
 */
class OffsetDateTimeConverter : Converter<Any, OffsetDateTime> {
    override fun from(databaseObject: Any?): OffsetDateTime? {
        val raw = (databaseObject as? String)?.takeIf(String::isNotBlank) ?: return null
        return runCatching { OffsetDateTime.parse(raw) }.getOrElse {
            val iso = raw.replaceFirst(' ', 'T')
            runCatching { OffsetDateTime.parse(iso) }
                .getOrElse { LocalDateTime.parse(iso).atOffset(ZoneOffset.UTC) }
        }
    }
    override fun to(userObject: OffsetDateTime?): Any? = userObject?.toString()
    override fun fromType(): Class<Any> = Any::class.java
    override fun toType(): Class<OffsetDateTime> = OffsetDateTime::class.java
}
