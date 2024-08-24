package zoned.framework.db

import org.jooq.*
import org.jooq.Record
import java.util.*

/**
 * Get a single record
 */
inline fun <reified T> ResultQuery<Record>.get(): T? {
    return fetchOneInto(T::class.java)
}

/**
 * Get a list of records
 */
inline fun <reified T> ResultQuery<Record>.all(): List<T> {
    return fetchInto(T::class.java)
}

inline fun <reified T> SelectConditionStep<Record1<T?>>.all(): List<T> {
    return fetchInto(T::class.java)
}

/**
 * Get the id from an insert (requires returningResult)
 */
fun InsertResultStep<Record1<UUID?>>.id(): UUID? {
    return fetchOneInto(UUID::class.java)
}