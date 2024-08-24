package zoned.framework.form

import kotlin.reflect.KClass

/**
 * Triggers a validation if the annotated param is null or empty. For collections, validates that the element at
 * each index is not null or empty.
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class NotEmpty(val label: String)

annotation class DefaultProvider<T>(val provider: KClass<out NewValueProvider<T>>)